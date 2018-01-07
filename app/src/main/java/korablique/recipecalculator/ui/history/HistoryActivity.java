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

import javax.inject.Inject;

import korablique.recipecalculator.FloatUtils;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.ui.CardDisplaySource;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.ui.usergoal.UserGoalActivity;
import korablique.recipecalculator.model.UserParameters;

import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;


public class HistoryActivity extends BaseActivity {
    @Inject
    DatabaseWorker databaseWorker;

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
            card.setFocusableExceptWeight(true);
            card.setFocusableWeight(true);
            card.setButtonsVisible(false, Card.ButtonType.SAVE, Card.ButtonType.SEARCH);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ViewGroup parentLayout = findViewById(R.id.history_parent);
        card = new Card(this, parentLayout);

        databaseWorker.requestCurrentUserParameters(
                HistoryActivity.this, new DatabaseWorker.RequestCurrentUserParametersCallback() {
            @Override
            public void onResult(final UserParameters userParameters) {
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
                this,
                adapterObserver,
                rates);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        databaseWorker.requestAllHistoryFromDb(this, new DatabaseWorker.RequestHistoryCallback() {
            @Override
            public void onResult(final ArrayList<HistoryEntry> historyEntries) {
                for (HistoryEntry historyEntry : historyEntries) {
                    adapter.addItem(historyEntry);
                }
            }
        });

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
                    // значит, пользователь щёлкнул на уже добавленный продукт
                    Foodstuff foodstuffFromDb = (Foodstuff) card.getCurrentCustomPayload();
                    double weight = foodstuffFromDb.getWeight();
                    double newWeight = foodstuff.getWeight();
                    double protein = foodstuffFromDb.getProtein();
                    double newProtein = foodstuff.getProtein();
                    double fats = foodstuffFromDb.getFats();
                    double newFats = foodstuff.getFats();
                    double carbs = foodstuffFromDb.getCarbs();
                    double newCarbs = foodstuff.getCarbs();
                    double calories = foodstuffFromDb.getCalories();
                    double newCalories = foodstuff.getCalories();
                    // проверить, были ли отредактированы Б, Ж, У или К
                    if (!FloatUtils.areFloatsEquals(protein, newProtein)
                            || !FloatUtils.areFloatsEquals(fats, newFats)
                            || !FloatUtils.areFloatsEquals(carbs, newCarbs)
                            || !FloatUtils.areFloatsEquals(calories, newCalories)
                            || !foodstuff.getName().equals(foodstuffFromDb.getName())) {
                        final HistoryEntry historyEntry =
                                ((HistoryAdapter.FoodstuffData) adapter.getItem(editedFoodstuffPosition))
                                        .getHistoryEntry();
                        HistoryEntry newEntry = new HistoryEntry(
                                historyEntry.getHistoryId(), foodstuff, historyEntry.getTime());
                        adapter.replaceItem(newEntry, editedFoodstuffPosition);
                        databaseWorker.saveUnlistedFoodstuff(
                                HistoryActivity.this,
                                foodstuff,
                                new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
                                    @Override
                                    public void onResult(long foodstuffId) {
                                        databaseWorker.updateFoodstuffIdInHistory(
                                                HistoryActivity.this,
                                                historyEntry.getHistoryId(),
                                                foodstuffId,
                                                null);
                                    }
                                });
                    }
                    if (!FloatUtils.areFloatsEquals(weight, newWeight)) {
                        HistoryEntry historyEntry =
                                ((HistoryAdapter.FoodstuffData) adapter.getItem(editedFoodstuffPosition))
                                .getHistoryEntry();
                        HistoryEntry newEntry = new HistoryEntry(
                                historyEntry.getHistoryId(), foodstuff, historyEntry.getTime());
                        adapter.replaceItem(newEntry, editedFoodstuffPosition);
                        databaseWorker.editWeightInHistoryEntry(
                                HistoryActivity.this, newEntry.getHistoryId(), newWeight, null);
                    }
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
                long historyId = ((HistoryAdapter.FoodstuffData) adapter.getItem(editedFoodstuffPosition))
                        .getHistoryEntry().getHistoryId();
                adapter.deleteItem(editedFoodstuffPosition);
                databaseWorker.deleteEntryFromHistory(HistoryActivity.this, historyId);
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

                databaseWorker.saveFoodstuff(
                        HistoryActivity.this,
                        savingFoodstuff,
                        new DatabaseWorker.SaveFoodstuffCallback() {
                    @Override
                    public void onResult(long id) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new KeyboardHandler(HistoryActivity.this).hideKeyBoard();
                                Snackbar.make(
                                        findViewById(android.R.id.content),
                                        "Продукт сохранён",
                                        Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                    @Override
                    public void onDuplication() {
                        Alerter.create(HistoryActivity.this)
                                .setTitle("Опаньки...")
                                .setText("Продукт уже существует")
                                .setDuration(2000)
                                .setBackgroundColorRes(R.color.colorAccent)
                                .enableSwipeToDismiss()
                                .show();
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
        databaseWorker.saveFoodstuffToHistory(
                HistoryActivity.this,
                date,
                foodstuffId,
                foodstuff.getWeight(), new DatabaseWorker.AddHistoryEntriesCallback() {
            @Override
            public void onResult(final ArrayList<Long> historyEntriesIds) {
                adapter.addItem(new HistoryEntry(historyEntriesIds.get(0), foodstuff, date));
            }
        });
    }

    private void addUnlistedFoodstuffToHistory(final Foodstuff foodstuff) {
        final Date date = new Date();
        databaseWorker.saveUnlistedFoodstuff(
                HistoryActivity.this,
                foodstuff,
                new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
            @Override
            public void onResult(long foodstuffId) {
                databaseWorker.saveFoodstuffToHistory(
                        HistoryActivity.this,
                        date,
                        foodstuffId,
                        foodstuff.getWeight(), new DatabaseWorker.AddHistoryEntriesCallback() {
                    @Override
                    public void onResult(final ArrayList<Long> historyEntriesIds) {
                        adapter.addItem(new HistoryEntry(
                                historyEntriesIds.get(0), foodstuff, date));
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

    public Card getCard() {
        return card;
    }
}
