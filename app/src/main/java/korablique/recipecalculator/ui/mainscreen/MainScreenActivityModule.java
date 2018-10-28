package korablique.recipecalculator.ui.mainscreen;

import android.arch.lifecycle.Lifecycle;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

@Module
public abstract class MainScreenActivityModule {
    @ActivityScope
    @Provides
    static MainScreenActivityController provideController(
            MainScreenActivity activity,
            DatabaseWorker databaseWorker,
            HistoryWorker historyWorker,
            ActivityCallbacks callbacks,
            Lifecycle lifecycle) {
        return new MainScreenActivityController(activity, databaseWorker, historyWorker, callbacks, lifecycle);
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract SearchResultsFragment searchResultsFragmentInjector();

    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(MainScreenActivity activity) {
        return activity;
    }
}
