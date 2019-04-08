package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import korablique.recipecalculator.R;
import korablique.recipecalculator.dagger.InjectorHolder;

public abstract class BaseActivity extends AppCompatActivity {
    private final ActivityCallbacks activityCallbacks = new ActivityCallbacks();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        InjectorHolder.getInjector().inject(this);
        super.onCreate(savedInstanceState);
        activityCallbacks.dispatchActivityCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityCallbacks.dispatchActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityCallbacks.dispatchActivityPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityCallbacks.dispatchActivityDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        activityCallbacks.dispatchSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        activityCallbacks.dispatchRestoreInstanceState(savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        activityCallbacks.dispatchActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //понятия не имею, что это
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public ActivityCallbacks getActivityCallbacks() {
        return activityCallbacks;
    }
}
