package korablique.recipecalculator.ui.usergoal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.FullName;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.DecimalUtils;
import korablique.recipecalculator.ui.mainscreen.MainActivity;

import static korablique.recipecalculator.util.SpinnerTuner.startTuningSpinner;

public class UserParametersActivity extends BaseActivity {
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxActivitySubscriptions subscriptions;
    @Inject
    UserNameProvider userNameProvider;
    @Inject
    TimeProvider timeProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_parameters);

        // гендер
        startTuningSpinner(findViewById(R.id.gender_spinner))
                .withItems(R.array.gender_array)
                .addDisabledItemAt(0)
                .tune();

        // образ жизни
        Spinner lifestyleSpinner = findViewById(R.id.lifestyle_spinner);
        startTuningSpinner(lifestyleSpinner)
                .withItems(R.array.physical_activity_array)
                .onItemSelected((position, id) -> {
                    String description = getResources().getStringArray(
                            R.array.physical_activity_description_array)[position];
                    ((TextView) findViewById(R.id.description)).setText(description);
                })
                .tune();

        // формула
        startTuningSpinner(findViewById(R.id.formula_spinner))
                .withItems(R.array.physical_activity_array)
                .tune();

        Button saveUserParamsButton = findViewById(R.id.button_save);
        saveUserParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!allFieldsFilled()) {
                    Toast.makeText(UserParametersActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                String firstName = ((EditText) findViewById(R.id.first_name)).getText().toString();
                String lastName = ((EditText) findViewById(R.id.last_name)).getText().toString();
                FullName fullName = new FullName(firstName, lastName);
                userNameProvider.saveUserName(fullName);

                UserParameters userParameters = extractUserParameters();
                Completable callback = userParametersWorker.saveUserParameters(userParameters);
                subscriptions.subscribe(callback, () -> {
                    // если пользователь первый раз открыл приложение и у него ещё нет данных,
                    // то после того, как он их сохранит, открыть MainActivity
                    if (UserParametersActivity.this.isTaskRoot()) {
                        MainActivity.start(UserParametersActivity.this);
                    }
                    finish();
                });
            }
        });

        EditText dateOfBirthView = findViewById(R.id.date_of_birth);
        dateOfBirthView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText dateOfBirthView = findViewById(R.id.date_of_birth);
                String dateOfBirthString = dateOfBirthView.getText().toString();

                DatePickerFragment datePickerFragment;
                if (dateOfBirthString.isEmpty()) {
                    datePickerFragment = DatePickerFragment.showDialog(getSupportFragmentManager());
                } else {
                    LocalDate dateOfBirth = parseDateOfBirth(dateOfBirthString);
                    datePickerFragment = DatePickerFragment.showDialog(getSupportFragmentManager(), dateOfBirth);
                }
                datePickerFragment.setOnDateSetListener(new DatePickerFragment.DateSetListener() {
                    @Override
                    public void onDateSet(LocalDate date) {
                        dateOfBirthView.setText(date.toString(getString(R.string.date_format)));
                    }
                });
            }
        });

        Single<Optional<UserParameters>> oldUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(oldUserParamsSingle, userParametersOptional -> {
            if (userParametersOptional.isPresent()) {
                UserParameters oldUserParams = userParametersOptional.get();
                fillWithOldUserParameters(oldUserParams);
                fillUserName(userNameProvider.getUserName());
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
        EditText nameView = findViewById(R.id.first_name);
        EditText surnameView = findViewById(R.id.last_name);
        EditText ageView = findViewById(R.id.date_of_birth);
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
        float targetWeight = Float.parseFloat(((EditText) findViewById(R.id.target_weight)).getText().toString());

        int genderSelectedPosition = ((Spinner) findViewById(R.id.gender_spinner)).getSelectedItemPosition();
        Gender gender = Gender.POSITIONS.get(genderSelectedPosition - 1);

        String dateOfBirthString = ((EditText) findViewById(R.id.date_of_birth)).getText().toString();
        LocalDate dateOfBirth = parseDateOfBirth(dateOfBirthString);

        int height = Integer.parseInt(((EditText) findViewById(R.id.height)).getText().toString());
        float weight = Float.parseFloat(((EditText) findViewById(R.id.weight)).getText().toString());

        int lifestyleSelectedPosition = ((Spinner) findViewById(R.id.lifestyle_spinner)).getSelectedItemPosition();
        Lifestyle lifestyle = Lifestyle.POSITIONS.get(lifestyleSelectedPosition);

        int formulaSelectedPosition = ((Spinner) findViewById(R.id.formula_spinner)).getSelectedItemPosition();
        Formula formula = Formula.POSITIONS.get(formulaSelectedPosition);

        long nowTimestamp = timeProvider.nowUtc().getMillis();

        return new UserParameters(targetWeight, gender, dateOfBirth, height, weight, lifestyle, formula, nowTimestamp);
    }

    private void fillWithOldUserParameters(UserParameters oldUserParams) {
        EditText dateOfBirthView = findViewById(R.id.date_of_birth);
        EditText heightView = findViewById(R.id.height);
        EditText weightView = findViewById(R.id.weight);
        Spinner genderSpinner = findViewById(R.id.gender_spinner);
        EditText targetWeightView = findViewById(R.id.target_weight);
        Spinner lifestyleSpinner = findViewById(R.id.lifestyle_spinner);
        Spinner formulaSpinner = findViewById(R.id.formula_spinner);

        LocalDate dateOfBirth = oldUserParams.getDateOfBirth();
        dateOfBirthView.setText(dateOfBirth.toString(getString(R.string.date_format)));

        heightView.setText(String.valueOf(oldUserParams.getHeight()));
        weightView.setText(DecimalUtils.toDecimalString(oldUserParams.getWeight()));
        targetWeightView.setText(DecimalUtils.toDecimalString(oldUserParams.getTargetWeight()));

        Gender gender = oldUserParams.getGender();
        genderSpinner.setSelection(Gender.POSITIONS_REVERSED.get(gender) + 1);

        Lifestyle lifestyle = oldUserParams.getLifestyle();
        lifestyleSpinner.setSelection(Lifestyle.POSITIONS_REVERSED.get(lifestyle));

        Formula formula = oldUserParams.getFormula();
        formulaSpinner.setSelection(Formula.POSITIONS_REVERSED.get(formula));
    }

    private LocalDate parseDateOfBirth(String dateString) {
        String[] dateSplited = dateString.split("\\.");
        int day = Integer.parseInt(dateSplited[0]);
        int month = Integer.parseInt(dateSplited[1]);
        int year = Integer.parseInt(dateSplited[2]);
        return new LocalDate(year, month, day);
    }

    private void fillUserName(FullName userFullName) {
        EditText firstNameEditText = findViewById(R.id.first_name);
        firstNameEditText.setText(userFullName.getFirstName());
        EditText lastNameEditText = findViewById(R.id.last_name);

        lastNameEditText.setText(userFullName.getLastName());
    }
}
