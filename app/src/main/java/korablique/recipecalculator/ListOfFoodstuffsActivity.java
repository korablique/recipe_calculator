package korablique.recipecalculator;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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

import java.util.ArrayList;

import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

public class ListOfFoodstuffsActivity extends MyActivity {
    private Card card;
    private FoodstuffsAdapter recyclerViewAdapter;
    private CardDisplaySource cardDisplaySource;
    private int editedFoodstuffPosition;
    private long editedFoodstuffId; //id из базы данных
    private FoodstuffsAdapter.Observer defaultObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            editedFoodstuffPosition = position;
            editedFoodstuffId = foodstuff.getId();
            card.displayForFoodstuff(foodstuff, editedFoodstuffId);
        }

        @Override
        public void onItemsCountChanged(int count) {}
    };
    private FoodstuffsAdapter.Observer findFoodstuffObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            Intent intent = new Intent();
            intent.putExtra(SEARCH_RESULT, foodstuff);
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
            card.hideButtonOk();
            card.hideWeight();
            card.hideSearchButton();
            card.setOnButtonSaveClickedRunnable(new Runnable() {
                @Override
                public void run() {
                    if (!card.areAllEditTextsFull()) {
                        Snackbar.make(findViewById(android.R.id.content), "Заполните название и БЖУК", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Foodstuff newFoodstuff = card.parseFoodstuff();
                    if (newFoodstuff.getProtein() + newFoodstuff.getFats() + newFoodstuff.getCarbs() > 100) {
                        Snackbar.make(findViewById(android.R.id.content), "Сумма белков, жиров и углеводов не может быть больше 100", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    //сохраняем новые значения в базу данных
                    DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                    databaseWorker.editFoodstuff(ListOfFoodstuffsActivity.this, editedFoodstuffId, newFoodstuff);
                    recyclerViewAdapter.replaceItem(newFoodstuff, editedFoodstuffPosition);
                    Snackbar.make(findViewById(android.R.id.content), "Изменения сохранены", Snackbar.LENGTH_SHORT).show();
                    card.hide();
                    KeyboardHandler keyboardHandler = new KeyboardHandler(ListOfFoodstuffsActivity.this);
                    keyboardHandler.hideKeyBoard();
                }
            });

            card.setOnButtonDeleteClickedRunnable(new Runnable() {
                @Override
                public void run() {
                    DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                    databaseWorker.deleteFoodstuff(ListOfFoodstuffsActivity.this, editedFoodstuffId);
                    recyclerViewAdapter.deleteItem(editedFoodstuffPosition);
                    Snackbar.make(findViewById(android.R.id.content), "Продукт удалён", Snackbar.LENGTH_SHORT).show();
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
            final String searchName = getIntent().getStringExtra(NAME);
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
