package korablique.recipecalculator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;

public abstract class MyActivity extends AppCompatActivity {
    private Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final DrawerBuilder drawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withSavedInstance(savedInstanceState)
                .withTranslucentStatusBar(true)
                .withActionBarDrawerToggle(true)
                .withSelectedItem(-1)
                .withOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
                    @Override
                    public boolean onNavigationClickListener(View clickedView) {
                        MyActivity.this.finish();
                        return true;
                    }
                });

        IDrawerItem itemPrimary1 = new PrimaryDrawerItem()
                .withName(R.string.drawer_item_home)
                .withSelectable(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Intent intent = new Intent(MyActivity.this, CalculatorActivity.class);
                        MyActivity.this.startActivity(intent);
                        return true;
                    }
                });
        IDrawerItem itemPrimary2 = new PrimaryDrawerItem()
                .withName(R.string.drawer_item_list)
                .withSelectable(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Intent intent = new Intent(MyActivity.this, ListOfFoodstuffsActivity.class);
                        MyActivity.this.startActivity(intent);
                        return false;
                    }
                });
        IDrawerItem itemSecondary = new SecondaryDrawerItem()
                .withName(R.string.drawer_item_settings)
                .withSelectable(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        //пока что никаких настроек нет
                        return false;
                    }
                });
        List<IDrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(itemPrimary1);
        drawerItems.add(itemPrimary2);
        drawerItems.add(itemSecondary);

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