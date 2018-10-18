package korablique.recipecalculator.dagger;

import android.support.v4.app.Fragment;

import dagger.android.AndroidInjection;
import dagger.android.support.AndroidSupportInjection;
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

    @Override
    public void inject(Fragment fragment) {
        AndroidSupportInjection.inject(fragment);
    }
}
