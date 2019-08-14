package korablique.recipecalculator.ui.mainactivity.profile;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public class ProfileFragmentModule {
    @FragmentScope
    @Provides
    static BaseFragment provideBaseFragment(ProfileFragment fragment) {
        return fragment;
    }
}
