package korablique.recipecalculator.ui.userparameters;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;

@Module
public class UserParametersActivityModule {
    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(UserParametersActivity activity) {
        return activity;
    }
}
