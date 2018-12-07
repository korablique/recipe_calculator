package korablique.recipecalculator.ui.profile;

import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.UserParameters;

@FragmentScope
public class ProfileController extends FragmentCallbacks.Observer {
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions rxFragmentSubscriptions;

    @Inject
    ProfileController(FragmentCallbacks fragmentCallbacks,
                      UserParametersWorker userParametersWorker,
                      RxFragmentSubscriptions rxFragmentSubscriptions) {
        fragmentCallbacks.addObserver(this);
        this.userParametersWorker = userParametersWorker;
        this.rxFragmentSubscriptions = rxFragmentSubscriptions;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView) {
        TextView ageTextView = fragmentView.findViewById(R.id.age);
        TextView heightTextView = fragmentView.findViewById(R.id.height);
        TextView goaltextView = fragmentView.findViewById(R.id.goal);
        TextView weightTextView = fragmentView.findViewById(R.id.weight_value);

        Single<Optional<UserParameters>> userParamsSingle =
                userParametersWorker.requestCurrentUserParameters();
        rxFragmentSubscriptions.subscribe(userParamsSingle, userParametersOptional -> {
            if (userParametersOptional.isPresent()) {
                UserParameters userParameters = userParametersOptional.get();
                if (userParameters != null) {
                    ageTextView.setText(String.valueOf(userParameters.getAge()));
                    heightTextView.setText(String.valueOf(userParameters.getHeight()));
                    goaltextView.setText(userParameters.getGoal());
                    weightTextView.setText(String.valueOf(userParameters.getWeight()));
                }
            }

        });
    }
}
