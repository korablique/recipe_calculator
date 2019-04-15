package korablique.recipecalculator.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;

public class HistoryFragment extends BaseFragment {
    @Inject
    HistoryController historyController;

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    public static void show(FragmentManager fragmentManager) {
        // чтобы не пересоздавать фрагмент, который уже показан прямо сейчас
        // и чтобы сохранялся его стейт (потому что при пересоздании фрагмента стейт потеряется)
        if (fragmentManager.findFragmentById(R.id.main_container) instanceof HistoryFragment) {
            return;
        }
        Fragment historyFragment = new HistoryFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, historyFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
