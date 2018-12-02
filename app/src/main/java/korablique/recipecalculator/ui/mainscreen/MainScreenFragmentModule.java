package korablique.recipecalculator.ui.mainscreen;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public class MainScreenFragmentModule {
    @FragmentScope
    @Provides
    static BaseFragment provideBaseFragment(MainScreenFragment fragment) {
        return fragment;
    }
}
