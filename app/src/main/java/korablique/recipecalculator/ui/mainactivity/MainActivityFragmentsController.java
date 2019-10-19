package korablique.recipecalculator.ui.mainactivity;

import android.os.Bundle;
import android.util.Pair;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenFragment;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileFragment;
import korablique.recipecalculator.session.SessionClient;
import korablique.recipecalculator.session.SessionController;

/**
 * Controls main-activity fragments initialization and switching between them.
 */
@ActivityScope
public class MainActivityFragmentsController implements
        ActivityCallbacks.Observer, SessionController.Observer {
    private static final String BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID = "BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID";
    private static final String EXTRA_MAIN_SCREEN_ARGUMENTS = "EXTRA_MAIN_SCREEN_ARGUMENTS";
    private final MainActivity mainActivity;
    private final SessionController sessionController;
    private final List<Observer> observers = new ArrayList<>();

    private BottomNavigationView bottomNavigationView;

    private Fragment currentFragment;
    private ProfileFragment profileFragment;
    private HistoryFragment historyFragment;
    private MainScreenFragment mainScreenFragment;

    public interface Observer {
        void onFragmentSwitch(Fragment oldShownFragment, Fragment newShownFragment);
    }

    @Inject
    public MainActivityFragmentsController(
            MainActivity mainActivity,
            SessionController sessionController,
            ActivityCallbacks activityCallbacks) {
        this.mainActivity = mainActivity;
        this.sessionController = sessionController;
        activityCallbacks.addObserver(this);
        sessionController.addObserver(this);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        FragmentManager fm = mainActivity.getSupportFragmentManager();
        if (savedInstanceState == null) {
            mainScreenFragment = new MainScreenFragment();
            Bundle mainScreenInitData = mainActivity.getIntent().getBundleExtra(EXTRA_MAIN_SCREEN_ARGUMENTS);
            if (mainScreenInitData != null) {
                mainScreenFragment.setArguments(mainScreenInitData);
            }
            profileFragment = new ProfileFragment();
            historyFragment = new HistoryFragment();
            fm.beginTransaction()
                    .add(R.id.main_container, profileFragment).hide(profileFragment)
                    .add(R.id.main_container, historyFragment).hide(historyFragment)
                    .add(R.id.main_container, mainScreenFragment).show(mainScreenFragment)
                    .commit();
            currentFragment = mainScreenFragment;
        } else {
            for (Fragment fragment : fm.getFragments()) {
                if (fragment instanceof ProfileFragment) {
                    profileFragment = (ProfileFragment) fragment;
                    if (!profileFragment.isHidden()) {
                        currentFragment = profileFragment;
                    }
                } else if (fragment instanceof HistoryFragment) {
                    historyFragment = (HistoryFragment) fragment;
                    if (!historyFragment.isHidden()) {
                        currentFragment = historyFragment;
                    }
                } else if (fragment instanceof MainScreenFragment) {
                    mainScreenFragment = (MainScreenFragment) fragment;
                    if (!mainScreenFragment.isHidden()) {
                        currentFragment = mainScreenFragment;
                    }
                }
            }
        }

        bottomNavigationView = mainActivity.findViewById(R.id.navigation);
        // We save and restore bottom bar's state ourselves (onRestoreInstanceState is called
        // after onCreate, but we check and start new session in onCreate - if view would restore
        // its state by itself, it would override our intention of starting new state).
        bottomNavigationView.setSaveEnabled(false);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_foodstuffs:
                    switchToFragment(mainScreenFragment);
                    break;
                case R.id.menu_item_history:
                    switchToFragment(historyFragment);
                    break;
                case R.id.menu_item_profile:
                    switchToFragment(profileFragment);
                    break;
            }
            return true;
        });

        if (savedInstanceState != null) {
            bottomNavigationView.setSelectedItemId(savedInstanceState.getInt(BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID));
        }

        if (sessionController.shouldStartNewSessionFor(SessionClient.MAIN_ACTIVITY_FRAGMENT)) {
            onNewSession();
        }
    }

    private void switchToFragment(Fragment newShownFragment) {
        if (currentFragment != newShownFragment) {
            Fragment oldShownFragment = currentFragment;
            mainActivity.getSupportFragmentManager()
                    .beginTransaction().hide(oldShownFragment).show(newShownFragment).commit();
            currentFragment = newShownFragment;
            for (Observer observer : observers) {
                observer.onFragmentSwitch(oldShownFragment, newShownFragment);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        outState.putInt(BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID, bottomNavigationView.getSelectedItemId());
    }

    @Override
    public boolean onActivityBackPressed() {
        if (!(currentFragment instanceof MainScreenFragment)) {
            bottomNavigationView.setSelectedItemId(R.id.menu_item_foodstuffs);
            // Если сейчас показан не главный фрагмент - покажем главный и поглотим событие
            return true;
        }
        return false;
    }

    public static Pair<String, Bundle> createMainScreenInitialDataBundle(
            ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        Bundle bundle = MainScreenFragment.createArguments(top, allFoodstuffsFirstBatch);
        return Pair.create(EXTRA_MAIN_SCREEN_ARGUMENTS, bundle);
    }

    public void showMainScreen() {
        bottomNavigationView.setSelectedItemId(R.id.menu_item_foodstuffs);
    }

    public void addFoodstuffsToHistory(LocalDate selectedDate, List<WeightedFoodstuff> foodstuffs) {
        bottomNavigationView.setSelectedItemId(R.id.menu_item_history);
        historyFragment.addFoodstuffs(selectedDate, foodstuffs);
    }

    @Override
    public void onNewSession() {
        // В начале новой сессии меняем активный фрагмент на MainScreen
        bottomNavigationView.setSelectedItemId(R.id.menu_item_foodstuffs);
        sessionController.onClientStartedNewSession(SessionClient.MAIN_ACTIVITY_FRAGMENT);
    }

    @Override
    public void onActivityDestroy() {
        sessionController.removeObserver(this);
    }
}
