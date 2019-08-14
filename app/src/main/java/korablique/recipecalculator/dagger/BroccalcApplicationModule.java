package korablique.recipecalculator.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import korablique.recipecalculator.base.BaseActivityModule;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.base.TimeProviderImpl;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutorImpl;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutorImpl;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseThreadExecutorImpl;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.mainactivity.MainActivity;
import korablique.recipecalculator.ui.mainactivity.MainScreenActivityModule;
import korablique.recipecalculator.ui.notifications.FoodReminder;
import korablique.recipecalculator.ui.notifications.FoodReminderReceiver;
import korablique.recipecalculator.ui.splash.SplashScreenActivity;
import korablique.recipecalculator.ui.splash.SplashScreenActivityModule;
import korablique.recipecalculator.ui.userparameters.UserParametersActivity;
import korablique.recipecalculator.ui.userparameters.UserParametersActivityModule;

@Module(includes = {AndroidSupportInjectionModule.class})
public abstract class BroccalcApplicationModule {
    @Provides
    @Singleton
    public static MainThreadExecutor provideMainThreadExecutor() {
        return new MainThreadExecutorImpl();
    }

    @Provides
    @Singleton
    public static DatabaseThreadExecutor provideDatabaseThreadExecutor() {
        return new DatabaseThreadExecutorImpl();
    }

    @Provides
    @Singleton static ComputationThreadsExecutor provideComputationThreadsExecutor() {
        return new ComputationThreadsExecutorImpl();
    }

    @Provides
    @Singleton
    public static DatabaseWorker provideDatabaseWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        return new DatabaseWorker(databaseHolder, mainThreadExecutor, databaseThreadExecutor);
    }

    @Provides
    @Singleton
    public static UserParametersWorker provideUserParametersWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        return new UserParametersWorker(databaseHolder, mainThreadExecutor, databaseThreadExecutor);
    }

    @Provides
    @Singleton
    public static FoodReminder provideFoodReminder(Context context, TimeProvider timeProvider) {
        return new FoodReminder(context, timeProvider);
    }

    @Provides
    @Singleton
    public static TimeProvider provideTimeProvider() {
        return new TimeProviderImpl();
    }

    @ActivityScope
    @ContributesAndroidInjector(modules = { BaseActivityModule.class, MainScreenActivityModule.class })
    abstract MainActivity contributeMainScreenActivityInjector();

    @BroadcastReceiverScope
    @ContributesAndroidInjector
    abstract FoodReminderReceiver contributeFoodReminderReceiver();

    @ActivityScope
    @ContributesAndroidInjector
    abstract BucketListActivity contributeBucketListActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract EditFoodstuffActivity contributeEditFoodstuffActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector(modules = { BaseActivityModule.class, UserParametersActivityModule.class })
    abstract UserParametersActivity contributeUserParametersActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector(modules = { BaseActivityModule.class, SplashScreenActivityModule.class })
    abstract SplashScreenActivity contributeSplashScreenActivityInjector();
}
