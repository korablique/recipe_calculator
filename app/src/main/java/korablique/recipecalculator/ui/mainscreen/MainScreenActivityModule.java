package korablique.recipecalculator.ui.mainscreen;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragmentModule;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public abstract class MainScreenActivityModule {
    @ActivityScope
    @Provides
    static MainActivityController provideMainActivityController(
            MainActivity mainActivity,
            ActivityCallbacks activityCallbacks) {
        return new MainActivityController(mainActivity, activityCallbacks);
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract SearchResultsFragment searchResultsFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = { BaseFragmentModule.class, MainScreenFragmentModule.class })
    abstract MainScreenFragment mainScreenFragmentInjector();

    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(MainActivity activity) {
        return activity;
    }
}
