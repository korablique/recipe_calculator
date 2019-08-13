package korablique.recipecalculator.ui.mainactivity.mainscreen;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public class SearchResultsFragmentModule {
    @FragmentScope
    @Provides
    static BaseFragment provideBaseFragment(SearchResultsFragment fragment) {
        return fragment;
    }
}
