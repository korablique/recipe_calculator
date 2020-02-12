package korablique.recipecalculator.ui.mainactivity.mainscreen;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.BaseFragmentModule;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public abstract class MainScreenFragmentModule {
    @FragmentScope
    @Provides
    static BaseFragment provideBaseFragment(MainScreenFragment fragment) {
        return fragment;
    }

    @MainScreenFragmentSubScope
    @ContributesAndroidInjector
    abstract SearchResultsFragment searchResultsFragmentInjector();
}
