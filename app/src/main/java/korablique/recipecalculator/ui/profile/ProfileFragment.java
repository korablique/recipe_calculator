package korablique.recipecalculator.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.UserParameters;

public class ProfileFragment extends BaseFragment {
    @Inject
    ProfileController controller;
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxFragmentSubscriptions subscriptions;

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_profile, container, false);

        Single<Optional<UserParameters>> paramsSingle =
                userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(paramsSingle, (Optional<UserParameters> parameters) -> {
            initializeFragment(parameters.get(), fragmentView);
        });

        return fragmentView;
    }

    public static void show(FragmentActivity context) {
        Fragment profileFragment = new ProfileFragment();
        FragmentTransaction transaction = context.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_container, profileFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void initializeFragment(UserParameters userParameters, View fragmentView) {
        ((TextView) fragmentView.findViewById(R.id.age)).setText(String.valueOf(userParameters.getAge()));
        ((TextView) fragmentView.findViewById(R.id.height)).setText(String.valueOf(userParameters.getHeight()));
        ((TextView) fragmentView.findViewById(R.id.goal)).setText(userParameters.getGoal());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String userName = prefs.getString(getString(R.string.user_name), "");
        String userSurname = prefs.getString(getString(R.string.user_surname), "");
        String nameStr = userName + " " + userSurname;
        ((TextView) fragmentView.findViewById(R.id.user_name)).setText(nameStr);
    }
}
