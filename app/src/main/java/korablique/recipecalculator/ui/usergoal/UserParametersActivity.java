package korablique.recipecalculator.ui.usergoal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.ArrayAdapterWithDisabledItem;
import korablique.recipecalculator.ui.mainscreen.MainActivity;

public class UserParametersActivity extends BaseActivity {
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxActivitySubscriptions subscriptions;
    @Inject
    UserNameProvider userNameProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_parameters);

        // гендер
        List<String> genderList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.gender_array)));
        int disableItemIndex = 0;
        ArrayAdapterWithDisabledItem genderAdapter = new ArrayAdapterWithDisabledItem(
                this, android.R.layout.simple_spinner_item, genderList, disableItemIndex);
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
                if (!allFieldsFilled()) {
                    Toast.makeText(UserParametersActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                String name = ((EditText) findViewById(R.id.name)).getText().toString();
                String surname = ((EditText) findViewById(R.id.surname)).getText().toString();
                userNameProvider.saveUserName(name, surname);

                UserParameters userParameters = extractUserParameters();
                Completable callback = userParametersWorker.saveUserParameters(userParameters);
                subscriptions.subscribe(callback, () -> {
                    MainActivity.start(UserParametersActivity.this);
                    finish();
                });
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.set_user_params);
        } else {
            Crashlytics.log("getSupportActionBar returned null");
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, UserParametersActivity.class);
        context.startActivity(intent);
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

    private UserParameters extractUserParameters() {
        int goalSelectedPosition = ((Spinner) findViewById(R.id.goal_spinner)).getSelectedItemPosition();
        Goal goal = Goal.POSITIONS.get(goalSelectedPosition);

        int genderSelectedPosition = ((Spinner) findViewById(R.id.gender_spinner)).getSelectedItemPosition();
        Gender gender = Gender.POSITIONS.get(genderSelectedPosition);

        int age = Integer.parseInt(((EditText) findViewById(R.id.age)).getText().toString());
        int height = Integer.parseInt(((EditText) findViewById(R.id.height)).getText().toString());
        int weight = Integer.parseInt(((EditText) findViewById(R.id.weight)).getText().toString());

        int lifestyleSelectedPosition = ((Spinner) findViewById(R.id.lifestyle_spinner)).getSelectedItemPosition();
        Lifestyle lifestyle = Lifestyle.POSITIONS.get(lifestyleSelectedPosition);

        int formulaSelectedPosition = ((Spinner) findViewById(R.id.formula_spinner)).getSelectedItemPosition();
        Formula formula = Formula.POSITIONS.get(formulaSelectedPosition);

        return new UserParameters(goal, gender, age, height, weight, lifestyle, formula);
    }
}
