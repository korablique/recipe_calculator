package korablique.recipecalculator.ui.mainactivity.history.pages;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public abstract class HistoryPageFragmentModule {
    @FragmentScope
    @Provides
    public static BaseFragment provideBaseFragment(HistoryPageFragment fragment) {
        return fragment;
    }
}
