package korablique.recipecalculator;

import android.app.Activity;
import android.app.Application;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import io.fabric.sdk.android.Fabric;
import korablique.recipecalculator.dagger.DaggerBroccalcApplicationComponent;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.notifications.FoodReminder;

public class BroccalcApplication extends Application implements HasActivityInjector {
    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;
    @Inject
    HistoryWorker historyWorker;
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    FoodReminder foodReminder;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerBroccalcApplicationComponent
                .builder()
                .context(this)
                .build()
                .inject(this);
        Fabric.with(this, new Crashlytics());

        historyWorker.initCache();
        userParametersWorker.initCache();

        foodReminder.scheduleNotification();
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }
}
