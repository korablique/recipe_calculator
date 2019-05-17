package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class MainActivity extends BaseActivity implements HasSupportFragmentInjector {
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
        return MainActivityController.createAndAddToHistoryIntent(context, historyList, date);
    }
}
