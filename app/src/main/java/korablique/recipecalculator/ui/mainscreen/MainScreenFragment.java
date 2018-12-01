package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;


public class MainScreenFragment extends BaseFragment {
    @Inject
    MainScreenController controller;

    @Override
    public View createView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_screen, container, false);
    }

    public static void show(FragmentActivity context) {
        Fragment mainScreenFragment = new MainScreenFragment();
        FragmentTransaction transaction = context.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_container, mainScreenFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
