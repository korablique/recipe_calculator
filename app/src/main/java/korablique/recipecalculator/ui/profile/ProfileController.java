package korablique.recipecalculator.ui.profile;

import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.UserParametersWorker;

@FragmentScope
public class ProfileController extends FragmentCallbacks.Observer {
    private BaseActivity context;
    @Inject
    UserParametersWorker userParametersWorker;

    @Inject
    ProfileController(BaseActivity context, FragmentCallbacks fragmentCallbacks) {
        this.context = context;
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView) {
        TextView ageTextView = fragmentView.findViewById(R.id.age);
        TextView heightTextView = fragmentView.findViewById(R.id.height);
        TextView goaltextView = fragmentView.findViewById(R.id.goal);
        TextView weightTextView = fragmentView.findViewById(R.id.weight_value);
        userParametersWorker.requestCurrentUserParameters(context, userParameters -> {
            if (userParameters != null) {
                ageTextView.setText(userParameters.getAge());
                heightTextView.setText(userParameters.getHeight());
                goaltextView.setText(userParameters.getGoal());
                weightTextView.setText(userParameters.getWeight());
            }
        });
    }
}
