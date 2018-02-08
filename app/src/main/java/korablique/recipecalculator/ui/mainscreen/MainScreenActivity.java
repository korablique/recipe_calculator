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
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.PopularProductsUtils;

public class MainScreenActivity extends BaseActivity {
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    HistoryWorker historyWorker;
    AdapterParent adapterParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        RecyclerView recyclerView = findViewById(R.id.main_screen_recycler_view);
        adapterParent = new AdapterParent();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapterParent);

        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу

        List<Foodstuff> topFoodstuffsList = new ArrayList<>();
        List<Foodstuff> allFoodstuffsList = new ArrayList<>();

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
                        topFoodstuffsList.addAll(foodstuffs);
                        attemptToAddElementsToAdapters(topFoodstuffsList, allFoodstuffsList);
                    });
                });

        // получаем все продукты
        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(MainScreenActivity.this, batchSize, (foodstuffs) -> {
            allFoodstuffsList.addAll(foodstuffs);
            attemptToAddElementsToAdapters(topFoodstuffsList, allFoodstuffsList);
        });
    }

    private void attemptToAddElementsToAdapters(List<Foodstuff> topFoodstuffs, List<Foodstuff> allFoodstuffs) {
        if (!topFoodstuffs.isEmpty() && !allFoodstuffs.isEmpty()) {
            AdapterChild topAdapterChild = new FoodstuffsAdapterChild(
                    MainScreenActivity.this, R.layout.top_foodstuffs_header);
            adapterParent.addChild(topAdapterChild);
            topAdapterChild.addItems(topFoodstuffs);

            AdapterChild foodstuffAdapterChild = new FoodstuffsAdapterChild(
                    MainScreenActivity.this, R.layout.all_foodstuffs_header);
            adapterParent.addChild(foodstuffAdapterChild);
            foodstuffAdapterChild.addItems(allFoodstuffs);
        }
    }
}
