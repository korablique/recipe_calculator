package korablique.recipecalculator.ui.mainscreen;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

public class MainScreenActivity extends BaseActivity {
    public static final String CLICKED_FOODSTUFF = "CLICKED_FOODSTUFF";
    @Inject
    MainScreenPresenter presenter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        MainScreenView view = new MainScreenViewImpl(this);
        MainScreenModel model = new MainScreenModelImpl(databaseWorker, historyWorker);
        presenter = new MainScreenPresenterImpl(view, model, this);
        presenter.onActivityCreate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        presenter.onActivitySaveState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        presenter.onActivityRestoreState(savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onActivityResume();
    }
}
