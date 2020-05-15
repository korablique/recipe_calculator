package korablique.recipecalculator.ui.userparameters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.redmadrobot.inputmask.MaskedTextChangedListener;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.BuildConfig;
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
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry;
import korablique.recipecalculator.ui.DatePickerFragment;
import korablique.recipecalculator.ui.DecimalUtils;
import korablique.recipecalculator.ui.TextWatcherAfterTextChangedAdapter;
import korablique.recipecalculator.ui.inputfilters.GeneralDateFormatInputFilter;
import korablique.recipecalculator.ui.mainactivity.MainScreenLoader;

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
    @Inject
    MainScreenLoader mainScreenLoader;
    @Inject
    ServerUserParamsRegistry serverUserParamsRegistry;
    private TextWatcher textWatcher = new TextWatcherAfterTextChangedAdapter(editable -> updateSaveButtonEnability());
    private Button saveUserParamsButton;

    @Override
    protected Integer getLayoutId() {
        return R.layout.activity_user_parameters;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViewById(R.id.privacy_policy).setOnClickListener((v) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(getString(R.string.privacy_policy_address)));
            startActivity(browserIntent);
        });

        // гендер
        RadioButton radioMale = findViewById(R.id.radio_male);
        RadioButton radioFemale = findViewById(R.id.radio_female);
        radioMale.setOnCheckedChangeListener((buttonView, isChecked) -> updateSaveButtonEnability());
        radioFemale.setOnCheckedChangeListener((buttonView, isChecked) -> updateSaveButtonEnability());

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
                .withItems(R.array.formula_array)
                .tune();

        saveUserParamsButton = findViewById(R.id.button_save);
        saveUserParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = ((EditText) findViewById(R.id.name)).getText().toString();
                FullName fullName = new FullName(name, "");
                userNameProvider.saveUserName(fullName);
                serverUserParamsRegistry.updateUserNameIgnoreResult(fullName.toString());

                UserParameters userParameters = extractUserParameters();
                Completable callback = userParametersWorker.saveUserParameters(userParameters);
                subscriptions.subscribe(callback, () -> {
                    // если пользователь первый раз открыл приложение и у него ещё нет данных,
                    // то после того, как он их сохранит, открыть MainActivity
                    if (UserParametersActivity.this.isTaskRoot()) {
                        // Закажем загрузку MainActivity и подпишемся на окончание загрузки,
                        // при окончании загрузки завершим себя (finish()).
                        subscriptions.subscribe(
                                mainScreenLoader.loadMainScreenActivity(UserParametersActivity.this),
                                UserParametersActivity.this::finish);
                    } else {
                        finish();
                    }
                });
            }
        });

        DateTime minBirthdayDate = timeProvider.now().minusYears(100);
        DateTime maxBirthdayDate = timeProvider.now().minusYears(14);

        View calendarView = findViewById(R.id.calendar_button);
        calendarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText dateOfBirthView = findViewById(R.id.date_of_birth);
                LocalDate birthday = parseDateOfBirth(dateOfBirthView.getText().toString());

                DatePickerFragment datePickerFragment;
                if (birthday == null) {
                    datePickerFragment = DatePickerFragment.showDialog(
                            getSupportFragmentManager(),
                            minBirthdayDate.getMillis(),
                            maxBirthdayDate.getMillis());
                } else {
                    datePickerFragment = DatePickerFragment.showDialog(
                            getSupportFragmentManager(),
                            birthday,
                            minBirthdayDate.getMillis(),
                            maxBirthdayDate.getMillis());
                }
                datePickerFragment.setOnDateSetListener(new DatePickerFragment.DateSetListener() {
                    @Override
                    public void onDateSet(LocalDate date) {
                        dateOfBirthView.setText(date.toString(getString(R.string.date_format)));
                    }
                });
            }
        });

        EditText birthdayEditText = findViewById(R.id.date_of_birth);
        MaskedTextChangedListener.Companion.installOn(
                birthdayEditText,
                "[09]{.}[09]{.}[0000]",
                (maskFilled, extractedValue, formattedValue) -> {});
        ArrayList<InputFilter> filters = new ArrayList<>(Arrays.asList(birthdayEditText.getFilters()));
        filters.add(new GeneralDateFormatInputFilter(
                minBirthdayDate.getYear(),
                maxBirthdayDate.getYear()));
        birthdayEditText.setFilters(filters.toArray(new InputFilter[0]));
        birthdayEditText.setHint("20.12.1993");

        EditText nameEditText = findViewById(R.id.name);
        nameEditText.addTextChangedListener(textWatcher);
        birthdayEditText.addTextChangedListener(textWatcher);
        EditText targetWeightEditText = findViewById(R.id.target_weight);
        targetWeightEditText.addTextChangedListener(textWatcher);
        EditText heightEditText = findViewById(R.id.height);
        heightEditText.addTextChangedListener(textWatcher);
        EditText weightEditText = findViewById(R.id.weight);
        weightEditText.addTextChangedListener(textWatcher);
        // run once to disable if empty
        updateSaveButtonEnability();

        Single<Optional<UserParameters>> oldUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(oldUserParamsSingle, userParametersOptional -> {
            if (userParametersOptional.isPresent()) {
                UserParameters oldUserParams = userParametersOptional.get();
                fillWithOldUserParameters(oldUserParams);
                fillUserName(userNameProvider.getUserName());
            }
        });

        if (BuildConfig.DEBUG) {
            findViewById(R.id.personal_info_title).setOnClickListener(v -> {
                UserParameters debugUserParams = new UserParameters(
                        65, Gender.MALE, LocalDate.parse("1993-07-15"),
                        165, 62, Lifestyle.PASSIVE_LIFESTYLE,
                        Formula.HARRIS_BENEDICT, 0);
                fillWithOldUserParameters(debugUserParams);
                fillUserName(new FullName("Debug", "Debug"));
            });
        }
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

    private void updateSaveButtonEnability() {
        EditText nameView = findViewById(R.id.name);
        EditText ageView = findViewById(R.id.date_of_birth);
        LocalDate birthday = parseDateOfBirth(ageView.getText().toString());
        EditText heightView = findViewById(R.id.height);
        EditText weightView = findViewById(R.id.weight);
        EditText targetWeight = findViewById(R.id.target_weight);
        RadioButton radioMale = findViewById(R.id.radio_male);
        RadioButton radioFemale = findViewById(R.id.radio_female);
        boolean allFieldsFilled = !nameView.getText().toString().isEmpty()
                && birthday != null
                && !heightView.getText().toString().isEmpty()
                && !weightView.getText().toString().isEmpty()
                && !targetWeight.getText().toString().isEmpty()
                && (radioMale.isChecked() || radioFemale.isChecked());
        saveUserParamsButton.setEnabled(allFieldsFilled);
    }

    private UserParameters extractUserParameters() {
        float targetWeight = Float.parseFloat(((EditText) findViewById(R.id.target_weight)).getText().toString());

        Gender gender;
        RadioButton radioMale = findViewById(R.id.radio_male);
        if (radioMale.isChecked()) {
            gender = Gender.MALE;
        } else {
            gender = Gender.FEMALE;
        }

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
        RadioButton radioMale = findViewById(R.id.radio_male);
        RadioButton radioFemale = findViewById(R.id.radio_female);
        EditText targetWeightView = findViewById(R.id.target_weight);
        Spinner lifestyleSpinner = findViewById(R.id.lifestyle_spinner);
        Spinner formulaSpinner = findViewById(R.id.formula_spinner);

        LocalDate dateOfBirth = oldUserParams.getDateOfBirth();
        dateOfBirthView.setText(dateOfBirth.toString(getString(R.string.date_format)));

        heightView.setText(String.valueOf(oldUserParams.getHeight()));
        weightView.setText(DecimalUtils.toDecimalString(oldUserParams.getWeight()));
        targetWeightView.setText(DecimalUtils.toDecimalString(oldUserParams.getTargetWeight()));

        Gender gender = oldUserParams.getGender();
        if (gender == Gender.MALE) {
            radioMale.setChecked(true);
        } else {
            radioFemale.setChecked(true);
        }

        Lifestyle lifestyle = oldUserParams.getLifestyle();
        lifestyleSpinner.setSelection(Lifestyle.POSITIONS_REVERSED.get(lifestyle));

        Formula formula = oldUserParams.getFormula();
        formulaSpinner.setSelection(Formula.POSITIONS_REVERSED.get(formula));
    }

    @Nullable
    private LocalDate parseDateOfBirth(String dateString) {
        try {
            return LocalDate.parse(dateString, DateTimeFormat.forPattern("dd.MM.yyyy"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void fillUserName(FullName userFullName) {
        EditText nameEditText = findViewById(R.id.name);
        nameEditText.setText(userFullName.toString());
    }
}
