package korablique.recipecalculator.ui.mainscreen;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

@Module
public class MainScreenModule {
    @ActivityScope
    @Provides
    ActivityCallbacks provideCallbacks(MainScreenActivity activity) {
        return activity.getActivityCallbacks();
    }

    @ActivityScope
    @Provides
    MainScreenActivityController providePresenter(
            MainScreenActivity activity,
            DatabaseWorker databaseWorker,
            HistoryWorker historyWorker,
            ActivityCallbacks callbacks) {
        return new MainScreenActivityController(activity, databaseWorker, historyWorker, callbacks);
    }
}
