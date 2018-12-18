package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;

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
    private MainActivity context;
    private UserParametersWorker userParametersWorker;
    private RxActivitySubscriptions subscriptions;

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

        BottomNavigationView bottomNavigationView = context.findViewById(R.id.navigation);
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
}
