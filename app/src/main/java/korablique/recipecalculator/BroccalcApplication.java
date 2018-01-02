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

public class BroccalcApplication extends Application implements HasActivityInjector {
    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerBroccalcApplicationComponent
                .builder()
                .context(this)
                .build()
                .inject(this);
        Fabric.with(this, new Crashlytics());
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }
}
