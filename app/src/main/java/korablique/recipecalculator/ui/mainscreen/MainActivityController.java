package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;

import androidx.annotation.Nullable;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;

@ActivityScope
public class MainActivityController implements ActivityCallbacks.Observer {
    private static final String ACTION_ADD_FOODSTUFFS_TO_HISTORY = "ACTION_ADD_FOODSTUFFS_TO_HISTORY";
    public static final String ACTION_OPEN_MAIN_SCREEN = "ACTION_OPEN_MAIN_SCREEN";

    private static final String EXTRA_FOODSTUFFS_LIST = "EXTRA_FOODSTUFFS_LIST";
    private static final String EXTRA_DATE = "EXTRA_DATE";

    private MainActivity context;
    private MainActivityFragmentsController fragmentsController;

    @Inject
    public MainActivityController(
            MainActivity context,
            ActivityCallbacks activityCallbacks,
            MainActivityFragmentsController fragmentsController) {
        this.context = context;
        activityCallbacks.addObserver(this);
        this.fragmentsController = fragmentsController;
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        Intent intent = context.getIntent();
        if (intent != null) {
            handleIntent(intent, savedInstanceState);
        }
    }

    @Override
    public void onActivityNewIntent(Intent intent) {
        handleIntent(intent, null);
    }

    private void handleIntent(Intent intent, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return;
        }

        if (ACTION_OPEN_MAIN_SCREEN.equals(intent.getAction())) {
            fragmentsController.showMainScreen();
        }

        if (ACTION_ADD_FOODSTUFFS_TO_HISTORY.equals(intent.getAction())) {
            List<WeightedFoodstuff> foodstuffs = intent.getParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST);
            LocalDate selectedDate = (LocalDate) intent.getSerializableExtra(EXTRA_DATE);
            fragmentsController.addFoodstuffsToHistory(selectedDate, foodstuffs);
        }
    }

    public static void openMainScreen(
            Context context, ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        context.startActivity(createMainScreenIntent(context, top, allFoodstuffsFirstBatch));
    }

    public static Intent createMainScreenIntent(
            Context context, ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        Intent intent = new Intent(context, MainActivity.class);
        Pair<String, Bundle> mainScreenInitialData =
                MainActivityFragmentsController.createMainScreenInitialDataBundle(
                        top, allFoodstuffsFirstBatch);
        intent.putExtra(mainScreenInitialData.first, mainScreenInitialData.second);
        intent.setAction(ACTION_OPEN_MAIN_SCREEN);
        return intent;
    }

    public static void openHistoryAndAddFoodstuffs(
            Context context,
            List<WeightedFoodstuff> historyList,
            LocalDate selectedDate) {
        context.startActivity(createAddToHistoryIntent(context, historyList, selectedDate));
    }

    public static Intent createAddToHistoryIntent(Context context, List<WeightedFoodstuff> historyList, LocalDate date) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(historyList));
        intent.putExtra(EXTRA_DATE, date);
        intent.setAction(ACTION_ADD_FOODSTUFFS_TO_HISTORY);
        return intent;
    }
}
