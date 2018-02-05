package korablique.recipecalculator.ui.foodstuffslist;

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
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.FoodstuffsAdapter;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.R;

import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

public class ListOfFoodstuffsActivity extends BaseActivity {
    @Inject
    DatabaseWorker databaseWorker;

    private Card card;
    private FoodstuffsAdapter recyclerViewAdapter;
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

        if (wasActivityOpenedForSearching()) {
            createRecyclerView(findFoodstuffObserver);
        } else {
            card = new Card(this, findViewById(R.id.list_of_recipes_parent));
            card.setButtonsVisible(false, Card.ButtonType.OK, Card.ButtonType.SEARCH);
            card.hideWeight();
            card.setOnButtonSaveClickedRunnable(new Runnable() {
                @Override
                public void run() {
                    if (!card.isFilledEnoughToSaveFoodstuff()) {
                        Alerter.create(ListOfFoodstuffsActivity.this)
                                .setTitle("Сохранить не получится!")
                                .setText("Нужно заполнить название и БЖУК")
                                .setDuration(3500)
                                .setBackgroundColorRes(R.color.colorAccent)
                                .enableSwipeToDismiss()
                                .show();
                        return;
                    }
                    Foodstuff newFoodstuff = card.parseFoodstuff();
                    if (newFoodstuff.getProtein() + newFoodstuff.getFats() + newFoodstuff.getCarbs() > 100) {
                        Alerter.create(ListOfFoodstuffsActivity.this)
                                .setTitle("Опаньки...")
                                .setText("Сумма белков, жиров и углеводов не может быть больше 100")
                                .setDuration(3500)
                                .setBackgroundColorRes(R.color.colorAccent)
                                .enableSwipeToDismiss()
                                .show();
                        return;
                    }
                    //сохраняем новые значения в базу данных
                    databaseWorker.editFoodstuff(ListOfFoodstuffsActivity.this, editedFoodstuffId, newFoodstuff);
                    recyclerViewAdapter.replaceItem(newFoodstuff, editedFoodstuffPosition);
                    KeyboardHandler keyboardHandler = new KeyboardHandler(ListOfFoodstuffsActivity.this);
                    keyboardHandler.hideKeyBoard();
                    card.hide();
                    Snackbar.make(findViewById(android.R.id.content), "Изменения сохранены", Snackbar.LENGTH_SHORT).show();
                }
            });

            card.setOnButtonDeleteClickedRunnable(new Runnable() {
                @Override
                public void run() {
                    databaseWorker.makeFoodstuffUnlisted(ListOfFoodstuffsActivity.this, editedFoodstuffId, null);
                    recyclerViewAdapter.deleteItem(editedFoodstuffPosition);
                    new KeyboardHandler(ListOfFoodstuffsActivity.this).hideKeyBoard();
                    card.hide();
                    Snackbar.make(findViewById(android.R.id.content), "Продукт удалён", Snackbar.LENGTH_SHORT).show();
                }
            });
            createRecyclerView(defaultObserver);
        }
    }

    private void createRecyclerView(FoodstuffsAdapter.Observer observer) {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        recyclerViewAdapter = new FoodstuffsAdapter(this, observer);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(recyclerViewAdapter);

        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(
                this,
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(final List<Foodstuff> foodstuffs) {
                recyclerViewAdapter.addItems(foodstuffs);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        if (wasActivityOpenedForSearching()) {
            // Добавляем ActionExpandListener, чтобы во время коллапса (когда скукоживаается поиск)
            // закрывать активити
            MenuItem searchItem = menu.findItem(R.id.search);
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    ListOfFoodstuffsActivity.this.finish();
                    return true;
                }
            });
        }

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

        // NOTE: setHintColor() всегда нужно вызывать перед setQuery(),
        // иначе setHintColor() не отрабатывает на андроидах < 21
        EditText searchEditText = searchView.findViewById(R.id.search_src_text);
        searchEditText.setHintTextColor(getResources().getColor(R.color.colorPrimaryLight));

        if (getString(R.string.find_foodstuff_action).equals(getIntent().getAction())) {
            final String searchName = getIntent().getStringExtra(NAME);
            menu.findItem(R.id.search).expandActionView();
            searchView.setQuery(searchName, false);
        }

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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.list_of_foodstuffs);
        } else {
            Crashlytics.log("getSupportActionBar вернул null");
        }
    }

    private boolean wasActivityOpenedForSearching() {
        Intent receivedIntent = getIntent();
        return receivedIntent.getAction() != null
                && receivedIntent.getAction().equals(getString(R.string.find_foodstuff_action));
    }
}
