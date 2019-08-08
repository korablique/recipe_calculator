package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.LocalDate;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;


public class MainScreenFragment extends BaseFragment {
    @Inject
    MainScreenController controller;
    @Inject
    UpFABController upFABController;

    @Override
    public View createView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_screen, container, false);
    }

    public static void show(FragmentManager fragmentManager, boolean addToBackStack) {
        // чтобы не пересоздавать фрагмент, который уже показан прямо сейчас
        // и чтобы сохранялся его стейт (потому что при пересоздании фрагмента стейт потеряется)
        if (fragmentManager.findFragmentById(R.id.main_container) instanceof MainScreenFragment) {
            return;
        }
        Fragment mainScreenFragment = new MainScreenFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, mainScreenFragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public static void show(FragmentManager fragmentManager, LocalDate date) {
        MainScreenController.show(fragmentManager, date);
    }
}
