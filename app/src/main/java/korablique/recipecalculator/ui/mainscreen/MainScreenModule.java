package korablique.recipecalculator.ui.mainscreen;

import android.arch.lifecycle.Lifecycle;

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
    MainScreenModel provideModel(DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        return new MainScreenModelImpl(databaseWorker, historyWorker);
    }

    @ActivityScope
    @Provides
    MainScreenPresenter providePresenter(
            MainScreenView view, MainScreenModel model, MainScreenActivity activity, ActivityCallbacks callbacks) {
        return new MainScreenPresenterImpl(view, model, activity, callbacks);
    }

    @ActivityScope
    @Provides
    MainScreenView provideView(MainScreenActivity activity, ActivityCallbacks callbacks) {
        return new MainScreenViewImpl(activity, callbacks);
    }
}
