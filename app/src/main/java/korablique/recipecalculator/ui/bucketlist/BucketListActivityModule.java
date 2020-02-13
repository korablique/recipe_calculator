package korablique.recipecalculator.ui.bucketlist;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.mainactivity.MainActivity;

@Module
public abstract class BucketListActivityModule {
    @FragmentScope
    @ContributesAndroidInjector
    abstract CardDialog newCardDialogInjector();

    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(MainActivity activity) {
        return activity;
    }
}
