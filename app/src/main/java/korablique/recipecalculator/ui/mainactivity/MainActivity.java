package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
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
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer;

public class MainActivity extends BaseActivity implements HasSupportFragmentInjector {
    @Inject
    MainActivityController controller;
    @Inject
    InteractiveServerUserParamsObtainer interactiveServerUserParamsObtainer;
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
            Activity parent, ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        MainActivityController.openMainScreen(parent, top, allFoodstuffsFirstBatch);
    }

    public static void openHistoryAndAddFoodstuffs(
            Context context,
            List<WeightedFoodstuff> historyList,
            LocalDate selectedDate) {
        MainActivityController.openHistoryAndAddFoodstuffs(context, historyList, selectedDate);
    }

    /**
     * Don't call if {@link #getContentView()} returns null!
     */
    public void openFoodstuffCard(Foodstuff foodstuff) {
        controller.openFoodstuffCard(foodstuff);
    }
}
