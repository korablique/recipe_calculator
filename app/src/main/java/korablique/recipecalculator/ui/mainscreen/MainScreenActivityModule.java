package korablique.recipecalculator.ui.mainscreen;

import android.arch.lifecycle.Lifecycle;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.FoodstuffsList;

@Module
public abstract class MainScreenActivityModule {
    @ActivityScope
    @Provides
    static ActivityCallbacks provideCallbacks(MainScreenActivity activity) {
        return activity.getActivityCallbacks();
    }

    @ActivityScope
    @Provides
    static Lifecycle provideLifecycle(MainScreenActivity activity) {
        return activity.getLifecycle();
    }

    @ActivityScope
    @Provides
    static MainScreenActivityController provideController(
            MainScreenActivity activity,
            DatabaseWorker databaseWorker,
            HistoryWorker historyWorker,
            FoodstuffsList foodstuffsList,
            ActivityCallbacks callbacks,
            Lifecycle lifecycle) {
        return new MainScreenActivityController(activity, databaseWorker, historyWorker, foodstuffsList, callbacks, lifecycle);
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract SearchResultsFragment searchResultsFragmentInjector();
}
