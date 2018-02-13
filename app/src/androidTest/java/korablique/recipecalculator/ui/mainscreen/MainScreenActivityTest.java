package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsDbHelper;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.util.DbUtil;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainScreenActivityTest {
    private DatabaseWorker databaseWorker =
            new DatabaseWorker(new SyncMainThreadExecutor(), new InstantDatabaseThreadExecutor());
    private HistoryWorker historyWorker;
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Rule
    public ActivityTestRule<MainScreenActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(MainScreenActivity.class)
                    .withInjector((MainScreenActivity activity) -> {
                        activity.databaseWorker = databaseWorker;
                        activity.historyWorker = historyWorker;
                    })
                    .withManualStart()
                    .build();

    @Before
    public void setUp() throws InterruptedException {
        FoodstuffsDbHelper.deinitializeDatabase(context);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        historyWorker = new HistoryWorker(
                context, new SyncMainThreadExecutor(), new InstantDatabaseThreadExecutor());

        Foodstuff[] foodstuffs = new Foodstuff[7];
        foodstuffs[0] = new Foodstuff("apple", -1, 1, 1, 1, 1);
        foodstuffs[1] = new Foodstuff("pineapple", -1, 1, 1, 1, 1);
        foodstuffs[2] = new Foodstuff("plum", -1, 1, 1, 1, 1);
        foodstuffs[3] = new Foodstuff("water", -1, 1, 1, 1, 1);
        foodstuffs[4] = new Foodstuff("soup", -1, 1, 1, 1, 1);
        foodstuffs[5] = new Foodstuff("bread", -1, 1, 1, 1, 1);
        foodstuffs[6] = new Foodstuff("banana", -1, 1, 1, 1, 1);
        List<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(context, foodstuffs, (ids) -> {
            foodstuffsIds.addAll(ids);
        });

        NewHistoryEntry[] newEntries = new NewHistoryEntry[10];
        // 1 day: apple, bread, banana
        newEntries[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100, new Date(118, 0, 1));
        newEntries[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100, new Date(118, 0, 1));
        newEntries[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100, new Date(118, 0, 1));
        // 2 day: apple, water
        newEntries[3] = new NewHistoryEntry(foodstuffsIds.get(0), 100, new Date(118, 0, 2));
        newEntries[4] = new NewHistoryEntry(foodstuffsIds.get(3), 100, new Date(118, 0, 2));
        // 3 day: bread, soup
        newEntries[5] = new NewHistoryEntry(foodstuffsIds.get(5), 100, new Date(118, 0, 3));
        newEntries[6] = new NewHistoryEntry(foodstuffsIds.get(4), 100, new Date(118, 0, 3));
        // 4 day: apple, pineapple, water
        newEntries[7] = new NewHistoryEntry(foodstuffsIds.get(0), 100, new Date(118, 0, 1));
        newEntries[8] = new NewHistoryEntry(foodstuffsIds.get(1), 100, new Date(118, 0, 1));
        newEntries[9] = new NewHistoryEntry(foodstuffsIds.get(3), 100, new Date(118, 0, 1));
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);

        // каждый тест должен сам сделать launchActivity()
    }

    @Test
    public void topHeaderDoNotDisplayedIfHistoryIsEmpty() {
        DbUtil.clearTable(context, HISTORY_TABLE_NAME);
        mActivityRule.launchActivity(null);
        assertNotContains(mActivityRule.getActivity().getString(R.string.top_header));
    }

    @Test
    public void bothHeadersDisplayedIfHistoryIsNotEmpty() {
        mActivityRule.launchActivity(null);
        assertContains(mActivityRule.getActivity().getString(R.string.top_header));
        assertContains(mActivityRule.getActivity().getString(R.string.all_foodstuffs_header));
    }
}
