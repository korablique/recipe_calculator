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
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.ui.bucketlist.BucketListActivity.EXTRA_FOODSTUFFS_LIST;

public class HistoryFragment extends BaseFragment {
    @Inject
    HistoryController historyController;

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    public static void show(FragmentManager fragmentManager) {
        Fragment historyFragment = new HistoryFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, historyFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public static void show(FragmentManager fragmentManager, List<WeightedFoodstuff> foodstuffs) {
        Fragment historyFragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(foodstuffs));
        historyFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, historyFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
