package korablique.recipecalculator.ui.splash;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.mainscreen.MainScreenLoader;
import korablique.recipecalculator.ui.usergoal.UserParametersActivity;

public class SplashScreenActivity extends BaseActivity {
    @Inject
    MainScreenLoader mainScreenLoader;
    @Inject
    UserParametersWorker userParametersWorker;
    @Inject
    RxActivitySubscriptions rxSubscriptions;

    @Override
    protected Integer getLayoutId() {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rxSubscriptions.subscribe(userParametersWorker.requestCurrentUserParameters(), (params) -> {
            if (params.isPresent()) {
                rxSubscriptions.subscribe(mainScreenLoader.loadMainScreenActivity());
            } else {
                UserParametersActivity.start(this);
            }
        });

    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, SplashScreenActivity.class));
    }
}
