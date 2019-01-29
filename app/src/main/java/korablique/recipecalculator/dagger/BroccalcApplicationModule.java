package korablique.recipecalculator.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import korablique.recipecalculator.base.BaseActivityModule;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutorImpl;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseThreadExecutorImpl;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.mainscreen.MainActivity;
import korablique.recipecalculator.ui.history.HistoryModule;
import korablique.recipecalculator.ui.mainscreen.MainScreenActivityModule;
import korablique.recipecalculator.ui.notifications.FoodReminder;
import korablique.recipecalculator.ui.notifications.FoodReminderReceiver;
import korablique.recipecalculator.ui.usergoal.UserParametersActivity;
import korablique.recipecalculator.ui.usergoal.UserParametersActivityModule;

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
    public static FoodReminder provideFoodReminder(Context context) {
        return new FoodReminder(context);
    }

    @ActivityScope
    @ContributesAndroidInjector
    abstract ListOfFoodstuffsActivity contributeListOfFoodstuffsActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector(modules = { BaseActivityModule.class, HistoryModule.class })
    abstract HistoryActivity contributeHistoryActivityInjector();

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
}
