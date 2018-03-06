package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.ui.NewCard;
import korablique.recipecalculator.ui.history.HistoryActivity;

public class MainScreenActivity extends BaseActivity {
    public static final String CLICKED_FOODSTUFF = "CLICKED_FOODSTUFF";
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    HistoryWorker historyWorker;
    AdapterParent adapterParent;
    private FoodstuffsAdapterChild foodstuffAdapterChild;
    private List<Foodstuff> top;
    private List<Foodstuff> all;
    private FoodstuffsAdapterChild.ClickObserver clickObserver = (foodstuff, displayedPosition) -> {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLICKED_FOODSTUFF, foodstuff);
        CardDialog dialog = new CardDialog();
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "show card");
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener((item) -> {
            switch (item.getItemId()) {
                case R.id.menu_item_history:
                    Intent historyIntent = new Intent(MainScreenActivity.this, HistoryActivity.class);
                    startActivity(historyIntent);
                    break;
            }
            return false;
        });

        RecyclerView recyclerView = findViewById(R.id.main_screen_recycler_view);
        adapterParent = new AdapterParent();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapterParent);

        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу

        // получаем топ продуктов
        List<Long> foodstuffsIds = new ArrayList<>(); // это айдишники всех продуктов за период
        historyWorker.requestFoodstuffsIdsFromHistoryForPeriod(
                0,
                Long.MAX_VALUE,
                (ids) -> {
                    foodstuffsIds.addAll(ids);
                    List<PopularProductsUtils.FoodstuffFrequency> topList = PopularProductsUtils.getTop(foodstuffsIds); // это топ из них
                    List<Long> topFoodstuffIds = new ArrayList<>(); // это айдишники топа
                    for (int index = 0; index < topList.size() && index < 5; ++index) {
                        topFoodstuffIds.add(topList.get(index).getFoodstuffId());
                    }
                    databaseWorker.requestFoodstuffsByIds(MainScreenActivity.this, topFoodstuffIds, (foodstuffs) -> {
                        top = new ArrayList<>();
                        top.addAll(foodstuffs);
                        attemptToAddElementsToAdapters();
                    });
                });

        // получаем все продукты
        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(MainScreenActivity.this, batchSize, (foodstuffs) -> {
            if (all == null) {
                all = new ArrayList<>();
            }
            all.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(FragmentManager fm, Fragment f, Context context) {
                super.onFragmentAttached(fm, f, context);
                if (f instanceof CardDialog) {
                    CardDialog cardDialog = (CardDialog) f;
                    cardDialog.setOnAddFoodstuffButtonClickListener(foodstuff -> {
                        cardDialog.dismiss();
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "added foodstuff: " + foodstuff.getName(),
                                Snackbar.LENGTH_SHORT)
                                .show();
                    });
                } else {
                    throw new IllegalStateException("Unexpected type of fragment");
                }
            }
        }, true);
    }

    private void attemptToAddElementsToAdapters() {
        if (top == null || all == null) {
            return;
        }
        if (!top.isEmpty()) {
            FoodstuffsAdapterChild topAdapterChild = new FoodstuffsAdapterChild(
                    MainScreenActivity.this, clickObserver, R.layout.top_foodstuffs_header);
            adapterParent.addChild(topAdapterChild);
            topAdapterChild.addItems(top);
        }
        // если топ пустой, то топ-адаптер не нужно создавать, чтобы не было заголовка

        if (foodstuffAdapterChild == null) {
            foodstuffAdapterChild = new FoodstuffsAdapterChild(
                    MainScreenActivity.this, clickObserver, R.layout.all_foodstuffs_header);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(all);
    }
}
