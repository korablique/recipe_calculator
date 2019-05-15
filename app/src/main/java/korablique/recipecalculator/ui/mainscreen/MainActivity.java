package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.ui.history.HistoryFragment.EXTRA_FOODSTUFFS_LIST;
import static korablique.recipecalculator.ui.mainscreen.MainScreenFragment.SELECTED_DATE;

public class MainActivity extends BaseActivity implements HasSupportFragmentInjector {
    public static final String ACTION_ADD_FOODSTUFFS_TO_HISTORY = "ACTION_ADD_FOODSTUFFS_TO_HISTORY";
    @Inject
    MainActivityController controller;
    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;
    @Inject
    TimeProvider timeProvider;

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    public static void openHistoryAndAddFoodstuffs(
            Context context,
            List<WeightedFoodstuff> historyList,
            @NotNull LocalDate selectedDate) {
        context.startActivity(createAddToHistoryIntent(context, historyList, selectedDate));
    }

    public static Intent createAddToHistoryIntent(
            Context context,
            List<WeightedFoodstuff> historyList,
            LocalDate date) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(historyList));
        intent.putExtra(SELECTED_DATE, date);
        intent.setAction(ACTION_ADD_FOODSTUFFS_TO_HISTORY);
        return intent;
    }
}
