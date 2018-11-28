package korablique.recipecalculator.ui.usergoal;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;

@Module
public class UserGoalModule {
    @ActivityScope
    @Provides
    BaseActivity provideBaseActivity(UserGoalActivity activity) {
        return activity;
    }
}
