package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.Fragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.base.BaseActivity;

public class MainActivity extends BaseActivity implements HasSupportFragmentInjector {
    @Inject
    MainActivityController controller;
    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }
}
