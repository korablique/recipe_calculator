package korablique.recipecalculator.ui.mainscreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import korablique.recipecalculator.IntentConstants;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsDbHelper;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.util.DbUtil;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static android.support.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.BundleMatchers.hasValue;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainScreenActivityTest {
    private SyncMainThreadExecutor mainThreadExecutor = new SyncMainThreadExecutor();
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Rule
    public ActivityTestRule<MainScreenActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(MainScreenActivity.class)
                .withManualStart()
                .withSingletones(() -> {
                    databaseWorker =
                            new DatabaseWorker(mainThreadExecutor, new InstantDatabaseThreadExecutor());
                    historyWorker = new HistoryWorker(
                            context, mainThreadExecutor, new InstantDatabaseThreadExecutor());
                    return Arrays.asList(databaseWorker, historyWorker);
                })
                .withActivityScoped((injectionTarget) -> {
                    if (!(injectionTarget instanceof MainScreenActivity)) {
                        return Collections.emptyList();
                    }
                    MainScreenActivity activity = (MainScreenActivity) injectionTarget;
                    ActivityCallbacks activityCallbacks = activity.getActivityCallbacks();
                    MainScreenActivityController controller = new MainScreenActivityController(
                            activity, databaseWorker, historyWorker, activityCallbacks);
                    return Collections.singletonList(controller);
                })
                .build();

    @Before
    public void setUp() {
        FoodstuffsDbHelper.deinitializeDatabase(context);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        Foodstuff[] foodstuffs = new Foodstuff[7];
        foodstuffs[0] = Foodstuff.withName("apple").withNutrition(1, 1, 1, 1);
        foodstuffs[1] = Foodstuff.withName("pineapple").withNutrition(1, 1, 1, 1);
        foodstuffs[2] = Foodstuff.withName("plum").withNutrition(1, 1, 1, 1);
        foodstuffs[3] = Foodstuff.withName("water").withNutrition(1, 1, 1, 1);
        foodstuffs[4] = Foodstuff.withName("soup").withNutrition(1, 1, 1, 1);
        foodstuffs[5] = Foodstuff.withName("bread").withNutrition(1, 1, 1, 1);
        foodstuffs[6] = Foodstuff.withName("banana").withNutrition(1, 1, 1, 1);
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

    @After
    public void tearDown() {
        databaseWorker = null;
        historyWorker = null;
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

    @Test
    public void topIsCorrect() {
        mActivityRule.launchActivity(null);

        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        // Рассчитываем, что в топе будет как минимум 3 фудстафа - как бы константа количества
        // фудстафов в топе не менялась, менее 3 её делать не стоит.
        for (int index = 0; index < 2; ++index) {
            Foodstuff foodstuff = topFoodstuffs.get(index);
            Foodstuff foodstuffBelow = topFoodstuffs.get(index + 1);

            // NOTE: оба Фудстафа мы фильтруем проверкой "completely above all_foodstuffs_header"
            // Это нужно из-за того, что одни и те же Фудстафы могут присутствовать в двух списках -
            // в топе Фудстафов и в списке всех Фудстафов. Когда Эспрессо просят найти вьюшку,
            // и под параметры поиска подпадают сразу несколько вьюшек, Эспрессо моментально паникует
            // и роняет тест.
            // В данном тесте мы проверяем только топ, весь список нам не нужен, поэтому явно говорим
            // Эспрессо, что нас интересуют только вьюшки выше заголовка all_foodstuffs_header.

            Matcher<View> foodstuffMatcher = allOf(
                    withText(foodstuff.getName()),
                    matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))));

            Matcher<View> foodstuffBelowMatcher = allOf(
                    withText(foodstuffBelow.getName()),
                    matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                    matches(isCompletelyBelow(foodstuffMatcher)));

            onView(foodstuffMatcher).check(matches(isDisplayed()));
            onView(foodstuffBelowMatcher).check(matches(isDisplayed()));
        }
    }

    @Test
    public void startsBucketListActivityWithSelectedFoodstuffs() {
        mActivityRule.launchActivity(null);

        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        ArrayList<Foodstuff> clickedFoodstuffs = new ArrayList<>();
        clickedFoodstuffs.add(topFoodstuffs.get(0));
        clickedFoodstuffs.add(topFoodstuffs.get(1));
        clickedFoodstuffs.add(topFoodstuffs.get(2));

        // Кликаем на первый, второй и третий продукт в топе.
        onView(allOf(
                withText(clickedFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.add_foodstuff_button)).perform(click());

        onView(allOf(
                withText(clickedFoodstuffs.get(1).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.add_foodstuff_button)).perform(click());

        onView(allOf(
                withText(clickedFoodstuffs.get(2).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.add_foodstuff_button)).perform(click());

        // Кликаем на корзинку в снэкбаре
        onView(withId(R.id.basket)).perform(click());

        List<WeightedFoodstuff> clickedWeightedFoodstuffs = new ArrayList<>();
        for (Foodstuff foodstuff : clickedFoodstuffs) {
            clickedWeightedFoodstuffs.add(foodstuff.withWeight(123));
        }

        // Проверяем, что была попытка стартовать активити по интенту от BucketListActivity,
        // также что этот интент содержит информацию о кликнутых продуктах.
        Intent expectedIntent =
                BucketListActivity.createStartIntentFor(clickedWeightedFoodstuffs, mActivityRule.getActivity());
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasComponent(expectedIntent.getComponent()),
                hasExtras(hasValue(clickedWeightedFoodstuffs))));
    }

    @Test
    public void editedFoodstuffReplacesInBothTopAndAllFoodstuffs() {
        mActivityRule.launchActivity(null);
        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        long id = topFoodstuffs.get(0).getId();
        Foodstuff edited = Foodstuff.withId(id).withName(topFoodstuffs.get(0).getName() + "1").withNutrition(1, 2, 3, 4);

        Intent data = EditFoodstuffActivity.createEditingResultIntent(edited);

        // onActivityResult нельзя вызвать на потоке тестов,
        // поэтому запускаем на главном потоке блокирующую операцию
        mainThreadExecutor.execute(() -> {
            mActivityRule.getActivity().onActivityResult(IntentConstants.EDIT_FOODSTUFF_REQUEST, Activity.RESULT_OK, data);
        });

        onView(withId(R.id.button_close)).perform(click());

        Matcher<View> topMatcher = allOf(
                withText(edited.getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).check(matches(isDisplayed()));

        Matcher<View> allFoodstuffsMatcher = allOf(
                withText(edited.getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))));
        onView(allFoodstuffsMatcher).check(matches(isDisplayed()));
    }

    private List<Foodstuff> extractFoodstuffsTopFromDB() {
        List<Long> ids = new ArrayList<>();
        historyWorker.requestFoodstuffsIdsFromHistoryForPeriod(0, Long.MAX_VALUE, (list) -> {
            ids.addAll(list);
        });

        List<PopularProductsUtils.FoodstuffFrequency> topIdsFrequencies = PopularProductsUtils.getTop(ids);
        List<Long> topIds = new ArrayList<>();
        for (PopularProductsUtils.FoodstuffFrequency frequency : topIdsFrequencies) {
            topIds.add(frequency.getFoodstuffId());
        }

        List<Foodstuff> topFoodstuffs = new ArrayList<>();
        databaseWorker.requestFoodstuffsByIds(context, topIds, (foodstuffs) -> {
            topFoodstuffs.addAll(foodstuffs);
        });
        return topFoodstuffs;
    }
}
