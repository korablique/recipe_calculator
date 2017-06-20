package korablique.recipecalculator;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class CalculatorActivity extends AppCompatActivity {
    public static final String NAME = "NAME";
    public static final String WEIGHT = "WEIGHT";
    public static final String PROTEIN = "PROTEIN";
    public static final String FATS = "FATS";
    public static final String CARBS = "CARBS";
    public static final String CALORIES = "CALORIES";
    public static final String SEARCH_RESULT = "SEARCH_RESULT";
    public static final int FIND_FOODSTUFF_REQUEST = 1;
    private RecyclerView ingredients;
    private Card card;
    private FoodstuffsAdapter.Observer adapterObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            card.displayForFoodstuff(foodstuff, position);
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
        setContentView(R.layout.activity_calculator);

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(this);
        final SQLiteDatabase database = dbHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_NAME + ";", null);
        //если база данных ещё пустая (типа при первом запуске приложения), то заполняем её:
        if (cursor.getCount() == 0) {
            DatabaseFiller.fillDbOnFirstAppStart(database);
        }
        cursor.close();

        //инициализируем layout, который будет отображать введенные продукты:
        ingredients = (RecyclerView) findViewById(R.id.ingredients);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        ingredients.setLayoutManager(layoutManager);
        ingredients.setAdapter(foodstuffsAdapter);

        //создаем карточку и прячем её под экран:
        final FrameLayout parentLayout = (FrameLayout) findViewById(R.id.activity_calculator_frame_layout);
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
                card.displayEmpty();
            }
        });

        View cardsSearchImageView = card.getSearchImageButton();
        cardsSearchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(CalculatorActivity.this, ListOfFoodstuffsActivity.class);
                sendIntent.setAction(getString(R.string.find_foodstuff_action));
                String foodstuffName = card.getNameEditText().getText().toString();
                sendIntent.putExtra(NAME, foodstuffName);
                startActivityForResult(sendIntent, FIND_FOODSTUFF_REQUEST);
            }
        });

        View cardsButtonOK = card.getButtonOk();
        cardsButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonOkClicked();
            }
        });

        View cardsButtonDelete = card.getButtonDelete();
        cardsButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                foodstuffsAdapter.deleteItem(card.getEditedFoodstuffPosition());
                card.hide();
            }
        });

        View cardsButtonSave = card.getButtonSave();
        cardsButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!card.isFilledEnoughToSaveFoodstuff()) {
                    Toast.makeText(CalculatorActivity.this, "Заполните название и БЖУК", Toast.LENGTH_LONG).show();
                    return;
                }

                String name = card.getNameEditText().getText().toString();
                double protein = Double.valueOf(card.getProteinEditText().getText().toString());
                double fats = Double.valueOf(card.getFatsEditText().getText().toString());
                double carbs = Double.valueOf(card.getCarbsEditText().getText().toString());
                double calories = Double.valueOf(card.getCaloriesEditText().getText().toString());

                Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_NAME
                        + " WHERE " + COLUMN_NAME_FOODSTUFF_NAME + " = '" + name + "' AND "
                        + COLUMN_NAME_PROTEIN + " = " + protein + " AND "
                        + COLUMN_NAME_FATS + " = " + fats + " AND "
                        + COLUMN_NAME_CARBS + " = " + carbs + " AND "
                        + COLUMN_NAME_CALORIES + " = " + calories + ";", null);
                //если такого продукта нет в БД:
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_NAME_FOODSTUFF_NAME, name);
                    values.put(COLUMN_NAME_PROTEIN, protein);
                    values.put(COLUMN_NAME_FATS, fats);
                    values.put(COLUMN_NAME_CARBS, carbs);
                    values.put(COLUMN_NAME_CALORIES, calories);
                    database.insert(TABLE_NAME, null, values);
                    Toast.makeText(CalculatorActivity.this, "Продукт сохранён", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CalculatorActivity.this, "Продукт уже существует", Toast.LENGTH_SHORT).show();
                }
                cursor.close();
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
        if (ingredients.getChildCount() == 0) {
            Toast.makeText(CalculatorActivity.this, "Добавьте ингридиенты", Toast.LENGTH_SHORT).show();
            return;
        }
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
            Toast.makeText(CalculatorActivity.this, "Заполните все данные", Toast.LENGTH_SHORT).show();
            return;
        }

        String productName = card.getNameEditText().getText().toString();
        double weight, protein, fats, carbs, calories;
        try {
            weight = Double.parseDouble(card.getWeightEditText().getText().toString());
            protein = Double.parseDouble(card.getProteinEditText().getText().toString());
            fats = Double.parseDouble(card.getFatsEditText().getText().toString());
            carbs = Double.parseDouble(card.getCarbsEditText().getText().toString());
            calories = Double.parseDouble(card.getCaloriesEditText().getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(CalculatorActivity.this, "Вводите только числа", Toast.LENGTH_SHORT).show();
            return;
        }

        Foodstuff foodstuff = new Foodstuff(productName, weight, protein, fats, carbs, calories);
        Foodstuff editedFoodstuff = card.getEditedFoodstuff();
        if (editedFoodstuff == null) {
            foodstuffsAdapter.addItem(foodstuff);
        } else {
            foodstuffsAdapter.replaceItem(foodstuff, card.getEditedFoodstuffPosition());
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
                card.getNameEditText().setText(foodstuff.getName());
                card.getProteinEditText().setText(String.valueOf(foodstuff.getProtein()));
                card.getFatsEditText().setText(String.valueOf(foodstuff.getFats()));
                card.getCarbsEditText().setText(String.valueOf(foodstuff.getCarbs()));
                card.getCaloriesEditText().setText(String.valueOf(foodstuff.getCalories()));
            }
        }
    }

    public Card getCard() {
        return card;
    }
}