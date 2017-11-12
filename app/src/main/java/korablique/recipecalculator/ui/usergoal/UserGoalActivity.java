package korablique.recipecalculator.ui.usergoal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.ui.MyActivity;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.history.HistoryActivity;

public class UserGoalActivity extends MyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_goal);

        final Spinner goalSpinner = findViewById(R.id.goal_spinner);
        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(this,
                R.array.goals_array, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalSpinner.setAdapter(goalAdapter);

        final Spinner genderSpinner = findViewById(R.id.gender_spinner);
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        final Spinner physicalActivitySpinner = findViewById(R.id.physical_activity_spinner);
        ArrayAdapter<CharSequence> physicalActivityAdapter = ArrayAdapter.createFromResource(this,
                R.array.physical_activity_array, android.R.layout.simple_spinner_item);
        physicalActivityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        physicalActivitySpinner.setAdapter(physicalActivityAdapter);
        physicalActivitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String description = getResources().getStringArray(
                        R.array.physical_activity_description_array)[position];
                ((TextView) findViewById(R.id.activity_coefficient_description)).setText(description);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                ((TextView) findViewById(R.id.activity_coefficient_description)).setText("");
            }
        });

        Button finishButton = findViewById(R.id.calculate_button);
        finishButton.setText(R.string.user_goal_finish_button_text);

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedGoal = (String) goalSpinner.getSelectedItem();
                String selectedGender = (String) genderSpinner.getSelectedItem();
                int age = Integer.parseInt(((EditText) findViewById(R.id.age_edit_text)).getText().toString());
                int height = Integer.parseInt(((EditText) findViewById(R.id.height_edit_text)).getText().toString());
                int weight = Integer.parseInt(((EditText) findViewById(R.id.weight_edit_text)).getText().toString());

                String physicalActivityString = (String) physicalActivitySpinner.getSelectedItem();
                String coefficientString = physicalActivityString.substring(0, physicalActivityString.indexOf(" "))
                        .replace(',', '.');
                float coefficient = Float.parseFloat(coefficientString);
                String defaultFormula = getResources().getStringArray(R.array.formula_array)[0];
                UserParameters userParameters = new UserParameters(
                        selectedGoal, selectedGender, age, height, weight, coefficient, defaultFormula);
                DatabaseWorker.getInstance().saveUserParameters(
                        UserGoalActivity.this, userParameters, new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent(UserGoalActivity.this, HistoryActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.set_goal);
        } else {
            Crashlytics.log("getSupportActionBar вернул null");
        }
    }
}
