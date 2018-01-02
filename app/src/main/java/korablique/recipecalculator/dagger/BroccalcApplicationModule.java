package korablique.recipecalculator.dagger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.usergoal.UserGoalActivity;

@Module(includes = {AndroidSupportInjectionModule.class})
public abstract class BroccalcApplicationModule {
    @Provides
    @Singleton
    public static DatabaseWorker provideDatabaseWorker() {
        return new DatabaseWorker();
    }

    @ActivityScope
    @ContributesAndroidInjector
    abstract CalculatorActivity contributeCalculatorActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract ListOfFoodstuffsActivity contributeListOfFoodstuffsActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract HistoryActivity contributeHistoryActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract UserGoalActivity contributeUserGoalActivityInjector();
}
