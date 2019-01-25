package korablique.recipecalculator.base;

import androidx.lifecycle.Lifecycle;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.dagger.ActivityScope;

@Module
public class BaseActivityModule {
    @ActivityScope
    @Provides
    ActivityCallbacks provideCallbacks(BaseActivity activity) {
        return activity.getActivityCallbacks();
    }

    @ActivityScope
    @Provides
    Lifecycle provideLifecycle(BaseActivity activity) {
        return activity.getLifecycle();
    }

}
