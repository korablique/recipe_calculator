package korablique.recipecalculator.ui.mainactivity.partners;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public abstract class PartnersListFragmentModule {
    @FragmentScope
    @Provides
    public static BaseFragment provideBaseFragment(PartnersListFragment fragment) {
        return fragment;
    }
}
