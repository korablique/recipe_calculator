package korablique.recipecalculator.base;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.dagger.FragmentScope;

@Module
public class BaseFragmentModule {
    @FragmentScope
    @Provides
    static FragmentCallbacks provideCallbacks(BaseFragment fragment) {
        return fragment.getFragmentCallbacks();
    }
}
