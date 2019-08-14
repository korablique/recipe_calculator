package korablique.recipecalculator.ui.mainactivity.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.UserNameProvider;

public class ProfileFragment extends BaseFragment {
    @Inject
    ProfileController controller;
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxFragmentSubscriptions subscriptions;
    @Inject
    UserNameProvider userNameProvider;

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
}
