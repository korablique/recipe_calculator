package korablique.recipecalculator.base;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides current displayed activity and allows subscribing to watch displayed activities changes.
 *
 * Behaviour of the class is based on activities lifecycle description from here:
 * https://developer.android.com/guide/components/activities/activity-lifecycle#coordinating-activities
 *
 * A short summary from the doc -
 * When activity A starts activity B, the order of lifecycle calls is the following:
 * 1. A.onPause()
 * 2. B.onCreate(), B.onStart(), B.onResume()
 * 3. A.onStop()
 */
@Singleton
public class CurrentActivityProvider {
    private final List<Observer> observers = new ArrayList<>();
    @Nullable
    private BaseActivity currentActivity;

    public interface Observer {
        void onCurrentActivityChanged(
                @Nullable BaseActivity oldActivity,
                @Nullable BaseActivity newActivity);
    }

    @Inject
    public CurrentActivityProvider() {
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    void watchNewActivity(BaseActivity activity) {
        ActivityWatcher activityWatcher = new ActivityWatcher(activity);
        activity.getActivityCallbacks().addObserver(activityWatcher);
    }

    private void onActivityStart(BaseActivity activity) {
        BaseActivity oldActivity = currentActivity;
        currentActivity = activity;
        for (Observer observer : observers) {
            observer.onCurrentActivityChanged(oldActivity, currentActivity);
        }
    }

    private void onActivityStop(BaseActivity activity) {
        if (activity == currentActivity) {
            BaseActivity oldActivity = currentActivity;
            currentActivity = null;
            for (Observer observer : observers) {
                observer.onCurrentActivityChanged(oldActivity, null);
            }
        }
    }

    /**
     * @return currently visible activity or null, if the app is not currently visible.
     */
    @Nullable
    public BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    private class ActivityWatcher implements ActivityCallbacks.Observer {
        BaseActivity activity;
        ActivityWatcher(BaseActivity activity) {
            this.activity = activity;
        }
        @Override
        public void onActivityStart() {
            CurrentActivityProvider.this.onActivityStart(activity);
        }
        @Override
        public void onActivityStop() {
            CurrentActivityProvider.this.onActivityStop(activity);
        }
    }
}
