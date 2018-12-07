package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.ui.profile.ProfileFragment;

public class MainActivityController extends ActivityCallbacks.Observer {
    private MainActivity context;

    public MainActivityController(MainActivity context, ActivityCallbacks activityCallbacks) {
        this.context = context;
        activityCallbacks.addObserver(this);
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        context.setContentView(R.layout.activity_main_screen);

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
