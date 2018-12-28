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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.ArrayAdapterWithDisabledItem;
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
        List<String> genderList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.gender_array)));
        int disableItemIndex = 0;
        ArrayAdapterWithDisabledItem genderAdapter = new ArrayAdapterWithDisabledItem(
                this, android.R.layout.simple_spinner_item, genderList, disableItemIndex);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);


        final Spinner lifestyleSpinner = findViewById(R.id.physical_activity_spinner);
        ArrayAdapter<CharSequence> lifestyleAdapter = ArrayAdapter.createFromResource(this,
                R.array.physical_activity_array, android.R.layout.simple_spinner_item);
        lifestyleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lifestyleSpinner.setAdapter(lifestyleAdapter);
        lifestyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                Goal selectedGoal = (Goal) goalSpinner.getSelectedItem();
                Gender selectedGender = (Gender) genderSpinner.getSelectedItem();
                int age = Integer.parseInt(((EditText) findViewById(R.id.age_edit_text)).getText().toString());
                int height = Integer.parseInt(((EditText) findViewById(R.id.height_edit_text)).getText().toString());
                int weight = Integer.parseInt(((EditText) findViewById(R.id.weight_edit_text)).getText().toString());
                Lifestyle selectedLifestyle = (Lifestyle) lifestyleSpinner.getSelectedItem();
                Formula defaultFormula = Formula.HARRIS_BENEDICT;

                UserParameters userParameters = new UserParameters(
                        selectedGoal, selectedGender, age, height, weight, selectedLifestyle, defaultFormula);
                Completable callback = userParametersWorker.saveUserParameters(userParameters);
                subscriptions.subscribe(callback, () -> {
                    Intent intent = new Intent(UserGoalActivity.this, HistoryActivity.class);
                    startActivity(intent);
                    finish();
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
