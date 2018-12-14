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

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.R;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.history.HistoryActivity;

public class UserGoalActivity extends BaseActivity {
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxActivitySubscriptions subscriptions;

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
                String coefficientString = physicalActivityString.replace(',', '.');
                float coefficient = getCoefficient(coefficientString);

                String defaultFormula = getResources().getStringArray(R.array.formula_array)[0];
                UserParameters userParameters = new UserParameters(
                        selectedGoal, selectedGender, age, height, weight, coefficient, defaultFormula);
                Completable callback = userParametersWorker.saveUserParameters(userParameters);
                subscriptions.subscribe(callback, () -> {
                    Intent intent = new Intent(UserGoalActivity.this, HistoryActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    private float getCoefficient(String coefficientString) {
        String[] lifestyleValues = getResources().getStringArray(R.array.physical_activity_array);
        if (coefficientString.equals(lifestyleValues[0])) {
            return 1.2f;
        } else if (coefficientString.equals(lifestyleValues[1])) {
            return 1.375f;
        } else if (coefficientString.equals(lifestyleValues[2])) {
            return 1.55f;
        } else if (coefficientString.equals(lifestyleValues[3])) {
            return 1.725f;
        } else {
            return 1.9f;
        }
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
