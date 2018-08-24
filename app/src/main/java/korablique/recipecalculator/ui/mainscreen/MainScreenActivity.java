package korablique.recipecalculator.ui.mainscreen;

import android.content.Intent;

import javax.inject.Inject;

import korablique.recipecalculator.base.BaseActivity;

public class MainScreenActivity extends BaseActivity {
    @Inject
    MainScreenPresenter presenter;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
