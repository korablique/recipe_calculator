package korablique.recipecalculator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class UserGoalActivity extends MyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_goal);

        final Spinner goalSpinner = (Spinner) findViewById(R.id.goal_spinner);
        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(this,
                R.array.goals_array, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalSpinner.setAdapter(goalAdapter);

        final Spinner genderSpinner = (Spinner) findViewById(R.id.gender_spinner);
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        final Spinner physicalActivitySpinner = (Spinner) findViewById(R.id.physical_activity_spinner);
        ArrayAdapter<CharSequence> physicalActivityAdapter = ArrayAdapter.createFromResource(this,
                R.array.physical_activity_array, R.layout.long_spinner_item);
        physicalActivityAdapter.setDropDownViewResource(R.layout.long_spinner_item);
        physicalActivitySpinner.setAdapter(physicalActivityAdapter);

        final Spinner formulaSpinner = (Spinner) findViewById(R.id.formula_spinner);
        ArrayAdapter<CharSequence> formulaAdapter = ArrayAdapter.createFromResource(this,
                R.array.formula_array, android.R.layout.simple_spinner_item);
        formulaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        formulaSpinner.setAdapter(formulaAdapter);

        findViewById(R.id.calculate_nutrition_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedGoal = (String) goalSpinner.getSelectedItem();
                String selectedGender = (String) genderSpinner.getSelectedItem();
                int age = Integer.parseInt(((EditText) findViewById(R.id.age_edit_text)).getText().toString());
                int height = Integer.parseInt(((EditText) findViewById(R.id.height_edit_text)).getText().toString());
                int weight = Integer.parseInt(((EditText) findViewById(R.id.weight_edit_text)).getText().toString());
                String selectedFormula = (String) formulaSpinner.getSelectedItem();

                String physicalActivityString = (String) physicalActivitySpinner.getSelectedItem();
                String coefficientString = physicalActivityString.substring(0, physicalActivityString.indexOf(" "))
                        .replace(',', '.');
                float coefficient = Float.parseFloat(coefficientString);

                Rates rates = RateCalculator.calculate(
                        UserGoalActivity.this, selectedGoal, selectedGender, age, height, weight, coefficient, selectedFormula);
                // TODO: 11.10.17 сохранить эти значения в БД

                Intent intent = new Intent(UserGoalActivity.this, HistoryActivity.class);
                intent.putExtra("calories", rates.getCalories());
                intent.putExtra("protein", rates.getProtein());
                intent.putExtra("fats", rates.getFats());
                intent.putExtra("carbs", rates.getCarbs());
                startActivity(intent);
            }
        });
    }
}
