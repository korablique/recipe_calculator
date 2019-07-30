package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.ui.mainscreen.MainActivityController.createAddToHistoryIntent;
import static korablique.recipecalculator.ui.mainscreen.MainActivityController.createMainScreenIntent;

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

    public static void start(
            Context context, ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        Intent intent = createMainScreenIntent(context, top, allFoodstuffsFirstBatch);
        context.startActivity(intent);
    }

    public static void openHistoryAndAddFoodstuffs(
            Context context,
            List<WeightedFoodstuff> historyList,
            @NotNull LocalDate selectedDate) {
        context.startActivity(createAddToHistoryIntent(context, historyList, selectedDate));
    }
}
