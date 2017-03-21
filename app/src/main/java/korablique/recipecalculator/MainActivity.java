package korablique.recipecalculator;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Formatter;

public class MainActivity extends AppCompatActivity {
    public static final String RESULT_WEIGHT = "RESULT_WEIGHT";
    public static final String PROTEIN_PER_100_GRAM = "PROTEIN_PER_100_GRAM";
    public static final String FATS_PER_100_GRAM = "PROTEIN_PER_100_GRAM";
    public static final String CARBS_PER_100_GRAM = "PROTEIN_PER_100_GRAM";
    public static final String CALORIES_PER_100_GRAM = "PROTEIN_PER_100_GRAM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button countButton = (Button) findViewById(R.id.count_button);
        countButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TableLayout tableLayout = (TableLayout) findViewById(R.id.table_layout);
                //получить все TableRaw, кроме первой, т к 1 - это шапка
                //пройти циклом по всем этим TableRow и умножаем белок на массу продукта (массу перевести в кг)
                //то же сделать с жирами и углеводами
                //посчитать общую массу продукта
                //потом по формуле рассчитать кбжу на 100 г
                ArrayList<TableRow> rows = new ArrayList<>();
                for (int index = 1; index < tableLayout.getChildCount(); index++) {
                    rows.add((TableRow) tableLayout.getChildAt(index));
                }

                //1 - масса
                //2 - белки
                //3 - жиры
                //4 - углеводы
                //5 - калории
                double proteinPer100Gram, fatsPer100Gram, carbsPer100Gram, caloriesPer100Gram, productWeight;
                double allProtein = 0, allFats = 0, allCarbs = 0, allCalories = 0, totalWeight = 0;
                for (int index = 0; index < rows.size(); index++) {
                    if (!((EditText) rows.get(index).getChildAt(1)).getText().toString().isEmpty()) {
                        productWeight = Double.parseDouble(((EditText) rows.get(index).getChildAt(1)).getText().toString());
                        proteinPer100Gram = Double.parseDouble(((EditText) rows.get(index).getChildAt(2)).getText().toString());
                        fatsPer100Gram = Double.parseDouble(((EditText) rows.get(index).getChildAt(3)).getText().toString());
                        carbsPer100Gram = Double.parseDouble(((EditText) rows.get(index).getChildAt(4)).getText().toString());
                        caloriesPer100Gram = Double.parseDouble(((EditText) rows.get(index).getChildAt(5)).getText().toString());

                        allProtein += (proteinPer100Gram * productWeight * 0.01);
                        allFats += (fatsPer100Gram * productWeight * 0.01);
                        allCarbs += (carbsPer100Gram * productWeight * 0.01);
                        allCalories += (caloriesPer100Gram * productWeight * 0.01);
                        totalWeight += productWeight;
                    } else {
                        // если это пустая линия - пропустить.
                        //TODO: только надо учитывать строчки, если юзер массу ввел, а всё остальное - нет (программа упадет)
                        continue;
                    }
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

                TextView textView = (TextView) findViewById(R.id.result);
                textView.setText(formatter.toString());
            }
        });
    }
}
