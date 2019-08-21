package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.dagger.InjectorHolder;

public abstract class BaseActivity extends AppCompatActivity {
    private final ActivityCallbacks activityCallbacks = new ActivityCallbacks();
    @Nullable
    private Bundle savedInstanceState;

    @Inject
    CurrentActivityProvider currentActivityProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        InjectorHolder.getInjector().inject(this);
        currentActivityProvider.watchNewActivity(this);
        super.onCreate(savedInstanceState);
        Integer layoutId = getLayoutId();
        if (layoutId != null) {
            setContentView(layoutId);
        }
        activityCallbacks.dispatchActivityCreate(savedInstanceState);
    }

    /**
     * @return R.layout.XXX of the activity, or null, if the activity doesn't have a layout.
     */
    @LayoutRes
    protected abstract Integer getLayoutId();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        activityCallbacks.dispatchActivityNewIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityCallbacks.dispatchActivityStart();
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
    protected void onStop() {
        super.onStop();
        activityCallbacks.dispatchActivityStop();
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
    public void onBackPressed() {
        boolean eventConsumed = activityCallbacks.dispatchActivityBackPressed();
        if (!eventConsumed) {
            super.onBackPressed();
        }
    }

    public ActivityCallbacks getActivityCallbacks() {
        return activityCallbacks;
    }

    @Nullable
    public Bundle getSavedInstanceState() {
        return savedInstanceState;
    }
}
