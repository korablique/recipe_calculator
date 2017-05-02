package korablique.recipecalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class CalculatorActivity extends AppCompatActivity {
    public static final String WEIGHT = "WEIGHT";
    public static final String PROTEIN = "PROTEIN";
    public static final String FATS = "FATS";
    public static final String CARBS = "CARBS";
    public static final String CALORIES = "CALORIES";
    private LinearLayout ingredientsLayout;
    private Card card;
    private View.OnClickListener onRowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            card.displayForRow((TableRow) v);
        }
    };

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

        //инициализируем tableLayout:
        ingredientsLayout = (LinearLayout) findViewById(R.id.ingredients_layout);

        //создаем карточку и прячем её под экран:
        FrameLayout parentLayout = (FrameLayout) findViewById(R.id.frame_layout);
        card = new Card(this, parentLayout);
        card.hide();
        card.addOnGlobalLayoutListener();

        EditText resultWeightEditText = (EditText) findViewById(R.id.result_weight_edit_text);
        resultWeightEditText.clearFocus(); //не работает
        resultWeightEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                card.hide();
            }
        });

        Button addProductButton = (Button) findViewById(R.id.button_add);
        addProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                card.displayEmpty();
                /*TranslateAnimation translateAnimation =
                        new TranslateAnimation(0, 0, displayHeight, parent.getHeight() - card.getHeight());
                translateAnimation.setDuration(500);
                translateAnimation.setFillAfter(true);
                card.startAnimation(translateAnimation);*/
            }
        });

        Button cardsButtonOK = card.getButtonOk();
        cardsButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonOkClicked();
            }
        });

        Button cardsButtonDelete = card.getButtonDelete();
        cardsButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ingredientsLayout.removeView(card.getEditedRow());
                card.hide();
                card.clear();
            }
        });

        Button cardsButtonSave = card.getButtonSave();
        cardsButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: правильное условие? есть метод areAllEditTextFull(), но мне в данном случае не нужна масса продукта
                if (card.getNameEditText().getText().toString().isEmpty()
                        || card.getProteinEditText().getText().toString().isEmpty()
                        || card.getFatsEditText().getText().toString().isEmpty()
                        || card.getCarbsEditText().getText().toString().isEmpty()
                        || card.getCaloriesEditText().getText().toString().isEmpty()) {
                    Toast.makeText(CalculatorActivity.this, "Заполните название и БЖУК", Toast.LENGTH_LONG).show();
                    return;
                }
                String name = card.getNameEditText().getText().toString();
                double protein = Double.parseDouble(card.getProteinEditText().getText().toString());
                double fats = Double.parseDouble(card.getFatsEditText().getText().toString());
                double carbs = Double.parseDouble(card.getCarbsEditText().getText().toString());
                double calories = Double.parseDouble(card.getCaloriesEditText().getText().toString());

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

    private void onCalculateButtonClicked() {
        if (ingredientsLayout.getChildCount() == 0) {
            Toast.makeText(CalculatorActivity.this, "Добавьте ингридиенты", Toast.LENGTH_SHORT).show();
            return;
        }
        //получить все Row
        ArrayList<LinearLayout> rows = new ArrayList<>();
        for (int index = 0; index < ingredientsLayout.getChildCount(); index++) {
            rows.add((LinearLayout) ingredientsLayout.getChildAt(index));
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
            TextView weightTextView = (TextView) rows.get(index).findViewById(R.id.weight);
            TextView proteinTextView = (TextView) rows.get(index).findViewById(R.id.protein);
            TextView fatsTextView = (TextView) rows.get(index).findViewById(R.id.fats);
            TextView carbsTextView = (TextView) rows.get(index).findViewById(R.id.carbs);
            TextView caloriesTextView = (TextView) rows.get(index).findViewById(R.id.calories);

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
        String productName = ((EditText) findViewById(R.id.name_edit_text)).getText().toString();
        double weight, protein, fats, carbs, calories;
        try {
            weight = Double.parseDouble(((EditText) findViewById(R.id.weight_edit_text)).getText().toString());
            protein = Double.parseDouble(((EditText) findViewById(R.id.protein_edit_text)).getText().toString());
            fats = Double.parseDouble(((EditText) findViewById(R.id.fats_edit_text)).getText().toString());
            carbs = Double.parseDouble(((EditText) findViewById(R.id.carbs_edit_text)).getText().toString());
            calories = Double.parseDouble(((EditText) findViewById(R.id.calories_edit_text)).getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(CalculatorActivity.this, "Вводите только числа", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout editedRow = card.getEditedRow();
        LinearLayout row;
        if (editedRow == null) {
            //добавить строчку в таблицу:
            row = (LinearLayout) LayoutInflater.from(CalculatorActivity.this).inflate(R.layout.foodstuff_layout, null);
        } else {
            row = editedRow;
        }
        //заполнить этими числами строчку в таблице:
        TextView nameTextView = (TextView) row.findViewById(R.id.name);
        nameTextView.setText(productName);
        LinearLayout linearWithTextViews = (LinearLayout) row.findViewById(R.id.linear_layout_with_text_views);
        ((TextView) linearWithTextViews.getChildAt(0)).setText(String.valueOf(weight));
        ((TextView) linearWithTextViews.getChildAt(1)).setText(String.valueOf(protein));
        ((TextView) linearWithTextViews.getChildAt(2)).setText(String.valueOf(fats));
        ((TextView) linearWithTextViews.getChildAt(3)).setText(String.valueOf(carbs));
        ((TextView) linearWithTextViews.getChildAt(4)).setText(String.valueOf(calories));
        if (editedRow == null) {
            row.setOnClickListener(onRowClickListener);
            ingredientsLayout.addView(row);
        }
        card.hide();
        card.clear();
        hideKeyBoard();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ingredientsLayout.getChildCount() == 0) {
            return;
        }
        //получить все Row
        ArrayList<LinearLayout> rows = new ArrayList<>();
        for (int index = 0; index < ingredientsLayout.getChildCount(); index++) {
            rows.add((LinearLayout) ingredientsLayout.getChildAt(index));
        }
        outState.putInt("rows count", rows.size());
        //у каждого Row получить TextView'хи
        for (int rowNumber = 0; rowNumber < rows.size(); rowNumber++) {
            LinearLayout currentRow = rows.get(rowNumber);
            TextView nameTextView = (TextView) currentRow.findViewById(R.id.name);
            TextView weightTextView = (TextView) currentRow.findViewById(R.id.weight);
            TextView proteinTextView = (TextView) currentRow.findViewById(R.id.protein);
            TextView fatsTextView = (TextView) currentRow.findViewById(R.id.fats);
            TextView carbsTextView = (TextView) currentRow.findViewById(R.id.carbs);
            TextView caloriesTextView = (TextView) currentRow.findViewById(R.id.calories);
            //и записать числа из них в массив data
            String[] data = new String[6];
            data[0] = nameTextView.getText().toString();
            data[1] = weightTextView.getText().toString();
            data[2] = proteinTextView.getText().toString();
            data[3] = fatsTextView.getText().toString();
            data[4] = carbsTextView.getText().toString();
            data[5] = caloriesTextView.getText().toString();
            outState.putStringArray("row " + rowNumber, data);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //кривое условие... проверяем, есть ли хоть первый массив
        if (savedInstanceState.containsKey("row " + 0)) {
            //создать новые строки
            for (int rowNumber = 0; rowNumber < savedInstanceState.getInt("rows count"); rowNumber++) {
                LinearLayout row = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.foodstuff_layout, null);
                String[] savedData = savedInstanceState.getStringArray("row " + rowNumber);
                //заполнить их сохраненными данными
                //TODO: было бы прикольно создать для такой row отдельный класс, а то сколько можно TextView'шки доставать
                TextView nameTextView = (TextView) row.findViewById(R.id.name);
                TextView weightTextView = (TextView) row.findViewById(R.id.weight);
                TextView proteinTextView = (TextView) row.findViewById(R.id.protein);
                TextView fatsTextView = (TextView) row.findViewById(R.id.fats);
                TextView carbsTextView = (TextView) row.findViewById(R.id.carbs);
                TextView caloriesTextView = (TextView) row.findViewById(R.id.calories);

                nameTextView.setText(savedData[0]); //TODO: почему? (хотя вроде до этого тоже светилась)
                weightTextView.setText(savedData[1]);
                proteinTextView.setText(savedData[2]);
                fatsTextView.setText(savedData[3]);
                carbsTextView.setText(savedData[4]);
                caloriesTextView.setText(savedData[5]);

                //добавить новую строку в таблицу
                ingredientsLayout.addView(row);
            }
        }
    }

    private void hideKeyBoard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}