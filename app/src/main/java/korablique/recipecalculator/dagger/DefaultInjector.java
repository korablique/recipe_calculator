package korablique.recipecalculator.dagger;

import dagger.android.AndroidInjection;
import korablique.recipecalculator.BroccalcApplication;
import korablique.recipecalculator.base.BaseActivity;

class DefaultInjector implements Injector {
    @Override
    public void inject(BroccalcApplication application) {
        DaggerBroccalcApplicationComponent
                .builder()
                .context(application)
                .build()
                .inject(application);
    }

    @Override
    public void inject(BaseActivity activity) {
        AndroidInjection.inject(activity);
    }
}
