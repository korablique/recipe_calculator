package korablique.recipecalculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;


public class HistoryActivity extends MyActivity {
    private Card card;
    private int editedFoodstuffPosition;
    private CardDisplaySource cardDisplaySource;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private HistoryAdapter.Observer adapterObserver = new HistoryAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            editedFoodstuffPosition = position;
            cardDisplaySource = CardDisplaySource.FoodstuffClicked;
            card.displayForFoodstuff(foodstuff);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean firstStart = prefs.getBoolean(getString(R.string.pref_first_start_of_history), true);
        if (firstStart) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(getString(R.string.pref_first_start_of_history), false);
            editor.apply();
            Intent intent = new Intent(this, UserGoalActivity.class);
            startActivity(intent);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        adapter = new HistoryAdapter(adapterObserver);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 5));
        adapter.addItem(new Foodstuff("тортик", 100, 5, 20, 50, 400), new Date(117, 7, 3));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 6));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 6, 31));
        adapter.addItem(new Foodstuff("тортик", 100, 5, 20, 50, 400), new Date(117, 7, 4));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 3));

        ViewGroup parentLayout = (ViewGroup) findViewById(R.id.history_parent);
        card = new Card(this, parentLayout);

        FloatingActionButton historyFab = (FloatingActionButton) findViewById(R.id.history_fab);
        historyFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cardDisplaySource = CardDisplaySource.PlusClicked;
                card.displayEmpty();
            }
        });

        card.setOnButtonOkClickedRunnable(new Runnable() {
            @Override
            public void run() {
                if (!card.areAllEditTextsFull()) {
                    Snackbar.make(findViewById(android.R.id.content), "Заполните все данные", Snackbar.LENGTH_LONG).show();
                    return;
                }

                Foodstuff foodstuff;
                try {
                    foodstuff = card.parseFoodstuff();
                } catch (NumberFormatException e) {
                    Snackbar.make(findViewById(android.R.id.content), "В полях для ввода БЖУК вводите только числа", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (cardDisplaySource == CardDisplaySource.PlusClicked) {
                    adapter.addItem(foodstuff, new Date());
                    recyclerView.smoothScrollToPosition(1); //т к новый продукт добавляется в текущую дату
                } else {
                    adapter.replaceItem(foodstuff, editedFoodstuffPosition);
                    recyclerView.smoothScrollToPosition(editedFoodstuffPosition);
                }

                card.hide();
                KeyboardHandler keyboardHandler = new KeyboardHandler(HistoryActivity.this);
                keyboardHandler.hideKeyBoard();
            }
        });

        card.setOnButtonDeleteClickedRunnable(new Runnable() {
            @Override
            public void run() {
                adapter.deleteItem(editedFoodstuffPosition);
                card.hide();
            }
        });

        card.setOnButtonSaveClickedRunnable(new Runnable() {
            @Override
            public void run() {
                if (!card.isFilledEnoughToSaveFoodstuff()) {
                    Snackbar.make(findViewById(android.R.id.content), "Заполните название и БЖУК", Snackbar.LENGTH_LONG).show();
                    return;
                }

                Foodstuff savingFoodstuff = card.parseFoodstuff();

                if (savingFoodstuff.getProtein() + savingFoodstuff.getFats() + savingFoodstuff.getCarbs() > 100) {
                    Snackbar.make(findViewById(android.R.id.content), "Сумма белков, жиров и углеводов не может быть больше 100", Snackbar.LENGTH_LONG).show();
                    return;
                }

                DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                databaseWorker.saveFoodstuff(HistoryActivity.this, savingFoodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
                    @Override
                    public void onResult(final boolean hasAlreadyContainsFoodstuff) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (hasAlreadyContainsFoodstuff) {
                                    Snackbar.make(findViewById(android.R.id.content), "Продукт уже существует", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Snackbar.make(findViewById(android.R.id.content), "Продукт сохранён", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });

        card.setOnSearchButtonClickedRunnable(new Runnable() {
            @Override
            public void run() {
                Intent sendIntent = new Intent(HistoryActivity.this, ListOfFoodstuffsActivity.class);
                sendIntent.setAction(getString(R.string.find_foodstuff_action));
                String foodstuffName = card.getName();
                sendIntent.putExtra(NAME, foodstuffName);
                startActivityForResult(sendIntent, FIND_FOODSTUFF_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                card.setFoodstuff(foodstuff);
            }
        }
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
