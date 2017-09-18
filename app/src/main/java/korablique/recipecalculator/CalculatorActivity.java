package korablique.recipecalculator;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

public class CalculatorActivity extends MyActivity {
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
            card.displayForFoodstuff(foodstuff);
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
    private FoodstuffsAdapter foodstuffsAdapter = new FoodstuffsAdapter(adapterObserver);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_calculator);

        final FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(this);
        try {
            dbHelper.createDatabase();
        } catch (IOException e) {
            throw new IllegalStateException("Unknown problem while creating database", e);
        }

        //инициализируем layout, который будет отображать введенные продукты:
        ingredients = (RecyclerView) findViewById(R.id.ingredients);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        ingredients.setLayoutManager(layoutManager);
        ingredients.setAdapter(foodstuffsAdapter);
        ingredients.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                View shadowBottom = findViewById(R.id.shadow_bottom);
                if (RecyclerViewUtils.isRecyclerScrollable(ingredients)) {
                    shadowBottom.setVisibility(View.VISIBLE);
                } else {
                    shadowBottom.setVisibility(View.GONE);
                }
            }
        });

        //создаем карточку и прячем её под экран:
        final ViewGroup parentLayout = (ViewGroup) findViewById(R.id.activity_calculator_frame_layout);
        card = new Card(this, parentLayout);

        EditText resultWeightEditText = (EditText) findViewById(R.id.result_weight_edit_text);
        resultWeightEditText.clearFocus(); //не работает
        resultWeightEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                card.hide();
            }
        });

        FloatingActionButton floatingActionButtonPlus = (FloatingActionButton) findViewById(R.id.fab_add_foodstuff);
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
                    Snackbar.make(findViewById(android.R.id.content), "Заполните название и БЖУК", Snackbar.LENGTH_LONG).show();
                    return;
                }

                Foodstuff savingFoodstuff = card.parseFoodstuff();

                if (savingFoodstuff.getProtein() + savingFoodstuff.getFats() + savingFoodstuff.getCarbs() > 100) {
                    Snackbar.make(findViewById(android.R.id.content), "Сумма белков, жиров и углеводов не может быть больше 100", Snackbar.LENGTH_LONG).show();
                    return;
                }

                DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                databaseWorker.saveFoodstuff(CalculatorActivity.this, savingFoodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
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

        Button calculateButton = (Button) findViewById(R.id.calculate_button);
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCalculateButtonClicked();
            }
        });
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
        //получить все Row
        ArrayList<Row> rows = new ArrayList<>();
        for (int index = 0; index < ingredients.getChildCount(); index++) {
            rows.add(new Row(this, ingredients, (LinearLayout) ingredients.getChildAt(index)));
        }

        //пройти циклом по всем Row и умножаем белок на массу продукта
        //то же сделать с жирами и углеводами
        //посчитать общую массу продукта
        //потом по формуле рассчитать бжук на 100 г

        //1 - масса
        //2 - белки
        //3 - жиры
        //4 - углеводы
        //5 - калории
        double proteinPer100Gram, fatsPer100Gram, carbsPer100Gram, caloriesPer100Gram, productWeight;
        double allProtein = 0, allFats = 0, allCarbs = 0, allCalories = 0, totalWeight = 0;
        for (int index = 0; index < rows.size(); index++) {
            TextView weightTextView = rows.get(index).getWeightTextView();
            TextView proteinTextView = rows.get(index).getProteinTextView();
            TextView fatsTextView = rows.get(index).getFatsTextView();
            TextView carbsTextView = rows.get(index).getCarbsTextView();
            TextView caloriesTextView = rows.get(index).getCaloriesTextView();

            productWeight = Double.parseDouble((weightTextView.getText().toString()));
            proteinPer100Gram = Double.parseDouble((proteinTextView.getText().toString()));
            fatsPer100Gram = Double.parseDouble((fatsTextView.getText().toString()));
            carbsPer100Gram = Double.parseDouble((carbsTextView.getText().toString()));
            caloriesPer100Gram = Double.parseDouble((caloriesTextView.getText().toString()));

            allProtein += (proteinPer100Gram * productWeight * 0.01);
            allFats += (fatsPer100Gram * productWeight * 0.01);
            allCarbs += (carbsPer100Gram * productWeight * 0.01);
            allCalories += (caloriesPer100Gram * productWeight * 0.01);
            totalWeight += productWeight;
        }

        double recipeProteinPer100Gram, recipeFatsPer100Gram, recipeCarbsPer100Gram, recipeCaloriesPer100Gram;

        EditText resultWeightEditText = (EditText) findViewById(R.id.result_weight_edit_text);

        double resultWeight;
        if (!resultWeightEditText.getText().toString().isEmpty()) {
            resultWeight = Double.parseDouble(resultWeightEditText.getText().toString());
        } else {
            resultWeight = totalWeight;
        }
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

        if (foodstuff.getProtein() + foodstuff.getFats() + foodstuff.getCarbs() > 100) {
            Snackbar.make(findViewById(android.R.id.content), "Сумма белков, жиров и углеводов не может быть больше 100", Snackbar.LENGTH_LONG).show();
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
                card.setFoodstuff(foodstuff);
            }
        }
    }

    public Card getCard() {
        return card;
    }
}