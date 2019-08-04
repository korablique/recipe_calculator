package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.LocalDate;

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

    public static void show(FragmentManager fragmentManager, boolean addToBackStack) {
        showImpl(fragmentManager, addToBackStack, null);
    }

    public static void show(FragmentManager fragmentManager, boolean addToBackStack, Bundle initialData) {
        showImpl(fragmentManager, addToBackStack, initialData);
    }

    public static void show(FragmentManager fragmentManager, Bundle initialData) {
        showImpl(fragmentManager, true, initialData);
    }

    private static void showImpl(FragmentManager fragmentManager, boolean addToBackStack, @Nullable Bundle initialData) {
        Fragment existingFragment = fragmentManager.findFragmentById(R.id.main_container);
        if (existingFragment instanceof MainScreenFragment) {
            if (initialData == null) {
                // Фрагмент уже есть, никаких конкретных данных для показа нам не дали -
                // существующий фрагмент пойдёт.
                return;
            } else {
                // Если сейчас показан фрагмент главного экрана, удаляем его, чтобы
                // 2 одинаковых фрагмента не были в back stack'е.
                fragmentManager.beginTransaction().remove(existingFragment).commit();
            }
        }

        Fragment mainScreenFragment = new MainScreenFragment();
        if (initialData != null) {
            mainScreenFragment.setArguments(initialData);
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, mainScreenFragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
