package korablique.recipecalculator.ui.mainscreen;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragmentModule;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.history.HistoryFragment;
import korablique.recipecalculator.ui.history.HistoryFragmentModule;
import korablique.recipecalculator.ui.profile.NewMeasurementsDialog;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.ui.profile.ProfileFragmentModule;

@Module
public abstract class MainScreenActivityModule {
    @ActivityScope
    @Provides
    static MainActivityController provideMainActivityController(
            MainActivity activity,
            ActivityCallbacks activityCallbacks,
            UserParametersWorker userParametersWorker,
            RxActivitySubscriptions subscriptions) {
        return new MainActivityController(activity, activityCallbacks, userParametersWorker, subscriptions);
    }

    @FragmentScope
    @ContributesAndroidInjector(modules = { BaseFragmentModule.class, SearchResultsFragmentModule.class })
    abstract SearchResultsFragment searchResultsFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = { BaseFragmentModule.class, MainScreenFragmentModule.class })
    abstract MainScreenFragment mainScreenFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = { BaseFragmentModule.class, ProfileFragmentModule.class })
    abstract ProfileFragment profileFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = { BaseFragmentModule.class, HistoryFragmentModule.class })
    abstract HistoryFragment historyFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector
    abstract NewMeasurementsDialog newMeasurementsDialogInjector();

    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(MainActivity activity) {
        return activity;
    }
}
