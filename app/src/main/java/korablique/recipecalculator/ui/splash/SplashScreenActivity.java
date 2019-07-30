package korablique.recipecalculator.ui.splash;

import android.os.Bundle;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.ui.mainscreen.MainScreenLoader;

public class SplashScreenActivity extends BaseActivity {
    @Inject
    MainScreenLoader mainScreenLoader;
    @Inject
    RxActivitySubscriptions rxSubscriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Disposable disposable = mainScreenLoader.loadMainScreenActivity();
        rxSubscriptions.storeDisposable(disposable);
    }
}
