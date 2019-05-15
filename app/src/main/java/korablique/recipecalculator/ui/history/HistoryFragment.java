package korablique.recipecalculator.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.joda.time.LocalDate;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.ui.mainscreen.MainScreenFragment.SELECTED_DATE;

public class HistoryFragment extends BaseFragment {
    public static final String EXTRA_FOODSTUFFS_LIST = "EXTRA_FOODSTUFFS_LIST";
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

    /**
     * @param date date on which to save foodstuffs to history
     */
    public static void show(FragmentManager fragmentManager, LocalDate date, List<WeightedFoodstuff> foodstuffs) {
        Fragment historyFragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(foodstuffs));
        args.putSerializable(SELECTED_DATE, date);
        historyFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, historyFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
