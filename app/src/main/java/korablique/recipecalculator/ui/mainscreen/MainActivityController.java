package korablique.recipecalculator.ui.mainscreen;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.history.HistoryFragment;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.ui.usergoal.UserParametersActivity;

import static korablique.recipecalculator.ui.history.HistoryFragment.EXTRA_FOODSTUFFS_LIST;
import static korablique.recipecalculator.ui.mainscreen.MainActivity.ACTION_ADD_FOODSTUFFS_TO_HISTORY;

public class MainActivityController extends ActivityCallbacks.Observer {
    private static final String BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID = "BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID";
    private MainActivity context;
    private UserParametersWorker userParametersWorker;
    private RxActivitySubscriptions subscriptions;
    private BottomNavigationView bottomNavigationView;

    public MainActivityController(
            MainActivity context,
            ActivityCallbacks activityCallbacks,
            UserParametersWorker userParametersWorker,
            RxActivitySubscriptions subscriptions) {
        this.context = context;
        activityCallbacks.addObserver(this);
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        context.setContentView(R.layout.activity_main_screen);

        Single<Optional<UserParameters>> userParamsSingle = userParametersWorker.requestCurrentUserParameters();
        subscriptions.subscribe(userParamsSingle, userParametersOptional -> {
            if (!userParametersOptional.isPresent()) {
                UserParametersActivity.start(context);
                context.finish();
            }
        });

        bottomNavigationView = context.findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_foodstuffs:
                    MainScreenFragment.show(context.getSupportFragmentManager());
                    break;
                case R.id.menu_item_history:
                    HistoryFragment.show(context.getSupportFragmentManager());
                    break;
                case R.id.menu_item_profile:
                    ProfileFragment.show(context.getSupportFragmentManager());
                    break;
            }
            return true;
        });

        Intent intent = context.getIntent();
        if (intent != null && ACTION_ADD_FOODSTUFFS_TO_HISTORY.equals(intent.getAction())) {
            List<WeightedFoodstuff> foodstuffs = intent.getParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST);
            if (foodstuffs == null) {
                throw new IllegalArgumentException("Need " + EXTRA_FOODSTUFFS_LIST);
            }
            // Меняем action на action-по-умолчанию, чтобы при пересоздании Активити
            // в неё повторно не были добавлены переданные сюда фудстафы.
            intent.setAction(Intent.ACTION_DEFAULT);
            HistoryFragment.show(context.getSupportFragmentManager(), foodstuffs);
        } else if (context.getSupportFragmentManager().findFragmentById(R.id.main_container) == null) {
            // если ни один фрагмент не показан (приложение только что запущено)
            MainScreenFragment.show(context.getSupportFragmentManager());
        }
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        super.onActivitySaveInstanceState(outState);
        outState.putInt(BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID, bottomNavigationView.getSelectedItemId());
    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {
        super.onActivityRestoreInstanceState(savedInstanceState);
        bottomNavigationView.setSelectedItemId(savedInstanceState.getInt(BOTTOM_NAVIGATION_VIEW_SELECTED_ITEM_ID));
    }
}
