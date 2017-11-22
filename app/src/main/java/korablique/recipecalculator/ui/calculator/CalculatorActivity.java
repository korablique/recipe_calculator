package korablique.recipecalculator.ui.calculator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.ui.CardDisplaySource;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.FoodstuffsAdapter;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.BaseActivity;
import korablique.recipecalculator.R;
import korablique.recipecalculator.ui.RecyclerViewUtils;

import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

public class CalculatorActivity extends BaseActivity {
    public static final String WEIGHT = "WEIGHT";
    public static final String PROTEIN = "PROTEIN";
    public static final String FATS = "FATS";
    public static final String CARBS = "CARBS";
    public static final String CALORIES = "CALORIES";
    private RecyclerView ingredients;
    private Card card;
    private int editedFoodstuffPosition;
    private CardDisplaySource cardSource;
    private FoodstuffsAdapter.Observer adapterObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            editedFoodstuffPosition = position;
            cardSource = CardDisplaySource.FoodstuffClicked;
            card.displayForFoodstuff(foodstuff, foodstuff.getId());
        }

        @Override
        public void onItemsCountChanged(int count) {
            if (count > 0) {
                findViewById(R.id.start_text_view).setVisibility(View.GONE);
            } else {
                findViewById(R.id.start_text_view).setVisibility(View.VISIBLE);
            }
        }
    };
    private FoodstuffsAdapter foodstuffsAdapter = new FoodstuffsAdapter(this, adapterObserver);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_calculator);

        //инициализируем layout, который будет отображать введенные продукты:
        ingredients = findViewById(R.id.ingredients);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        ingredients.setLayoutManager(layoutManager);
        ingredients.setAdapter(foodstuffsAdapter);
        ingredients.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateBottomShadowState();
            }
        });

        //создаем карточку и прячем её под экран:
        final ViewGroup parentLayout = findViewById(R.id.activity_calculator_frame_layout);
        card = new Card(this, parentLayout);

        EditText resultWeightEditText = findViewById(R.id.result_weight_edit_text);
        resultWeightEditText.clearFocus(); //не работает
        resultWeightEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                card.hide();
            }
        });

        FloatingActionButton floatingActionButtonPlus = findViewById(R.id.fab_add_foodstuff);
        floatingActionButtonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardSource = CardDisplaySource.PlusClicked;
                card.displayEmpty();
            }
        });

        card.setOnSearchButtonClickedRunnable(new Runnable() {
            @Override
            public void run() {
                Intent sendIntent = new Intent(CalculatorActivity.this, ListOfFoodstuffsActivity.class);
                sendIntent.setAction(getString(R.string.find_foodstuff_action));
                String foodstuffName = card.getName();
                sendIntent.putExtra(NAME, foodstuffName);
                startActivityForResult(sendIntent, FIND_FOODSTUFF_REQUEST);
            }
        });

        card.setOnButtonOkClickedRunnable(new Runnable() {
            @Override
            public void run() {
                onButtonOkClicked();
            }
        });

        card.setOnButtonDeleteClickedRunnable(new Runnable() {
            @Override
            public void run() {
                foodstuffsAdapter.deleteItem(editedFoodstuffPosition);
                card.hide();
            }
        });

        card.setOnButtonSaveClickedRunnable(new Runnable() {
            @Override
            public void run() {
                if (!card.isFilledEnoughToSaveFoodstuff()) {
                    Alerter.create(CalculatorActivity.this)
                            .setBackgroundColorRes(R.color.colorAccent)
                            .setTitle("Сохранить не получится!")
                            .setText("Нужно заполнить название и БЖУК")
                            .setDuration(3500)
                            .enableSwipeToDismiss()
                            .show();
                    return;
                }

                Foodstuff savingFoodstuff = card.parseFoodstuff();

                if (savingFoodstuff.getProtein() + savingFoodstuff.getFats() + savingFoodstuff.getCarbs() > 100) {
                    Alerter.create(CalculatorActivity.this)
                            .setBackgroundColorRes(R.color.colorAccent)
                            .setTitle("Опаньки...")
                            .setText("Сумма белков, жиров и углеводов не может быть больше 100")
                            .setDuration(3500)
                            .enableSwipeToDismiss()
                            .show();
                    return;
                }

                DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                databaseWorker.saveFoodstuff(CalculatorActivity.this, savingFoodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
                    @Override
                    public void onResult(final boolean hasAlreadyContainsFoodstuff, long id) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (hasAlreadyContainsFoodstuff) {
                                    Alerter.create(CalculatorActivity.this)
                                            .setBackgroundColorRes(R.color.colorAccent)
                                            .setTitle("Опаньки...")
                                            .setText("Продукт уже существует")
                                            .setDuration(2000)
                                            .enableSwipeToDismiss()
                                            .show();
                                } else {
                                    new KeyboardHandler(CalculatorActivity.this).hideKeyBoard();
                                    Snackbar.make(findViewById(android.R.id.content), "Продукт сохранён", Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        });
                    }
                });
            }
        });

        Button calculateButton = findViewById(R.id.calculate_button);
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCalculateButtonClicked();
            }
        });
    }

    private void updateBottomShadowState() {
        View shadowBottom = findViewById(R.id.shadow_bottom);
        if (RecyclerViewUtils.isRecyclerScrollable(ingredients)) {
            shadowBottom.setVisibility(View.VISIBLE);
        } else {
            shadowBottom.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (card.isDisplayed()) {
            card.hide();
        } else {
            super.onBackPressed();
        }
    }

    private void onCalculateButtonClicked() {
        //получить все Foodstuff'ы
        ArrayList<Foodstuff> foodstuffs = new ArrayList<>();
        for (int index = 0; index < foodstuffsAdapter.getItemCount(); index++) {
            foodstuffs.add(foodstuffsAdapter.getItem(index));
        }

        double proteinPer100Gram, fatsPer100Gram, carbsPer100Gram, caloriesPer100Gram, productWeight;
        // пройти циклом по всем фудстаффам и умножить Б, Ж, У и К (указанные в карточке на 100 г)
        // на массу продукта
        double allProtein = 0, allFats = 0, allCarbs = 0, allCalories = 0, totalWeight = 0;
        for (Foodstuff foodstuff : foodstuffs) {
            productWeight = foodstuff.getWeight();
            proteinPer100Gram = foodstuff.getProtein();
            fatsPer100Gram = foodstuff.getFats();
            carbsPer100Gram = foodstuff.getCarbs();
            caloriesPer100Gram = foodstuff.getCalories();

            allProtein += (proteinPer100Gram * productWeight * 0.01);
            allFats += (fatsPer100Gram * productWeight * 0.01);
            allCarbs += (carbsPer100Gram * productWeight * 0.01);
            allCalories += (caloriesPer100Gram * productWeight * 0.01);
            totalWeight += productWeight;
        }

        double recipeProteinPer100Gram, recipeFatsPer100Gram, recipeCarbsPer100Gram, recipeCaloriesPer100Gram;

        EditText resultWeightEditText = findViewById(R.id.result_weight_edit_text);
        double resultWeight;
        if (!resultWeightEditText.getText().toString().isEmpty()) {
            resultWeight = Double.parseDouble(resultWeightEditText.getText().toString());
        } else {
            resultWeight = totalWeight;
        }

        //рассчитать бжук на 100 г
        recipeProteinPer100Gram = allProtein * 100 / resultWeight;
        recipeFatsPer100Gram = allFats * 100 / resultWeight;
        recipeCarbsPer100Gram = allCarbs * 100 / resultWeight;
        recipeCaloriesPer100Gram = allCalories * 100 / resultWeight;

        Bundle bundle = new Bundle();
        bundle.putDouble(WEIGHT, resultWeight);
        bundle.putDouble(PROTEIN, recipeProteinPer100Gram);
        bundle.putDouble(FATS, recipeFatsPer100Gram);
        bundle.putDouble(CARBS, recipeCarbsPer100Gram);
        bundle.putDouble(CALORIES, recipeCaloriesPer100Gram);
        ShowResultDialogFragment dialog = new ShowResultDialogFragment();
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "show result");
    }

    private void onButtonOkClicked() {
        if (!card.areAllEditTextsFull()) {
            Alerter.create(CalculatorActivity.this)
                    .setBackgroundColorRes(R.color.colorAccent)
                    .setTitle("Опаньки...")
                    .setText("Заполнены не все данные")
                    .setDuration(2000)
                    .enableSwipeToDismiss()
                    .show();
            return;
        }

        Foodstuff foodstuff = card.parseFoodstuff();
        if (foodstuff.getProtein() + foodstuff.getFats() + foodstuff.getCarbs() > 100) {
            Alerter.create(CalculatorActivity.this)
                    .setBackgroundColorRes(R.color.colorAccent)
                    .setTitle("Опаньки")
                    .setText("Сумма белков, жиров и углеводов не может быть больше 100")
                    .setDuration(3500)
                    .enableSwipeToDismiss()
                    .show();
            return;
        }

        if (cardSource == CardDisplaySource.PlusClicked) {
            foodstuffsAdapter.addItem(foodstuff);
            ingredients.smoothScrollToPosition(foodstuffsAdapter.getItemCount() - 1);
        } else {
            foodstuffsAdapter.replaceItem(foodstuff, editedFoodstuffPosition);
            ingredients.smoothScrollToPosition(editedFoodstuffPosition);
        }

        card.hide();
        KeyboardHandler keyboardHandler = new KeyboardHandler(this);
        keyboardHandler.hideKeyBoard();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable[] foodstuffs = new Parcelable[foodstuffsAdapter.getItemCount()];
        for (int index = 0; index < foodstuffsAdapter.getItemCount(); index++) {
            foodstuffs[index] = foodstuffsAdapter.getItem(index);
        }
        outState.putParcelableArray("foodstuffs", foodstuffs);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Parcelable[] foodstuffs = savedInstanceState.getParcelableArray("foodstuffs");
        if (foodstuffs == null) {
            return;
        }
        for (Parcelable foodstuff : foodstuffs) {
            foodstuffsAdapter.addItem((Foodstuff) foodstuff);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                card.displayForFoodstuff(foodstuff, null);
                card.setButtonsVisible(false, Card.ButtonType.DELETE);
            }
        }
    }

    public Card getCard() {
        return card;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.calculator);
        } else {
            Crashlytics.log("getSupportActionBar вернул null");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomShadowState();
    }
}
