package korablique.recipecalculator.ui.history;

import android.content.Context;
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
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import korablique.recipecalculator.FloatUtils;
import korablique.recipecalculator.base.MainThreadExecutor;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.ui.CardDisplaySource;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.WeightedFoodstuff;
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
import static korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity.FIND_FOODSTUFF_ACTION;


public class HistoryActivity extends BaseActivity {
    private static final String ACTION_ADD_FOODSTUFFS = "ACTION_ADD_FOODSTUFFS";
    /**
     * При открытии HistoryActivity с одновременным добавлением в неё продуктов, нужно чтобы
     * продукты добавились не сразу, а с небольшой задержкой, чтобы юзер мог визуально увидеть
     * добавление этих новых продуктов.
     */
    private static final long DELAY_OF_ACTION_ADD_FOODSTUFFS = 1000;
    private static final String EXTRA_FOODSTUFFS = "EXTRA_FOODSTUFFS";

    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    HistoryWorker historyWorker;
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    MainThreadExecutor mainThreadExecutor;

    private Card card;
    private int editedFoodstuffPosition;
    private CardDisplaySource cardDisplaySource;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private HistoryAdapter.Observer adapterObserver = new HistoryAdapter.Observer() {
        @Override
        public void onItemClicked(WeightedFoodstuff foodstuff, int position) {
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

        userParametersWorker.requestCurrentUserParameters(
                HistoryActivity.this, new UserParametersWorker.RequestCurrentUserParametersCallback() {
            @Override
            public void onResult(final UserParameters userParameters) {
                if (userParameters == null) {
                    Intent intent = new Intent(HistoryActivity.this, UserGoalActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    initializeActivity(userParameters);
                    tryToAddFoodstuffsFrom(getIntent());
                }
            }
        });
    }

    private void tryToAddFoodstuffsFrom(@Nullable Intent intent) {
        if (intent == null || !ACTION_ADD_FOODSTUFFS.equals(intent.getAction())) {
            return;
        }

        List<WeightedFoodstuff> foodstuffs = intent.getParcelableArrayListExtra(EXTRA_FOODSTUFFS);
        if (foodstuffs == null) {
            throw new IllegalArgumentException("Need " + EXTRA_FOODSTUFFS);
        }
        // Меняем action на action-по-умолчанию чтобы при пересоздании Активити
        // в неё повторно не были добавлены переданные сюда фудстафы.
        intent.setAction(Intent.ACTION_DEFAULT);

        mainThreadExecutor.executeDelayed(DELAY_OF_ACTION_ADD_FOODSTUFFS, () -> {
            for (WeightedFoodstuff foodstuff : foodstuffs) {
                if (foodstuff.getId() >= 0) {
                    addListedFoodstuffToHistory(foodstuff, foodstuff.getId());
                } else {
                    addUnlistedFoodstuffToHistory(foodstuff);
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

        historyWorker.requestAllHistoryFromDb(new HistoryWorker.RequestHistoryCallback() {
            @Override
            public void onResult(final List<HistoryEntry> historyEntries) {
                adapter.addItems(historyEntries);
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

                final WeightedFoodstuff foodstuff = card.parseFoodstuff();

                if (cardDisplaySource == CardDisplaySource.PlusClicked) {
                    if (card.getCurrentCustomPayload() != null) { // значит, продукт добавлен из списка
                        // проверяем, был ли редактирован продукт
                        WeightedFoodstuff foodstuffFromDatabase = (WeightedFoodstuff) card.getCurrentCustomPayload();

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
                    WeightedFoodstuff foodstuffFromDb = (WeightedFoodstuff) card.getCurrentCustomPayload();
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
                                foodstuff.withoutWeight(),
                                new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
                                    @Override
                                    public void onResult(long foodstuffId) {
                                        historyWorker.updateFoodstuffIdInHistory(
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
                        historyWorker.editWeightInHistoryEntry(
                                newEntry.getHistoryId(), newWeight, null);
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
                historyWorker.deleteEntryFromHistory(historyId);
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

                WeightedFoodstuff savingFoodstuff = card.parseFoodstuff();

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
                        savingFoodstuff.withoutWeight(),
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
                sendIntent.setAction(FIND_FOODSTUFF_ACTION);
                String foodstuffName = card.getName();
                sendIntent.putExtra(NAME, foodstuffName);
                startActivityForResult(sendIntent, FIND_FOODSTUFF_REQUEST);
            }
        });
    }

    private void addListedFoodstuffToHistory(final WeightedFoodstuff foodstuff, long foodstuffId) {
        final Date date = new Date();
        historyWorker.saveFoodstuffToHistory(
                date,
                foodstuffId,
                foodstuff.getWeight(), new HistoryWorker.AddHistoryEntriesCallback() {
            @Override
            public void onResult(final List<Long> historyEntriesIds) {
                adapter.addItem(new HistoryEntry(historyEntriesIds.get(0), foodstuff, date));
            }
        });
    }

    private void addUnlistedFoodstuffToHistory(final WeightedFoodstuff foodstuff) {
        final Date date = new Date();
        databaseWorker.saveUnlistedFoodstuff(
                HistoryActivity.this,
                foodstuff.withoutWeight(),
                (foodstuffId) -> {
                    historyWorker.saveFoodstuffToHistory(
                            date,
                            foodstuffId,
                            foodstuff.getWeight(),
                            (historyEntriesIds) -> {
                                adapter.addItem(new HistoryEntry(
                                        historyEntriesIds.get(0), foodstuff, date));
                            });
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                WeightedFoodstuff weightedFoodstuff = foodstuff.withWeight(0);
                card.displayForFoodstuff(foodstuff, weightedFoodstuff);
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

    public static void startAndAdd(List<WeightedFoodstuff> items, Context context) {
        context.startActivity(createStartAndAddIntent(items, context));
    }

    public static Intent createStartAndAddIntent(List<WeightedFoodstuff> items, Context context) {
        Intent intent = new Intent(context, HistoryActivity.class);
        intent.setAction(ACTION_ADD_FOODSTUFFS);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS, new ArrayList<>(items));
        return intent;
    }
}
