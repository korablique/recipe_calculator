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
import androidx.annotation.StringRes;

import com.crashlytics.android.Crashlytics;
import com.redmadrobot.inputmask.MaskedTextChangedListener;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import korablique.recipecalculator.ui.inputfilters.NumericBoundsInputFilter;
import korablique.recipecalculator.ui.mainactivity.MainScreenLoader;

import static korablique.recipecalculator.util.SpinnerTuner.startTuningSpinner;

public class UserParametersActivity extends BaseActivity {
    private static final int MIN_AGE = 14;
    private static final int MAX_AGE = 100;
    private static final int MIN_WEIGHT = 35;
    private static final int MAX_WEIGHT = 300;
    private static final int MIN_HEIGHT = 130;
    private static final int MAX_HEIGHT = 300;

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

    private Button saveUserParamsButton;
    private EditText nameEditText;
    private EditText weightEditText;
    private EditText targetWeightEditText;
    private EditText heightEditText;
    private EditText birthdayEditText;
    private RadioButton radioMale;
    private RadioButton radioFemale;

    private Set<EditText> editedEditTexts = new HashSet<>();

    @Override
    protected Integer getLayoutId() {
        return R.layout.activity_user_parameters;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        saveUserParamsButton = findViewById(R.id.button_save);
        nameEditText = findViewById(R.id.name);
        weightEditText = findViewById(R.id.weight);
        targetWeightEditText = findViewById(R.id.target_weight);
        heightEditText = findViewById(R.id.height);
        birthdayEditText = findViewById(R.id.date_of_birth);
        radioMale = findViewById(R.id.radio_male);
        radioFemale = findViewById(R.id.radio_female);

        findViewById(R.id.privacy_policy).setOnClickListener((v) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(getString(R.string.privacy_policy_address)));
            startActivity(browserIntent);
        });

        // гендер
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

        saveUserParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameEditText.getText().toString();
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

        findViewById(R.id.calendar_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDate birthday = parseDateOfBirth(birthdayEditText.getText().toString());

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
                datePickerFragment.setOnDateSetListener(date -> {
                    birthdayEditText.setText(date.toString(getString(R.string.date_format)));
                });
            }
        });

        MaskedTextChangedListener.Companion.installOn(
                birthdayEditText,
                "[09]{.}[09]{.}[0000]",
                (maskFilled, extractedValue, formattedValue) -> {});
        birthdayEditText.setHint("20.12.1993");
        addInputFilter(birthdayEditText, new GeneralDateFormatInputFilter(
                minBirthdayDate.getYear(),
                maxBirthdayDate.getYear()));

        // 0 is min because to type "65" the user first needs to type "6", only then "5".
        addInputFilter(weightEditText, NumericBoundsInputFilter.withBounds(0, MAX_WEIGHT));
        addInputFilter(targetWeightEditText, NumericBoundsInputFilter.withBounds(0, MAX_WEIGHT));
        addInputFilter(heightEditText, NumericBoundsInputFilter.withBounds(0, MAX_HEIGHT));

        TextWatcher textWatcher =
                new TextWatcherAfterTextChangedAdapter(editable -> updateSaveButtonEnability());
        birthdayEditText.addTextChangedListener(textWatcher);
        nameEditText.addTextChangedListener(textWatcher);
        targetWeightEditText.addTextChangedListener(textWatcher);
        heightEditText.addTextChangedListener(textWatcher);
        weightEditText.addTextChangedListener(textWatcher);

        View.OnFocusChangeListener focusWatcher = (v, hasFocus) -> {
            if (!hasFocus) {
                // lost focus
                editedEditTexts.add((EditText) v);
                updateAllHintsStates();
            }
        };
        birthdayEditText.setOnFocusChangeListener(focusWatcher);
        nameEditText.setOnFocusChangeListener(focusWatcher);
        targetWeightEditText.setOnFocusChangeListener(focusWatcher);
        heightEditText.setOnFocusChangeListener(focusWatcher);
        weightEditText.setOnFocusChangeListener(focusWatcher);

        // run once to disable if empty
        updateSaveButtonEnability();

        Single<Optional<UserParameters>> oldUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(oldUserParamsSingle, userParametersOptional -> {
            if (userParametersOptional.isPresent()) {
                UserParameters oldUserParams = userParametersOptional.get();
                fillWithOldUserParameters(oldUserParams);
                nameEditText.setText(userNameProvider.getUserName().toString());
            } else {
                // Let's not confuse the user by showing them complex formulas names on first start
                findViewById(R.id.formula_text_view).setVisibility(View.GONE);
                findViewById(R.id.formula_spinner).setVisibility(View.GONE);
                findViewById(R.id.line4).setVisibility(View.GONE);
            }
        });

        if (BuildConfig.DEBUG) {
            findViewById(R.id.personal_info_title).setOnClickListener(v -> {
                UserParameters debugUserParams = new UserParameters(
                        65, Gender.MALE, LocalDate.parse("1993-07-15"),
                        165, 62, Lifestyle.PASSIVE_LIFESTYLE,
                        Formula.HARRIS_BENEDICT, 0);
                fillWithOldUserParameters(debugUserParams);
                nameEditText.setText("Debug Name");
            });
        }
    }

    void addInputFilter(EditText editText, InputFilter filter) {
        ArrayList<InputFilter> filters = new ArrayList<>(Arrays.asList(editText.getFilters()));
        filters.add(filter);
        editText.setFilters(filters.toArray(new InputFilter[0]));
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

    private void updateAllHintsStates() {
        updateIvalidValuesHints();
        updateSaveButtonEnability();
    }

    private void updateIvalidValuesHints() {
        if (parseDateOfBirth(birthdayEditText.getText().toString()) != null) {
            birthdayEditText.setError(null);
        } else if (editedEditTexts.contains(birthdayEditText)) {
            birthdayEditText.setError("Упс, дата какая-то не такая!");
        }

        if (!nameEditText.getText().toString().trim().isEmpty()) {
            nameEditText.setError(null);
        } else if (editedEditTexts.contains(nameEditText)) {
            nameEditText.setError("А как же вас зовут?");
        }

        setUpNumberFieldErrorHint(
                weightEditText, MIN_WEIGHT, MAX_WEIGHT,
                R.string.user_weight_invalid_format, R.string.user_weight_too_small, R.string.user_weight_too_big);
        setUpNumberFieldErrorHint(
                targetWeightEditText, MIN_WEIGHT, MAX_WEIGHT,
                R.string.user_weight_invalid_format, R.string.user_weight_too_small, R.string.user_weight_too_big);
        setUpNumberFieldErrorHint(
                heightEditText, MIN_HEIGHT, MAX_HEIGHT,
                R.string.user_height_invalid_format, R.string.user_height_too_small, R.string.user_height_too_big);
    }

    private void setUpNumberFieldErrorHint(EditText numberField,
                                           int min,
                                           int max,
                                           @StringRes int invalidFormatHint,
                                           @StringRes int valBelowMinHit,
                                           @StringRes int valAboveMaxHint) {
        if (isNumberFieldValid(numberField, min, max)) {
            numberField.setError(null);
        } else if (editedEditTexts.contains(numberField)) {
            Integer weight;
            try {
                weight = Integer.valueOf(numberField.getText().toString());
            } catch (NumberFormatException e) {
                weight = null;
            }
            if (weight == null) {
                numberField.setError(getText(invalidFormatHint));
            } else if (weight < min) {
                numberField.setError(getText(valBelowMinHit));
            } else if (weight > max) {
                numberField.setError(getText(valAboveMaxHint));
            }
        }
    }

    private void updateSaveButtonEnability() {
        RadioButton radioMale = findViewById(R.id.radio_male);
        RadioButton radioFemale = findViewById(R.id.radio_female);
        boolean allFieldsFilled = !nameEditText.getText().toString().trim().isEmpty()
                && parseDateOfBirth(birthdayEditText.getText().toString()) != null
                && isNumberFieldValid(heightEditText, MIN_HEIGHT, MAX_HEIGHT)
                && isNumberFieldValid(weightEditText, MIN_WEIGHT, MAX_WEIGHT)
                && isNumberFieldValid(targetWeightEditText, MIN_WEIGHT, MAX_WEIGHT)
                && (radioMale.isChecked() || radioFemale.isChecked());
        saveUserParamsButton.setEnabled(allFieldsFilled);
    }

    private boolean isNumberFieldValid(EditText numberField, int min, int max) {
        if (numberField.getText().toString().isEmpty()) {
            return false;
        }
        int number;
        try {
            number = Integer.parseInt(numberField.getText().toString());
        } catch (NumberFormatException e) {
            return false;
        }
        return min <= number && number <= max;
    }

    private UserParameters extractUserParameters() {
        float targetWeight = Float.parseFloat(targetWeightEditText.getText().toString());

        Gender gender;
        if (radioMale.isChecked()) {
            gender = Gender.MALE;
        } else {
            gender = Gender.FEMALE;
        }

        String dateOfBirthString = birthdayEditText.getText().toString();
        LocalDate dateOfBirth = parseDateOfBirth(dateOfBirthString);

        int height = Integer.parseInt(heightEditText.getText().toString());
        float weight = Float.parseFloat(weightEditText.getText().toString());

        int lifestyleSelectedPosition = ((Spinner) findViewById(R.id.lifestyle_spinner)).getSelectedItemPosition();
        Lifestyle lifestyle = Lifestyle.POSITIONS.get(lifestyleSelectedPosition);

        int formulaSelectedPosition = ((Spinner) findViewById(R.id.formula_spinner)).getSelectedItemPosition();
        Formula formula = Formula.POSITIONS.get(formulaSelectedPosition);

        long nowTimestamp = timeProvider.nowUtc().getMillis();

        return new UserParameters(targetWeight, gender, dateOfBirth, height, weight, lifestyle, formula, nowTimestamp);
    }

    private void fillWithOldUserParameters(UserParameters oldUserParams) {
        Spinner lifestyleSpinner = findViewById(R.id.lifestyle_spinner);
        Spinner formulaSpinner = findViewById(R.id.formula_spinner);

        LocalDate dateOfBirth = oldUserParams.getDateOfBirth();
        birthdayEditText.setText(dateOfBirth.toString(getString(R.string.date_format)));

        heightEditText.setText(String.valueOf(oldUserParams.getHeight()));
        weightEditText.setText(DecimalUtils.toDecimalString(oldUserParams.getWeight()));
        targetWeightEditText.setText(DecimalUtils.toDecimalString(oldUserParams.getTargetWeight()));

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
}
