package korablique.recipecalculator.ui.mainscreen;

import android.support.v4.app.Fragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.model.FoodstuffsList;

public class MainScreenActivity extends BaseActivity implements HasSupportFragmentInjector {
    @Inject
    MainScreenActivityController controller;
    @Inject
    FoodstuffsList foodstuffsList;
    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }
}
