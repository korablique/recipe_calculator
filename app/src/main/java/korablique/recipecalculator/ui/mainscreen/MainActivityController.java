package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.history.HistoryFragment;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.ui.usergoal.UserParametersActivity;

public class MainActivityController implements ActivityCallbacks.Observer {
    private static final String BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID = "BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID";

    private static final String ACTION_ADD_FOODSTUFFS_TO_HISTORY = "ACTION_ADD_FOODSTUFFS_TO_HISTORY";
    public static final String ACTION_OPEN_MAIN_SCREEN = "ACTION_OPEN_MAIN_SCREEN";

    private static final String EXTRA_FOODSTUFFS_LIST = "EXTRA_FOODSTUFFS_LIST";
    public static final String EXTRA_MAIN_SCREEN_INITIAL_DATA = "EXTRA_MAIN_SCREEN_INITIAL_DATA";
    private static final String EXTRA_DATE = "EXTRA_DATE";

    private MainActivity context;
    private UserParametersWorker userParametersWorker;
    private RxActivitySubscriptions subscriptions;
    private BottomNavigationView bottomNavigationView;

    public MainActivityController(
            MainActivity context,
            ActivityCallbacks activityCallbacks,
            UserParametersWorker userParametersWorker,
            RxActivitySubscriptions subscriptions) {
        this.context = context;
        activityCallbacks.addObserver(this);
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        context.setContentView(R.layout.activity_main_screen);

        Single<Optional<UserParameters>> userParamsSingle = userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(userParamsSingle, userParametersOptional -> {
            if (!userParametersOptional.isPresent()) {
                UserParametersActivity.start(context);
                context.finish();
            }
        });

        bottomNavigationView = context.findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_foodstuffs:
                    MainScreenFragment.show(context.getSupportFragmentManager(), true);
                    break;
                case R.id.menu_item_history:
                    HistoryFragment.show(context.getSupportFragmentManager());
                    break;
                case R.id.menu_item_profile:
                    ProfileFragment.show(context.getSupportFragmentManager());
                    break;
            }
            return true;
        });
        // обработка нажатий назад
        FragmentManager fragmentManager = context.getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);

                if (f instanceof MainScreenFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.menu_item_foodstuffs);
                } else if (f instanceof HistoryFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.menu_item_history);
                } else if (f instanceof ProfileFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.menu_item_profile);
                }
            }
        }, false);


        boolean fragmentStarted = tryStartFragmentByIntent(context.getIntent(), savedInstanceState);
        if (!fragmentStarted && !isAnyFragmentDisplayed()) {
            // если ни один фрагмент не показан (приложение только что запущено)
            MainScreenFragment.show(context.getSupportFragmentManager(), false);
        }
    }

    private boolean tryStartFragmentByIntent(@Nullable Intent intent, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Мы уже были запущены и должны быть восстановлены ОСью - нужные фрагменты с их
            // контроллерами будут пересозданы сами по себе и получат сохранённый стейт.
            return false;
        }

        if (intent == null) {
            return false;
        }

        if (ACTION_ADD_FOODSTUFFS_TO_HISTORY.equals(intent.getAction())) {
            List<WeightedFoodstuff> foodstuffs = intent.getParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST);
            if (foodstuffs == null) {
                throw new IllegalArgumentException("Need " + EXTRA_FOODSTUFFS_LIST);
            }
            // если selectedDate == null - дата сегодняшняя
            LocalDate selectedDate = (LocalDate) intent.getSerializableExtra(EXTRA_DATE);
            HistoryFragment.show(context.getSupportFragmentManager(), selectedDate, foodstuffs);
            return true;
        }

        boolean shouldPutMainFragmentToBackStack = isAnyFragmentDisplayed();
        if (ACTION_OPEN_MAIN_SCREEN.equals(intent.getAction())) {
            MainScreenFragment.show(
                    context.getSupportFragmentManager(),
                    shouldPutMainFragmentToBackStack,
                    intent.getBundleExtra(EXTRA_MAIN_SCREEN_INITIAL_DATA));
            return true;
        }

        return false;
    }

    private boolean isAnyFragmentDisplayed() {
        return context.getSupportFragmentManager().findFragmentById(R.id.main_container) != null;
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        outState.putInt(BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID, bottomNavigationView.getSelectedItemId());
    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {
        bottomNavigationView.setSelectedItemId(savedInstanceState.getInt(BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID));
    }

    public static Intent createAddToHistoryIntent(Context context, List<WeightedFoodstuff> historyList, LocalDate date) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(historyList));
        intent.putExtra(EXTRA_DATE, date);
        intent.setAction(ACTION_ADD_FOODSTUFFS_TO_HISTORY);
        return intent;
    }

    public static Intent createMainScreenIntent(
            Context context, ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        Intent intent = new Intent(context, MainActivity.class);
        Bundle bundle = MainScreenController.createInitialDataBundle(top, allFoodstuffsFirstBatch);
        intent.putExtra(EXTRA_MAIN_SCREEN_INITIAL_DATA, bundle);
        intent.setAction(ACTION_OPEN_MAIN_SCREEN);
        return intent;
    }
}
