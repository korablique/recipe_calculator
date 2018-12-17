package korablique.recipecalculator.ui.usergoal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.mainscreen.MainActivity;

public class UserParametersActivity extends BaseActivity {
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxActivitySubscriptions subscriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_parameters);

        // гендер
        List<String> genderList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.gender_array)));
        int hidingItemIndex = 0;
        GenderAdapter genderAdapter = new GenderAdapter(this, android.R.layout.simple_spinner_item, genderList, hidingItemIndex);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner genderSpinner = findViewById(R.id.gender_spinner);
        genderSpinner.setAdapter(genderAdapter);

        // образ жизни
        Spinner lifestyleSpinner = findViewById(R.id.lifestyle_spinner);
        ArrayAdapter<CharSequence> lifestyleAdapter = ArrayAdapter.createFromResource(this,
                R.array.physical_activity_array, android.R.layout.simple_spinner_item);
        lifestyleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lifestyleSpinner.setAdapter(lifestyleAdapter);
        lifestyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String description = getResources().getStringArray(
                        R.array.physical_activity_description_array)[position];
                ((TextView) findViewById(R.id.description)).setText(description);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                ((TextView) findViewById(R.id.description)).setText("");
            }
        });

        Button saveUserParamsButton = findViewById(R.id.button_save);
        saveUserParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = ((EditText) findViewById(R.id.name)).getText().toString();
                String surname = ((EditText) findViewById(R.id.surname)).getText().toString();
                String gender = (String) ((Spinner) findViewById(R.id.gender_spinner)).getSelectedItem();
                int age = Integer.parseInt(((EditText) findViewById(R.id.age)).getText().toString());

                String goal = (String) ((Spinner) findViewById(R.id.goal_spinner)).getSelectedItem();
                String physicalActivityString = (String) ((Spinner) findViewById(R.id.lifestyle_spinner)).getSelectedItem();
                String coefficientString = physicalActivityString.replace(',', '.');
                float coefficient = getCoefficient(coefficientString);

                int height = Integer.parseInt(((EditText) findViewById(R.id.height)).getText().toString());
                int weight = Integer.parseInt(((EditText) findViewById(R.id.weight)).getText().toString());
                String formula = (String) ((Spinner) findViewById(R.id.formula_spinner)).getSelectedItem();

                if (allFieldsFilled()) {
                    saveUserName(name, surname);

                    UserParameters userParameters = new UserParameters(
                            goal, gender, age, height, weight, coefficient, formula);
                    Completable callback = userParametersWorker.saveUserParameters(userParameters);
                    subscriptions.subscribe(callback, () -> {
                        Intent intent = new Intent(UserParametersActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Toast.makeText(UserParametersActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.set_user_params);
        } else {
            Crashlytics.log("getSupportActionBar вернул null");
        }
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

    private void saveUserName(String name, String surname) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.user_name), name);
        editor.putString(getString(R.string.user_surname), surname);
        editor.apply();
    }

    private boolean allFieldsFilled() {
        EditText nameView = findViewById(R.id.name);
        EditText surnameView = findViewById(R.id.surname);
        EditText ageView = findViewById(R.id.age);
        EditText heightView = findViewById(R.id.height);
        EditText weightView = findViewById(R.id.weight);
        Spinner genderSpinner = findViewById(R.id.gender_spinner);
        return !nameView.getText().toString().isEmpty()
                && !surnameView.getText().toString().isEmpty()
                && !ageView.getText().toString().isEmpty()
                && !heightView.getText().toString().isEmpty()
                && !weightView.getText().toString().isEmpty()
                && genderSpinner.getSelectedItemPosition() != 0;
    }
}
