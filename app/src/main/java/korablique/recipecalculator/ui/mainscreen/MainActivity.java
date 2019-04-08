package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.ui.bucketlist.BucketListActivity.EXTRA_FOODSTUFFS_LIST;

public class MainActivity extends BaseActivity implements HasSupportFragmentInjector {
    public static final String ACTION_ADD_FOODSTUFFS = "ACTION_ADD_FOODSTUFFS";
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

    public static void startWithHistory(Context context, List<WeightedFoodstuff> historyList) {
        context.startActivity(createStartIntent(context, historyList));
    }

    public static Intent createStartIntent(Context context, List<WeightedFoodstuff> historyList) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(historyList));
        intent.setAction(ACTION_ADD_FOODSTUFFS);
        return intent;
    }
}
