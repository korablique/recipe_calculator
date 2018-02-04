package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.PopularProductsUtils;

public class MainScreenActivity extends BaseActivity {
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    HistoryWorker historyWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        RecyclerView recyclerView = findViewById(R.id.main_screen_recycler_view);
        AdapterParent adapter = new AdapterParent();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        TopAdapterChild topAdapterChild = new TopAdapterChild(MainScreenActivity.this);
        FoodstuffAdapterChild foodstuffAdapterChild = new FoodstuffAdapterChild(MainScreenActivity.this);
        adapter.addChild(topAdapterChild);
        adapter.addChild(foodstuffAdapterChild);

        // заполняем topAdapterChild
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
                        topAdapterChild.addItems(foodstuffs);
                    });
                });

        // заполняем foodstuffsAdapterChild
        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(MainScreenActivity.this, batchSize, (foodstuffs) -> {
            foodstuffAdapterChild.addItems(foodstuffs);
        });
    }
}
