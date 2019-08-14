package korablique.recipecalculator.ui.mainactivity.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class HistoryFragment extends BaseFragment {
    @Inject
    HistoryController historyController;
    private boolean viewCreated;
    private final List<Runnable> delayedActions = new ArrayList<>();

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewCreated = true;
        for (Runnable action : delayedActions) {
            action.run();
        }
        delayedActions.clear();
    }

    public void addFoodstuffs(LocalDate date, List<WeightedFoodstuff> foodstuffs) {
        // The method can be called when the fragment is not fully created yet and doesn't
        // have a controller and a view - we need to delay the action in such a case to avoid crashes.
        if (viewCreated) {
            historyController.addFoodstuffs(date, foodstuffs);
        } else {
            delayedActions.add(() -> {
                addFoodstuffs(date, foodstuffs);
            });
        }
    }
}
