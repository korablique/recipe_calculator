package korablique.recipecalculator.ui.mainactivity.partners;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public class PartnersListFragmentModule {
    @FragmentScope
    @Provides
    public BaseFragment provideBaseFragment(PartnersListFragment fragment) {
        return fragment;
    }
}
