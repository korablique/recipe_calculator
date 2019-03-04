package korablique.recipecalculator.ui.profile;

import android.util.Pair;
import android.util.TimeUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.FullName;
import korablique.recipecalculator.model.GoalCalculator;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.NutritionValuesWrapper;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;
import korablique.recipecalculator.ui.usergoal.UserParametersActivity;

@FragmentScope
public class ProfileController extends FragmentCallbacks.Observer {
    private BaseFragment fragment;
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions subscriptions;
    private UserNameProvider userNameProvider;

    @Inject
    public ProfileController(
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            UserParametersWorker userParametersWorker,
            RxFragmentSubscriptions subscriptions,
            UserNameProvider userNameProvider) {
        this.fragment = fragment;
        fragmentCallbacks.addObserver(this);
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
        this.userNameProvider = userNameProvider;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView) {
        FullName userFullName = userNameProvider.getUserName();
        fillUserName(fragmentView, userFullName);

        Single<Optional<UserParameters>> lastParamsSingle =
                userParametersWorker.requestCurrentUserParameters();

        Single<Optional<UserParameters>> firstParamsSingle =
                userParametersWorker.requestFirstUserParameters();

        Single<Pair<Optional<UserParameters>, Optional<UserParameters>>> singlePair = firstParamsSingle.zipWith(lastParamsSingle,
                Pair::create);

        subscriptions.subscribe(singlePair, new Consumer<Pair<Optional<UserParameters>, Optional<UserParameters>>>() {
            @Override
            public void accept(Pair<Optional<UserParameters>, Optional<UserParameters>> firstAndLastParams) {
                fillProfile(firstAndLastParams.first.get(), firstAndLastParams.second.get(), fragmentView);
            }
        });

        // редактирование профиля
        View editProfileButton = fragmentView.findViewById(R.id.layout_button_edit);
        editProfileButton.setOnClickListener(view -> {
            UserParametersActivity.start(fragment.getContext());
        });
    }

    @Override
    public void onFragmentStart() {
        // обновляет данные пользователя, если они редактировались
        requestFirstAndCurrentUserParams((firstAndLastParams) -> {
            if (!firstAndLastParams.first.isPresent()) {
                throw new IllegalStateException("It is impossible for the first user parameters to be missing");
            }
            if (firstAndLastParams.second.isPresent()) {
                UserParameters firstParams = firstAndLastParams.first.get();
                UserParameters lastParams = firstAndLastParams.second.get();
                fillProfile(firstParams, lastParams, fragment.getView());
            }
        });

        Button setCurrentWeightButton = fragment.getView().findViewById(R.id.set_current_weight);
        setCurrentWeightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // к моменту нажатия на кнопку ввода нового веса данные пользователя могли измениться
                // (он мог уже один раз на неё нажать и изменить вес),
                // поэтому запрашиваем их ещё раз
                requestFirstAndCurrentUserParams((firstAndLastParams) -> {
                    UserParameters lastParams = firstAndLastParams.second.get();
                    NewMeasurementsDialog newMeasurementsDialog =
                            NewMeasurementsDialog.showDialog(fragment.getFragmentManager(), lastParams);
                    newMeasurementsDialog.setOnSaveNewMeasurementsListener(new NewMeasurementsDialog.OnSaveNewMeasurementsListener() {
                        @Override
                        public void onSave(UserParameters newUserParams) {
                            userParametersWorker.saveUserParameters(newUserParams);
                            fillProfile(firstAndLastParams.first.get(), newUserParams,  fragment.getView());
                        }
                    });
                });
            }
        });
    }

    private void requestFirstAndCurrentUserParams(Consumer<Pair<Optional<UserParameters>, Optional<UserParameters>>> consumer) {
        Single<Optional<UserParameters>> lastParamsSingle =
                userParametersWorker.requestCurrentUserParameters();
        Single<Optional<UserParameters>> firstParamsSingle =
                userParametersWorker.requestFirstUserParameters();
        Single<Pair<Optional<UserParameters>, Optional<UserParameters>>> pairSingle =
                firstParamsSingle.zipWith(lastParamsSingle, Pair::create);
        subscriptions.subscribe(pairSingle, consumer);
    }

    private void fillUserData(View fragmentView, UserParameters userParameters) {
        TextView ageTextView = fragmentView.findViewById(R.id.age);
        TextView heightTextView = fragmentView.findViewById(R.id.height);
        TextView targetWeightTextView = fragmentView.findViewById(R.id.target_weight);

        TextView weightMeasurementTextView = fragmentView.findViewById(R.id.current_weight_measurement_value);
        TextView targetWeightMeasurementTextView = fragmentView.findViewById(R.id.target_weight_measurement_value);

        int age = userParameters.getAge();
        String ageString = fragmentView.getResources().getQuantityString(R.plurals.years_old, age, age);
        ageTextView.setText(ageString);

        heightTextView.setText(String.valueOf(userParameters.getHeight()));
        targetWeightTextView.setText(String.valueOf(userParameters.getTargetWeight()));
        weightMeasurementTextView.setText(String.valueOf(userParameters.getWeight()));
        targetWeightMeasurementTextView.setText(String.valueOf(userParameters.getTargetWeight()));

        TextView lastMeasurementDate = fragmentView.findViewById(R.id.last_measurement_date_measurement_value);
        DateTime measurementsDate = new DateTime(userParameters.getMeasurementsTimestamp());
        String measurementsDateString = measurementsDate.toString(fragmentView.getResources().getString(R.string.date_format));
        lastMeasurementDate.setText(measurementsDateString);
    }

    private void fillNutritionRates(View fragmentView, Rates rates) {
        NutritionValuesWrapper nutritionValues = new NutritionValuesWrapper(
                fragmentView.getContext(),
                fragmentView.findViewById(R.id.nutrition_parent_layout));
        Nutrition nutrition = Nutrition.from(rates);
        nutritionValues.setNutrition(nutrition);

        TextView calorieIntakeView = fragmentView.findViewById(R.id.calorie_intake);
        calorieIntakeView.setText(String.valueOf(Math.round(rates.getCalories())));

        PluralProgressBar progressBar = fragmentView.findViewById(R.id.new_nutrition_progress_bar);
        float nutritionSum = (float) (nutrition.getProtein() + nutrition.getFats() + nutrition.getCarbs());
        float proteinPercentage = (float) nutrition.getProtein() / nutritionSum * 100;
        float fatsPercentage = (float) nutrition.getFats() / nutritionSum * 100;
        float carbsPercentage = (float) nutrition.getCarbs() / nutritionSum * 100;
        progressBar.setProgress(proteinPercentage, fatsPercentage, carbsPercentage);
    }

    private void fillUserName(View fragmentView, FullName userFullName) {
        TextView nameView = fragmentView.findViewById(R.id.user_name);
        nameView.setText(userFullName.toString());
    }

    private void setPercentDoneProgress(int percentDone, View fragmentView) {
        if (percentDone < 100) {
            fragmentView.findViewById(R.id.done_percent_sign).setVisibility(View.VISIBLE);
            fragmentView.findViewById(R.id.done_checkmark).setVisibility(View.GONE);
            TextView percentView = fragmentView.findViewById(R.id.done_percent);
            percentView.setVisibility(View.VISIBLE);
            percentView.setText(String.valueOf(percentDone));
        } else {
            fragmentView.findViewById(R.id.done_percent_sign).setVisibility(View.GONE);
            fragmentView.findViewById(R.id.done_checkmark).setVisibility(View.VISIBLE);
            fragmentView.findViewById(R.id.done_percent).setVisibility(View.GONE);
        }
        CircularProgressBar circularProgressBar = fragmentView.findViewById(R.id.circular_progress);
        circularProgressBar.setProgress(percentDone);
    }

    private void fillProfile(UserParameters firstParams, UserParameters lastParams, View fragmentView) {
        fillUserData(fragmentView, lastParams);

        Rates rates = RateCalculator.calculate(lastParams);
        fillNutritionRates(fragmentView, rates);

        fillUserName(fragmentView, userNameProvider.getUserName());

        float currentWeight = lastParams.getWeight();
        float firstWeight = firstParams.getWeight();
        float targetWeight = lastParams.getTargetWeight();
        int percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        setPercentDoneProgress(percentDone, fragmentView);
    }
}
