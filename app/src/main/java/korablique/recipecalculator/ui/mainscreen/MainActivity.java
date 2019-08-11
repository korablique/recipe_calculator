package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;

import androidx.fragment.app.Fragment;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class MainActivity extends BaseActivity implements HasSupportFragmentInjector {
    @Inject
    MainActivityController controller;
    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }

    @Override
    protected Integer getLayoutId() {
        return R.layout.activity_main_screen;
    }

    public static void openMainScreen(
            Context context, ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        MainActivityController.openMainScreen(context, top, allFoodstuffsFirstBatch);
    }

    public static void openHistoryAndAddFoodstuffs(
            Context context,
            List<WeightedFoodstuff> historyList,
            LocalDate selectedDate) {
        MainActivityController.openHistoryAndAddFoodstuffs(context, historyList, selectedDate);
    }
}
