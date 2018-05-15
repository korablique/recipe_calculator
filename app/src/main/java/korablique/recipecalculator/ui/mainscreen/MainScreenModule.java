package korablique.recipecalculator.ui.mainscreen;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

@Module
public class MainScreenModule {
    @ActivityScope
    @Provides
    MainScreenModel provideModel(DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        return new MainScreenModelImpl(databaseWorker, historyWorker);
    }

    @ActivityScope
    @Provides
    MainScreenPresenter providePresenter(MainScreenView view, MainScreenModel model, MainScreenActivity activity) {
        return new MainScreenPresenterImpl(view, model, activity);
    }

    @ActivityScope
    @Provides
    MainScreenView provideView(MainScreenActivity activity) {
        return activity;
    }
}
