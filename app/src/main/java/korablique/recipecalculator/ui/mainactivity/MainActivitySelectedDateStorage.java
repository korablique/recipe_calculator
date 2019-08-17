package korablique.recipecalculator.ui.mainactivity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.session.SessionClient;
import korablique.recipecalculator.session.SessionController;

/**
 * Utility class to store selected by user date and pass it across all fragments of main activity.
 */
@ActivityScope
public class MainActivitySelectedDateStorage implements
        ActivityCallbacks.Observer, SessionController.Observer {
    private static final String EXTRA_SELECTED_DATE = "EXTRA_SELECTED_DATE";
    private final MainActivity mainActivity;
    private final SessionController sessionController;
    private final TimeProvider timeProvider;
    @Nullable
    private LocalDate selectedDate;

    @Inject
    public MainActivitySelectedDateStorage(
            MainActivity mainActivity,
            ActivityCallbacks activityCallbacks,
            SessionController sessionController,
            TimeProvider timeProvider) {
        this.mainActivity = mainActivity;
        this.sessionController = sessionController;
        this.timeProvider = timeProvider;
        sessionController.addObserver(this);
        activityCallbacks.addObserver(this);

        // If the activity is already created - perform initialization.
        if (mainActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            onActivityCreate(mainActivity.getSavedInstanceState());
        }
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        if (selectedDate != null) {
            outState.putString(EXTRA_SELECTED_DATE, selectedDate.toString());
        }
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SELECTED_DATE)) {
            selectedDate = LocalDate.parse(savedInstanceState.getString(EXTRA_SELECTED_DATE));
        }
        if (selectedDate == null) {
            selectedDate = timeProvider.now().toLocalDate();
        }
        if (sessionController.shouldStartNewSessionFor(SessionClient.MAIN_ACTIVITY_SELECTED_DATE)) {
            onNewSession();
        }
    }

    @Override
    public void onNewSession() {
        selectedDate = timeProvider.now().toLocalDate();
        sessionController.onClientStartedNewSession(SessionClient.MAIN_ACTIVITY_SELECTED_DATE);
    }

    @Override
    public void onActivityDestroy() {
        sessionController.removeObserver(this);
    }
}
