package korablique.recipecalculator.ui.splash;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragmentModule;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.history.HistoryFragment;
import korablique.recipecalculator.ui.history.HistoryFragmentModule;
import korablique.recipecalculator.ui.mainscreen.MainActivity;
import korablique.recipecalculator.ui.mainscreen.MainActivityController;
import korablique.recipecalculator.ui.mainscreen.MainScreenFragment;
import korablique.recipecalculator.ui.mainscreen.MainScreenFragmentModule;
import korablique.recipecalculator.ui.mainscreen.SearchResultsFragment;
import korablique.recipecalculator.ui.mainscreen.SearchResultsFragmentModule;
import korablique.recipecalculator.ui.profile.NewMeasurementsDialog;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.ui.profile.ProfileFragmentModule;

@Module
public abstract class SplashScreenActivityModule {
    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(SplashScreenActivity activity) {
        return activity;
    }
}
