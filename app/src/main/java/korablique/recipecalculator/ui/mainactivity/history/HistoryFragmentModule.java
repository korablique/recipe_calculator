package korablique.recipecalculator.ui.mainactivity.history;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public class HistoryFragmentModule {
    @FragmentScope
    @Provides
    static BaseFragment provideBaseFragment(HistoryFragment fragment) {
        return fragment;
    }
}
