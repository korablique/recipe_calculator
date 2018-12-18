package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.R;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.mainscreen.MainActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private final ActivityCallbacks activityCallbacks = new ActivityCallbacks();
    private Drawer drawer;

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

        final DrawerBuilder drawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withHeader(R.layout.drawer_header)
                .withSavedInstance(savedInstanceState)
                .withTranslucentStatusBar(true)
                .withActionBarDrawerToggle(false)
                .withToolbar(toolbar)
                .withSelectedItem(-1)
                .withOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
                    @Override
                    public boolean onNavigationClickListener(View clickedView) {
                        BaseActivity.this.finish();
                        return true;
                    }
                });

        IDrawerItem itemPrimary1 = new PrimaryDrawerItem()
                .withName(R.string.calculator)
                .withSelectable(false)
                .withIcon(R.drawable.calculator_icon)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Intent intent = new Intent(BaseActivity.this, CalculatorActivity.class);
                        BaseActivity.this.startActivity(intent);
                        return true;
                    }
                });
        IDrawerItem itemPrimary2 = new PrimaryDrawerItem()
                .withName(R.string.list_of_foodstuffs)
                .withSelectable(false)
                .withIcon(R.drawable.list_of_foodstuffs_icon)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Intent intent = new Intent(BaseActivity.this, ListOfFoodstuffsActivity.class);
                        BaseActivity.this.startActivity(intent);
                        return false;
                    }
                });
        IDrawerItem itemPrimary3 = new PrimaryDrawerItem()
                .withName(R.string.history)
                .withSelectable(false)
                .withIcon(R.drawable.ic_history_24dp)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Intent intent = new Intent(BaseActivity.this, HistoryActivity.class);
                        BaseActivity.this.startActivity(intent);
                        return true;
                    }
                });

        IDrawerItem itemPrimary4 = null;
        if (BuildConfig.DEBUG) {
            itemPrimary4 = new PrimaryDrawerItem()
                    .withName("MainActivity")
                    .withSelectable(false)
                    .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                        @Override
                        public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                            Intent intent = new Intent(BaseActivity.this, MainActivity.class);
                            BaseActivity.this.startActivity(intent);
                            return true;
                        }
                    });
        }
        List<IDrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(itemPrimary1);
        drawerItems.add(itemPrimary2);
        drawerItems.add(itemPrimary3);
        if (itemPrimary4 != null) {
            drawerItems.add(itemPrimary4);
        }

        drawerBuilder.withDrawerItems(drawerItems);

        drawer = drawerBuilder.build();
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
        outState = drawer.saveInstanceState(outState);
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

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    public ActivityCallbacks getActivityCallbacks() {
        return activityCallbacks;
    }
}
