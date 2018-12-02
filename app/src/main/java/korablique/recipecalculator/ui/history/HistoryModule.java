package korablique.recipecalculator.ui.history;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;

@Module
public class HistoryModule {
    @ActivityScope
    @Provides
    BaseActivity provideBaseActivity(HistoryActivity historyActivity) {
        return historyActivity;
    }
}
