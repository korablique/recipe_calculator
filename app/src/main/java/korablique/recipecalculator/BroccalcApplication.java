package korablique.recipecalculator;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasBroadcastReceiverInjector;
import dagger.android.HasServiceInjector;
import io.fabric.sdk.android.Fabric;
import korablique.recipecalculator.dagger.Injector;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.outside.fcm.FCMManager;
import korablique.recipecalculator.outside.partners.direct.FoodstuffsCorrespondenceManager;
import korablique.recipecalculator.ui.notifications.FoodReminder;

public class BroccalcApplication extends Application
        implements HasActivityInjector, HasBroadcastReceiverInjector, HasServiceInjector {
    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;
    @Inject
    DispatchingAndroidInjector<BroadcastReceiver> broadcastReceiverDispatchingAndroidInjector;
    @Inject
    DispatchingAndroidInjector<Service> serviceDispatchingAndroidInjector;
    @Inject
    HistoryWorker historyWorker;
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    FoodReminder foodReminder;
    @Inject
    FCMManager fcmManager;
    @Inject
    FoodstuffsCorrespondenceManager foodstuffsCorrespondenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        if (!TestEnvironmentDetector.isInTests()) {
            InjectorHolder.getInjector().inject(this);
            historyWorker.initCache();
            userParametersWorker.initCache();
            // TODO: Вернуть шедулинг нотификаций после доработки логики их показа
//            foodReminder.scheduleNotification();
        }
    }

    /**
     * Don't call!
     * Use {@link Injector} instead.
     */
    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    public AndroidInjector<BroadcastReceiver> broadcastReceiverInjector() {
        return broadcastReceiverDispatchingAndroidInjector;
    }

    @Override
    public AndroidInjector<Service> serviceInjector() {
        return serviceDispatchingAndroidInjector;
    }
}
