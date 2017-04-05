package korablique.recipecalculator;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Formatter;

public class CalculatorActivity extends AppCompatActivity {
    public static final String RESULT_STRING = "RESULT_STRING";
    private TableLayout tableLayout;
    private RelativeLayout card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        final FrameLayout parent = (FrameLayout) findViewById(R.id.frame_layout);
        //инициализируем tableLayout:
        tableLayout = (TableLayout) findViewById(R.id.table_layout);

        //создаем карточку и прячем её под экран:
        card = (RelativeLayout) findViewById(R.id.card); //TODO
        Display display = getWindowManager().getDefaultDisplay(); //TODO: это наверное должен быть метод типа hideCard()
        Point size = new Point();
        display.getSize(size);
        final int displayHeight = size.y;
        card.setY(card.getHeight() + displayHeight);

        raiseCardAboveKeyboard();

        Button addProductButton = (Button) findViewById(R.id.button_add);
        addProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //показать карточку
                card.setVisibility(View.VISIBLE);
                card.bringToFront();
                card.setY(parent.getHeight() - card.getHeight());
                /*TranslateAnimation translateAnimation =
                        new TranslateAnimation(0, 0, displayHeight, parent.getHeight() - card.getHeight());
                translateAnimation.setDuration(500);
                translateAnimation.setFillAfter(true);
                card.startAnimation(translateAnimation);*/
            }
        });

        Button cardsButtonOK = (Button) findViewById(R.id.button_ok); //TODO: это должно остаться в MainActivity?
        cardsButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!areAllEditTextsFull()) {
                    Toast.makeText(CalculatorActivity.this, "Заполните все данные", Toast.LENGTH_SHORT).show();
                    return;
                }
                card.setY(card.getHeight() + displayHeight);
                card.setVisibility(View.INVISIBLE);
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
                //добавить строчку в таблицу:
                TableRow row = (TableRow) LayoutInflater.from(CalculatorActivity.this).inflate(R.layout.recipe_component_layout, null);
                for (int index = 0; index < row.getChildCount(); index++) {
                    ((TextView)row.getChildAt(index)).setMaxWidth(row.getChildAt(index).getWidth());
                }
                //заполнить этими числами строчку в таблице:
                ((TextView) row.getChildAt(0)).setText(productName);
                ((TextView) row.getChildAt(1)).setText(String.valueOf(weight));
                ((TextView) row.getChildAt(2)).setText(String.valueOf(protein));
                ((TextView) row.getChildAt(3)).setText(String.valueOf(fats));
                ((TextView) row.getChildAt(4)).setText(String.valueOf(carbs));
                ((TextView) row.getChildAt(5)).setText(String.valueOf(calories));
                addRowClickListener(row);
                tableLayout.addView(row);
                clearEditTexts();
                hideKeyBoard();
            }
        });

        Button cardsButtonDelete = (Button) findViewById(R.id.button_delete);
        cardsButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        Button countButton = (Button) findViewById(R.id.count_button);
        countButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tableLayout.getChildCount() == 1) {
                    Toast.makeText(CalculatorActivity.this, "Добавьте ингридиенты", Toast.LENGTH_SHORT).show();
                }
                //получить все TableRaw, кроме первой, т к 1 - это шапка
                ArrayList<TableRow> rows = new ArrayList<>();
                for (int index = 1; index < tableLayout.getChildCount(); index++) {
                    rows.add((TableRow) tableLayout.getChildAt(index));
                }

                //пройти циклом по всем этим TableRow и умножаем белок на массу продукта
                //то же сделать с жирами и углеводами
                //посчитать общую массу продукта
                //потом по формуле рассчитать кбжу на 100 г

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

                Formatter formatter = new Formatter();
                formatter.format("Масса готового продукта - %.0f грамм\n"
                        + "Белки - %.2f\n"
                        + "Жиры - %.2f\n"
                        + "Углеводы - %.2f\n"
                        + "Калорийность - %.2f\n",
                        resultWeight,
                        recipeProteinPer100Gram,
                        recipeFatsPer100Gram,
                        recipeCarbsPer100Gram,
                        recipeCaloriesPer100Gram);

                Bundle bundle = new Bundle();
                bundle.putString(RESULT_STRING, formatter.toString());
                ShowResultDialogFragment dialog = new ShowResultDialogFragment();
                dialog.setArguments(bundle);
                dialog.show(getSupportFragmentManager(), "show result");
            }
        });
    }

    private void addRowClickListener(TableRow row) {
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //тут изменить background у child'ов, чтоб было видно, на какую строчку нажал
                //показать карточку
            }
        });
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

    private void raiseCardAboveKeyboard() {
        final Window mRootWindow = getWindow();
        final View mRootView = mRootWindow.getDecorView().findViewById(android.R.id.content);
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        int mRootViewHeight = mRootView.getHeight();
                        Rect rect = new Rect();
                        View view = mRootWindow.getDecorView();
                        view.getWindowVisibleDisplayFrame(rect);
                        int visibleDisplayFrameHeight = rect.left;
                        int delta = mRootViewHeight - visibleDisplayFrameHeight;
                        card.setY(delta - card.getHeight());
                    }
                });
    }

    private boolean areAllEditTextsFull() {
        if (((EditText) findViewById(R.id.weight_edit_text)).getText().toString().isEmpty()) {
            return false;
        }
        if (((EditText) findViewById(R.id.protein_edit_text)).getText().toString().isEmpty()) {
            return false;
        }
        if (((EditText) findViewById(R.id.fats_edit_text)).getText().toString().isEmpty()) {
            return false;
        }
        if (((EditText) findViewById(R.id.carbs_edit_text)).getText().toString().isEmpty()) {
            return false;
        }
        if (((EditText) findViewById(R.id.calories_edit_text)).getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    private void clearEditTexts() {
        ((EditText) findViewById(R.id.weight_edit_text)).setText("");
        ((EditText) findViewById(R.id.protein_edit_text)).setText("");
        ((EditText) findViewById(R.id.fats_edit_text)).setText("");
        ((EditText) findViewById(R.id.carbs_edit_text)).setText("");
        ((EditText) findViewById(R.id.calories_edit_text)).setText("");
    }

    private void hideKeyBoard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
//TODO: наверное надо, чтоб пользователь мог вводить название продукта, который он вводит, чтоб он не забыл какой продукт вводил