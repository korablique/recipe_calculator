package korablique.recipecalculator.ui;

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

import korablique.recipecalculator.R;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                .withActionBarDrawerToggle(true)
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
        List<IDrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(itemPrimary1);
        drawerItems.add(itemPrimary2);
        drawerItems.add(itemPrimary3);

        drawerBuilder.withDrawerItems(drawerItems);

        drawer = drawerBuilder.build();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState = drawer.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
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
}
