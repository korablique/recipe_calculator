package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.ui.usergoal.UserParametersActivity;

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
                    MainScreenFragment.show(context);
                    break;
                case R.id.menu_item_profile:
                    ProfileFragment.show(context);
                    break;
            }
            return true;
        });

        MainScreenFragment.show(context);
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
