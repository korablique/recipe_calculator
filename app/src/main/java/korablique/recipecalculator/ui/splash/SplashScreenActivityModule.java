package korablique.recipecalculator.ui.splash;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;

@Module
public abstract class SplashScreenActivityModule {
    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(SplashScreenActivity activity) {
        return activity;
    }
}
