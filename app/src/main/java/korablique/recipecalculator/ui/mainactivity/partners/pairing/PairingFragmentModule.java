package korablique.recipecalculator.ui.mainactivity.partners.pairing;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public abstract class PairingFragmentModule {
    @FragmentScope
    @Provides
    public static BaseFragment provideBaseFragment(PairingFragment fragment) {
        return fragment;
    }
}
