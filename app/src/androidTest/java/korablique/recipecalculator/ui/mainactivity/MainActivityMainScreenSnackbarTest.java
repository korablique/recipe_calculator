package korablique.recipecalculator.ui.mainactivity;

import android.app.Instrumentation;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityMainScreenSnackbarTest extends MainActivityTestsBase {
    @Test
    public void snackbarUpdatesAfterChangingBucketList() {
        // добавляем в bucket list продукты, запускаем активити, в снекбаре должно быть 3 фудстаффа
        WeightedFoodstuff wf0 = foodstuffs[0].withWeight(100);
        WeightedFoodstuff wf1 = foodstuffs[1].withWeight(100);
        WeightedFoodstuff wf2 = foodstuffs[2].withWeight(100);

        mainThreadExecutor.execute(() -> {
            bucketList.add(wf0);
            bucketList.add(wf1);
            bucketList.add(wf2);
        });

        mActivityRule.launchActivity(null);
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("3")));

        // убираем один продукт, перезапускаем активити, в снекбаре должно быть 2 фудстаффа
        mainThreadExecutor.execute(() -> {
            bucketList.remove(foodstuffs[0].withWeight(100));
        });

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        instrumentation.waitForIdleSync();
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(isDisplayed()));
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("2")));
        Assert.assertTrue(bucketList.getList().contains(wf1));
        Assert.assertTrue(bucketList.getList().contains(wf2));

        // убираем все продукты, перезапускаем активити, снекбара быть не должно
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        instrumentation.waitForIdleSync();
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(not(isDisplayed())));
    }

    @Test
    public void swipedOutSnackbar_cleansBuckerList() throws InterruptedException {
        // добавляем в bucket list продукты, запускаем активити
        WeightedFoodstuff wf0 = foodstuffs[0].withWeight(100);
        WeightedFoodstuff wf1 = foodstuffs[1].withWeight(100);

        mainThreadExecutor.execute(() -> {
            bucketList.add(wf0);
            bucketList.add(wf1);
            assertEquals(2, bucketList.getList().size());
        });

        mActivityRule.launchActivity(null);

        // "Высвайпываем" снекбар
        onView(withId(R.id.snackbar)).perform(swipeRight());
        Thread.sleep(500); // Ждем 0.5с, потому что SwipeDismissBehaviour криво уведомляет о высвайпывании

        // Убеждаемся, что в бакетлисте пусто и снекбар не показан
        onView(withId(R.id.snackbar)).check(matches(not(isDisplayed())));
        mainThreadExecutor.execute(() -> {
            assertTrue(bucketList.getList().isEmpty());
        });
    }

    @Test
    public void swipedOutSnackbar_canBeCanceled() throws InterruptedException {
        // добавляем в bucket list продукты, запускаем активити
        WeightedFoodstuff wf0 = foodstuffs[0].withWeight(100);
        WeightedFoodstuff wf1 = foodstuffs[1].withWeight(100);

        mainThreadExecutor.execute(() -> {
            bucketList.add(wf0);
            bucketList.add(wf1);
            assertEquals(2, bucketList.getList().size());
        });

        mActivityRule.launchActivity(null);

        // "Высвайпываем" снекбар
        onView(withId(R.id.snackbar)).perform(swipeRight());
        Thread.sleep(500); // Ждем 0.5с, потому что SwipeDismissBehaviour криво уведомляет о высвайпывании

        // Тут же отменяем "высвайпывание"
        onView(withText(R.string.undo)).perform(click());

        // Убеждаемся, что в бакетлисте по-прежнему есть продукты и снекбар показан
        onView(withId(R.id.snackbar)).check(matches(isDisplayed()));
        mainThreadExecutor.execute(() -> {
            assertEquals(2, bucketList.getList().size());
        });
    }
}
