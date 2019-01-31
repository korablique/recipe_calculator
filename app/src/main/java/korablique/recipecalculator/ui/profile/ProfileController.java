package korablique.recipecalculator.ui.profile;

import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.FullName;
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
    ProfileController(
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

        Single<Optional<UserParameters>> paramsSingle =
                userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(paramsSingle, (Optional<UserParameters> parameters) -> {
            UserParameters userParameters = parameters.get();
            fillUserData(fragmentView, userParameters);

            Rates rates = RateCalculator.calculate(userParameters);
            fillNutritionRates(fragmentView, rates);
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
        Single<Optional<UserParameters>> paramsSingle =
                userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(paramsSingle, (Optional<UserParameters> parameters) -> {
            if (parameters.isPresent()) {
                View fragmentView = fragment.getView();

                UserParameters userParameters = parameters.get();
                fillUserData(fragmentView, userParameters);

                Rates rates = RateCalculator.calculate(userParameters);
                fillNutritionRates(fragmentView, rates);

                fillUserName(fragmentView, userNameProvider.getUserName());
            }
        });
    }

    private void fillUserData(View fragmentView, UserParameters userParameters) {
        TextView ageTextView = fragmentView.findViewById(R.id.age);
        TextView heightTextView = fragmentView.findViewById(R.id.height);
        TextView targetWeightTextView = fragmentView.findViewById(R.id.target_weight);
        TextView weightTextView = fragmentView.findViewById(R.id.weight_value);

        ageTextView.setText(String.valueOf(userParameters.getAge()));
        heightTextView.setText(String.valueOf(userParameters.getHeight()));
        targetWeightTextView.setText(String.valueOf(userParameters.getTargetWeight()));
        weightTextView.setText(String.valueOf(userParameters.getWeight()));
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
        String nameAndSurname = userFullName.getFirstName() + " " + userFullName.getLastName();
        nameView.setText(nameAndSurname);
    }
}
