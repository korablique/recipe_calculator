package korablique.recipecalculator.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.Date;

import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.ui.CardDisplaySource;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.MyActivity;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.ui.usergoal.UserGoalActivity;
import korablique.recipecalculator.model.UserParameters;

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
            card.displayForFoodstuff(foodstuff, foodstuff);
            card.setFocusableExceptWeight(false);
            card.setFocusableWeight(false);
            card.setButtonsVisible(false, Card.ButtonType.OK, Card.ButtonType.SAVE, Card.ButtonType.SEARCH);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        DatabaseWorker.getInstance().requestCurrentUserParameters(
                HistoryActivity.this, new DatabaseWorker.RequestCurrentUserParametersCallback() {
            @Override
            public void onResult(final UserParameters userParameters) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (userParameters == null) {
                            Intent intent = new Intent(HistoryActivity.this, UserGoalActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            initializeActivity(userParameters);
                        }
                    }
                });
            }
        });
    }

    private void initializeActivity(UserParameters userParameters) {
        final Rates rates = RateCalculator.calculate(
                HistoryActivity.this,
                userParameters.getGoal(),
                userParameters.getGender(),
                userParameters.getAge(),
                userParameters.getHeight(),
                userParameters.getWeight(),
                userParameters.getPhysicalActivityCoefficient(),
                userParameters.getFormula());
        recyclerView = findViewById(R.id.recycler_view);
        adapter = new HistoryAdapter(
                adapterObserver,
                rates.getCalories(),
                rates.getProtein(),
                rates.getFats(),
                rates.getCarbs());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        DatabaseWorker.getInstance().requestAllHistoryFromDb(this, new DatabaseWorker.RequestHistoryCallback() {
            @Override
            public void onResult(final ArrayList<HistoryEntry> historyEntries) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (HistoryEntry historyEntry : historyEntries) {
                            adapter.addItem(historyEntry);
                        }
                    }
                });
            }
        });

        ViewGroup parentLayout = findViewById(R.id.history_parent);
        card = new Card(this, parentLayout);

        FloatingActionButton historyFab = findViewById(R.id.history_fab);
        historyFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cardDisplaySource = CardDisplaySource.PlusClicked;
                card.displayEmpty();
                card.setFocusableExceptWeight(true);
                card.setFocusableWeight(true);
                card.setButtonsVisible(true, Card.ButtonType.OK, Card.ButtonType.SAVE, Card.ButtonType.SEARCH);
            }
        });

        card.setOnButtonOkClickedRunnable(new Runnable() {
            @Override
            public void run() {
                if (!card.areAllEditTextsFull()) {
                    Alerter.create(HistoryActivity.this)
                            .setTitle("Опаньки...")
                            .setText("Заполнены не все данные")
                            .setDuration(2000)
                            .setBackgroundColorRes(R.color.colorAccent)
                            .enableSwipeToDismiss()
                            .show();
                    return;
                }

                final Foodstuff foodstuff = card.parseFoodstuff();

                if (cardDisplaySource == CardDisplaySource.PlusClicked) {
                    if (card.getCurrentCustomPayload() != null) { // значит, продукт добавлен из списка
                        // проверяем, был ли редактирован продукт
                        Foodstuff foodstuffFromDatabase = (Foodstuff) card.getCurrentCustomPayload();

                        boolean areFoodstuffsSame = Foodstuff.haveSameNutrition(foodstuff, foodstuffFromDatabase)
                                && foodstuff.getName().equals(foodstuffFromDatabase.getName());
                        boolean wasFoodstuffFromListEdited = !areFoodstuffsSame;

                        if (wasFoodstuffFromListEdited) {
                            addUnlistedFoodstuffToHistory(foodstuff);
                        } else {
                            addListedFoodstuffToHistory(foodstuff, foodstuffFromDatabase.getId());
                        }
                    } else {
                        addUnlistedFoodstuffToHistory(foodstuff);
                    }
                    recyclerView.smoothScrollToPosition(1); //т к новый продукт добавляется в текущую дату
                } else {
                    // TODO: 29.09.17 сделать возможным редактирование записи в истории
                    //adapter.replaceItem(foodstuff, editedFoodstuffPosition);
                    recyclerView.smoothScrollToPosition(editedFoodstuffPosition);
                    throw new UnsupportedOperationException("Не редактируй!");
                }

                card.hide();
                KeyboardHandler keyboardHandler = new KeyboardHandler(HistoryActivity.this);
                keyboardHandler.hideKeyBoard();
            }
        });

        card.setOnButtonDeleteClickedRunnable(new Runnable() {
            @Override
            public void run() {
                long historyId = ((HistoryAdapter.FoodstuffData) adapter.getItem(editedFoodstuffPosition))
                        .getHistoryEntry().getHistoryId();
                adapter.deleteItem(editedFoodstuffPosition);
                DatabaseWorker.getInstance().deleteEntryFromHistory(HistoryActivity.this, historyId);
                card.hide();
            }
        });

        card.setOnButtonSaveClickedRunnable(new Runnable() {
            @Override
            public void run() {
                if (!card.isFilledEnoughToSaveFoodstuff()) {
                    Alerter.create(HistoryActivity.this)
                            .setTitle("Сохранить не получится!")
                            .setText("Нужно заполнить название и БЖУК")
                            .setDuration(3500)
                            .setBackgroundColorRes(R.color.colorAccent)
                            .enableSwipeToDismiss()
                            .show();
                    return;
                }

                Foodstuff savingFoodstuff = card.parseFoodstuff();

                if (savingFoodstuff.getProtein() + savingFoodstuff.getFats() + savingFoodstuff.getCarbs() > 100) {
                    Alerter.create(HistoryActivity.this)
                            .setTitle("Опаньки...")
                            .setText("Сумма белков, жиров и углеводов не может быть больше 100")
                            .setDuration(3500)
                            .setBackgroundColorRes(R.color.colorAccent)
                            .enableSwipeToDismiss()
                            .show();
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
                                    Alerter.create(HistoryActivity.this)
                                            .setTitle("Опаньки...")
                                            .setText("Продукт уже существует")
                                            .setDuration(2000)
                                            .setBackgroundColorRes(R.color.colorAccent)
                                            .enableSwipeToDismiss()
                                            .show();
                                } else {
                                    new KeyboardHandler(HistoryActivity.this).hideKeyBoard();
                                    Snackbar.make(findViewById(android.R.id.content), "Продукт сохранён", Snackbar.LENGTH_SHORT)
                                            .show();
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

    private void addListedFoodstuffToHistory(final Foodstuff foodstuff, long foodstuffId) {
        final Date date = new Date();
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        databaseWorker.saveFoodstuffToHistory(
                HistoryActivity.this, date, foodstuffId, foodstuff.getWeight(), new DatabaseWorker.AddHistoryEntryCallback() {
                    @Override
                    public void onResult(final long historyEntryId) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.addItem(new HistoryEntry(historyEntryId, foodstuff, date));
                            }
                        });
                    }
                });
    }

    private void addUnlistedFoodstuffToHistory(final Foodstuff foodstuff) {
        final Date date = new Date();
        final DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        databaseWorker.saveUnlistedFoodstuff(
                HistoryActivity.this, foodstuff, new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
                    @Override
                    public void onResult(long foodstuffId) {
                        databaseWorker.saveFoodstuffToHistory(
                                HistoryActivity.this,
                                date,
                                foodstuffId,
                                foodstuff.getWeight(), new DatabaseWorker.AddHistoryEntryCallback() {
                                    @Override
                                    public void onResult(final long historyEntryId) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                adapter.addItem(new HistoryEntry(historyEntryId, foodstuff, date));
                                            }
                                        });
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                card.displayForFoodstuff(foodstuff, foodstuff);
                card.setButtonsVisible(false, Card.ButtonType.DELETE);
                card.setButtonsVisible(true, Card.ButtonType.OK, Card.ButtonType.SAVE, Card.ButtonType.SEARCH);
                card.setFocusableExceptWeight(false);
                card.setFocusableWeight(true);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.history);
        } else {
            Crashlytics.log("getSupportActionBar вернул null");
        }
    }
}
