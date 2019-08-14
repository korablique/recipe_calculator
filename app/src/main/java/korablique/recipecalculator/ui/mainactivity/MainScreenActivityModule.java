package korablique.recipecalculator.ui.mainactivity;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragmentModule;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragmentModule;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenFragmentModule;
import korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragmentModule;
import korablique.recipecalculator.ui.mainactivity.profile.NewMeasurementsDialog;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileFragment;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileFragmentModule;

@Module
public abstract class MainScreenActivityModule {
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
