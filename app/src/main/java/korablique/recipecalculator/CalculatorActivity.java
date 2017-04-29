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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Formatter;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class CalculatorActivity extends AppCompatActivity {
//    public static final String RESULT_STRING = "RESULT_STRING";
    public static final String WEIGHT = "WEIGHT";
    public static final String PROTEIN = "PROTEIN";
    public static final String FATS = "FATS";
    public static final String CARBS = "CARBS";
    public static final String CALORIES = "CALORIES";
    private TableLayout tableLayout;
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
        tableLayout = (TableLayout) findViewById(R.id.table_layout);

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
                tableLayout.removeView(card.getEditedRow());
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
                //TODO: надо сначала проверить, нет ли уже такого продукта в БД
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
        if (tableLayout.getChildCount() == 1) {
            Toast.makeText(CalculatorActivity.this, "Добавьте ингридиенты", Toast.LENGTH_SHORT).show();
            return;
        }
        //получить все TableRaw, кроме первой, т к 1 - это шапка
        ArrayList<TableRow> rows = new ArrayList<>();
        for (int index = 1; index < tableLayout.getChildCount(); index++) {
            rows.add((TableRow) tableLayout.getChildAt(index));
        }

        //пройти циклом по всем этим TableRow и умножаем белок на массу продукта
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
            productWeight = Double.parseDouble(((TextView) rows.get(index).getChildAt(1)).getText().toString());
            proteinPer100Gram = Double.parseDouble(((TextView) rows.get(index).getChildAt(2)).getText().toString());
            fatsPer100Gram = Double.parseDouble(((TextView) rows.get(index).getChildAt(3)).getText().toString());
            carbsPer100Gram = Double.parseDouble(((TextView) rows.get(index).getChildAt(4)).getText().toString());
            caloriesPer100Gram = Double.parseDouble(((TextView) rows.get(index).getChildAt(5)).getText().toString());

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

        TableRow editedRow = card.getEditedRow();
        TableRow row;
        if (editedRow == null) {
            //добавить строчку в таблицу:
            row = (TableRow) LayoutInflater.from(CalculatorActivity.this).inflate(R.layout.recipe_component_layout, null);
            for (int index = 0; index < row.getChildCount(); index++) {
                ((TextView) row.getChildAt(index)).setMaxWidth(row.getChildAt(index).getWidth());
            }
        } else {
            row = editedRow;
        }
        //заполнить этими числами строчку в таблице:
        ((TextView) row.getChildAt(0)).setText(productName);
        ((TextView) row.getChildAt(1)).setText(String.valueOf(weight));
        ((TextView) row.getChildAt(2)).setText(String.valueOf(protein));
        ((TextView) row.getChildAt(3)).setText(String.valueOf(fats));
        ((TextView) row.getChildAt(4)).setText(String.valueOf(carbs));
        ((TextView) row.getChildAt(5)).setText(String.valueOf(calories));
        if (editedRow == null) {
            row.setOnClickListener(onRowClickListener);
            tableLayout.addView(row);
        }
        card.hide();
        card.clear();
        hideKeyBoard();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //если строка только одна (шапка), то сохранять ничего не надо
        if (tableLayout.getChildCount() == 1) {
            return;
        }
        //получить все TableRow
        ArrayList<TableRow> rows = new ArrayList<>();
        for (int index = 1; index < tableLayout.getChildCount(); index++) {
            rows.add((TableRow) tableLayout.getChildAt(index));
        }
        outState.putInt("rows count", rows.size());
        //у каждого TableRow получить TextView'хи
        for (int index = 0; index < rows.size(); index++) {
            TableRow currentRow = rows.get(index);
            String[] cells = new String[currentRow.getChildCount()];
            //проходим по всем клеткам строчки
            for (int cellNumber = 0; cellNumber < currentRow.getChildCount(); cellNumber++) {
                //и добавляем число из каждой - в массив cells
                cells[cellNumber] = ((TextView) currentRow.getChildAt(cellNumber)).getText().toString();
            }
            outState.putStringArray("row " + index, cells);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //кривое условие... проверяем, есть ли хоть первый массив
        if (savedInstanceState.containsKey("row " + 0)) {
            //создать новые строки
            for (int rowNumber = 0; rowNumber < savedInstanceState.getInt("rows count"); rowNumber++) {
                TableRow row = (TableRow) LayoutInflater.from(this).inflate(R.layout.recipe_component_layout, null);
                String[] savedData = savedInstanceState.getStringArray("row " + rowNumber);
                //заполнить их сохраненными данными
                for (int index = 0; index < row.getChildCount(); index++) {
                    ((TextView) row.getChildAt(index)).setText(savedData[index]);
                }
                //добавить новую строку в tableLayout
                tableLayout.addView(row);
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