package korablique.recipecalculator;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class ListOfFoodstuffsActivity extends MyActivity {
    private Card card;
    private FoodstuffsAdapter recyclerViewAdapter;
    private FoodstuffsAdapter.Observer defaultObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            card.displayForFoodstuff(foodstuff, position);
        }

        @Override
        public void onItemsCountChanged(int count) {}
    };
    private FoodstuffsAdapter.Observer findFoodstuffObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            Intent intent = new Intent();
            intent.putExtra(CalculatorActivity.SEARCH_RESULT, foodstuff);
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        public void onItemsCountChanged(int count) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_foodstuffs);

        Intent receivedIntent = getIntent();
        if (receivedIntent.getAction() != null
                && receivedIntent.getAction().equals(getString(R.string.find_foodstuff_action))) {
            createRecyclerView(findFoodstuffObserver);
        } else {
            card = new Card(this, (ViewGroup) findViewById(R.id.list_of_recipes_parent));
            card.getButtonOk().setVisibility(View.GONE);
            card.getWeightEditText().setVisibility(View.GONE);
            card.getSearchImageButton().setVisibility(View.GONE);
            card.getButtonSave().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!card.areAllEditTextsFull()) {
                        Toast.makeText(ListOfFoodstuffsActivity.this, "Заполните название и БЖУК", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String newName = card.getNameEditText().getText().toString().trim();
                    double newProtein = Double.parseDouble(card.getProteinEditText().getText().toString());
                    double newFats = Double.parseDouble(card.getFatsEditText().getText().toString());
                    double newCarbs = Double.parseDouble(card.getCarbsEditText().getText().toString());
                    double newCalories = Double.parseDouble(card.getCaloriesEditText().getText().toString());
                    if (newProtein + newFats + newCarbs > 100) {
                        Toast.makeText(
                                ListOfFoodstuffsActivity.this,
                                "Сумма белков, жиров и углеводов не может быть больше 100",
                                Toast.LENGTH_LONG)
                                .show();
                        return;
                    }
                    Foodstuff newFoodstuff = new Foodstuff(newName, 0, newProtein, newFats, newCarbs, newCalories);
                    //сохраняем новые значения в базу данных
                    long id = card.getEditedFoodstuff().getId();
                    DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                    databaseWorker.editFoodstuff(ListOfFoodstuffsActivity.this, id, newFoodstuff);
                    recyclerViewAdapter.replaceItem(newFoodstuff, card.getEditedFoodstuffPosition());
                    Toast.makeText(ListOfFoodstuffsActivity.this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                    card.hide();
                    KeyboardHandler keyboardHandler = new KeyboardHandler(ListOfFoodstuffsActivity.this);
                    keyboardHandler.hideKeyBoard();

                }
            });
            card.getButtonDelete().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long id = card.getEditedFoodstuff().getId();
                    DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                    databaseWorker.deleteFoodstuff(ListOfFoodstuffsActivity.this, id);
                    recyclerViewAdapter.deleteItem(card.getEditedFoodstuffPosition());
                    Toast.makeText(ListOfFoodstuffsActivity.this, "Продукт удалён", Toast.LENGTH_SHORT).show();
                    card.hide();
                }
            });
            createRecyclerView(defaultObserver);
        }
        recyclerViewAdapter.hideWeight();
        findViewById(R.id.column_name_weight).setVisibility(View.GONE);
    }

    private void createRecyclerView(FoodstuffsAdapter.Observer observer) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        recyclerViewAdapter = new FoodstuffsAdapter(observer);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(recyclerViewAdapter);

        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        databaseWorker.requestAllFoodstuffsFromDb(this, new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(final ArrayList<Foodstuff> foodstuffs) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Foodstuff foodstuff : foodstuffs) {
                            recyclerViewAdapter.addItem(foodstuff);
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        // задаём слушатель запросов
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                recyclerViewAdapter.setNameFilter(query);
                return true;
            }
        });
        if (getString(R.string.find_foodstuff_action).equals(getIntent().getAction())) {
            final String searchName = getIntent().getStringExtra(CalculatorActivity.NAME);
            MenuItemCompat.expandActionView(menu.findItem(R.id.search));
            searchView.setQuery(searchName, false);
        }

        EditText searchEditText = (EditText) searchView.findViewById(R.id.search_src_text);
        searchEditText.setHintTextColor(getResources().getColor(R.color.colorPrimaryLight));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (card != null && card.isDisplayed()) {
            card.hide();
        } else {
            super.onBackPressed();
        }
    }
}
