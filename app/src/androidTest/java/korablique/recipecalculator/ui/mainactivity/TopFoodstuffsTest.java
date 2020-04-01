package korablique.recipecalculator.ui.mainactivity;

import android.view.View;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenController;
import korablique.recipecalculator.util.EspressoUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TopFoodstuffsTest extends MainActivityTestsBase {
    @Test
    public void topHeaderDoNotDisplayedIfHistoryIsEmpty() {
        historyWorker.requestAllHistoryFromDb(historyEntries -> {
            for (HistoryEntry entry : historyEntries) {
                historyWorker.deleteEntryFromHistory(entry);
            }
        });
        mActivityRule.launchActivity(null);
        assertNotContains(mActivityRule.getActivity().getString(R.string.top_header));
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
    public void moreThanMonthOldFoodstuffs_dontGoToTop() {
        Foodstuff newFoodstuff1 = Foodstuff.withName("newfoodstuff1").withNutrition(1, 2, 3, 4);
        Foodstuff newFoodstuff2 = Foodstuff.withName("newfoodstuff2").withNutrition(1, 2, 3, 4);
        long[] ids = new long[2];
        foodstuffsList.saveFoodstuff(newFoodstuff1, new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                ids[0] = addedFoodstuff.getId();
            }
            @Override public void onDuplication() {}
        });
        foodstuffsList.saveFoodstuff(newFoodstuff2, new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                ids[1] = addedFoodstuff.getId();
            }
            @Override public void onDuplication() {}
        });

        // newFoodstuff1 на сегодня
        for (int index = 0; index < 5; ++index) {
            addFoodstuffToDate(timeProvider.now(), ids[0]);
        }
        // newFoodstuff2 на 2 месяца в прошлом
        for (int index = 0; index < 5; ++index) {
            addFoodstuffToDate(timeProvider.now().minusMonths(2), ids[1]);
        }

        // Старт
        mActivityRule.launchActivity(null);

        // newFoodstuff1 должен быть в топе
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_main_screen)),
                withText(newFoodstuff1.getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .check(matches(isDisplayed()));
        // newFoodstuff2 НЕ должен быть в топе
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_main_screen)),
                withText(newFoodstuff2.getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .check(doesNotExist());
    }

    @Test
    public void addedToHistoryFoodstuffs_appearInTop_afterActivityRestart() {
        clearAllData();
        mActivityRule.launchActivity(null);
        onView(withText(R.string.top_header)).check(doesNotExist());

        // Сохраним продукт в Историю
        AtomicReference<Foodstuff> newFoodstuff = new AtomicReference<>(
                Foodstuff.withName("new foodstuff").withNutrition(1, 2, 3, 4));
        foodstuffsList.saveFoodstuff(newFoodstuff.get(), new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                newFoodstuff.set(addedFoodstuff);
            }
            @Override public void onDuplication() {}
        });
        historyWorker.saveFoodstuffToHistory(timeProvider.now().toDate(), newFoodstuff.get().getId(), 123);

        // Проверим, что топа (и продукта в нём) пока нет.
        onView(withText(R.string.top_header)).check(doesNotExist());
        onView(allOf(
                withText(newFoodstuff.get().getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(doesNotExist());

        // Рестарт
        mActivityRule.finishActivity();
        mActivityRule.launchActivity(null);

        // Проверим, что появился топ и продукт в нём.
        onView(withText(R.string.top_header)).check(matches(isDisplayed()));
        onView(allOf(
                withText(newFoodstuff.get().getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(matches(isDisplayed()));
    }

    @Test
    public void addedToHistoryFoodstuffs_appearInTop_afterFragmentChange() {
        clearAllData();
        mActivityRule.launchActivity(null);
        onView(withText(R.string.top_header)).check(doesNotExist());

        // Сохраним продукт в Историю
        AtomicReference<Foodstuff> newFoodstuff = new AtomicReference<>(
                Foodstuff.withName("new foodstuff").withNutrition(1, 2, 3, 4));
        foodstuffsList.saveFoodstuff(newFoodstuff.get(), new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                newFoodstuff.set(addedFoodstuff);
            }
            @Override public void onDuplication() {}
        });
        historyWorker.saveFoodstuffToHistory(timeProvider.now().toDate(), newFoodstuff.get().getId(), 123);

        // Проверим, что топа (и продукта в нём) пока нет.
        onView(withText(R.string.top_header)).check(doesNotExist());
        onView(allOf(
                withText(newFoodstuff.get().getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(doesNotExist());

        // Смена фрагмента с возвратом
        onView(withId(R.id.menu_item_profile)).perform(click());
        onView(withId(R.id.menu_item_foodstuffs)).perform(click());

        // Проверим, что появился топ и продукт в нём.
        onView(withText(R.string.top_header)).check(matches(isDisplayed()));
        onView(allOf(
                withText(newFoodstuff.get().getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(matches(isDisplayed()));
    }

    @Test
    public void topDisappears_whenHistoryIsCleaned() {
        mActivityRule.launchActivity(null);

        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();
        // Проверим, что заголовок топа есть
        onView(withText(R.string.top_header)).check(matches(isDisplayed()));
        // Проверим, что продукт есть в топе
        onView(allOf(
                withText(topFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(matches(isDisplayed()));

        // Очистим историю
        historyWorker.requestAllHistoryFromDb((allHistory)->{
            for (HistoryEntry entry : allHistory) {
                historyWorker.deleteEntryFromHistory(entry);
            }
        });

        // Рестарт
        mActivityRule.finishActivity();
        mActivityRule.launchActivity(null);

        // Проверим, что щаголовок топа пропал
        onView(withText(R.string.top_header)).check(doesNotExist());
        // Проверим, что продукта в топе больше нет
        onView(allOf(
                withText(topFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(doesNotExist());
    }

    @Test
    public void editedFoodstuffReplacesInTop() {
        mActivityRule.launchActivity(null);
        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        Matcher<View> topMatcher = allOf(
                withText(topFoodstuffs.get(1).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).perform(click());
        onView(withId(R.id.button_edit)).perform(click());

        // Редактируем
        String newName = topFoodstuffs.get(1).getName() + "1";
        onView(withId(R.id.foodstuff_name)).perform(replaceText(newName));
        onView(withId(R.id.save_button)).perform(click());

        // Закрываем карточку
        onView(withId(R.id.button_close)).perform(click());

        // Проверяем отредактированное
        topMatcher = allOf(
                withText(newName),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).check(matches(isDisplayed()));
    }

    @Test
    public void mainScreenSeesTopFoodstuffsUpdate() {
        // Добавим новый продукт в БД
        Foodstuff newFoodstuff = Foodstuff.withName("newfoodstuff").withNutrition(1, 2, 3, 4);
        AtomicLong newFoodstuffId = new AtomicLong();
        databaseWorker.saveFoodstuff(newFoodstuff, id -> newFoodstuffId.set(id));

        // "Съедим" его 100 раз сегодня
        NewHistoryEntry[] newEntries = new NewHistoryEntry[100];
        for (int index = 0; index < newEntries.length; ++index) {
            newEntries[index] = new NewHistoryEntry(newFoodstuffId.get(), 100, timeProvider.now().toDate());
        }
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);

        // Старт
        mActivityRule.launchActivity(null);

        // Убедимся, что продукт действительно попал в топ
        boolean found = false;
        List<Foodstuff> top = extractFoodstuffsTopFromDB();
        for (Foodstuff foodstuff : top) {
            if (foodstuff.getId() == newFoodstuffId.get()) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        // Продукт должен появиться на экране в топе
        onView(withText(newFoodstuff.getName())).check(matches(isDisplayed()));
    }

    @Test
    public void topProducts_takenFromWeeklyTop() {
        clearAllData();

        Foodstuff foodstuff1 = Foodstuff.withName("apple1").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff2 = Foodstuff.withName("apple2").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff3 = Foodstuff.withName("apple3").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff4 = Foodstuff.withName("apple4").withNutrition(1, 2, 3, 4);
        foodstuff1 = foodstuffsList.saveFoodstuff(foodstuff1).blockingGet();
        foodstuff2 = foodstuffsList.saveFoodstuff(foodstuff2).blockingGet();
        foodstuffsList.saveFoodstuff(foodstuff3);
        foodstuffsList.saveFoodstuff(foodstuff4);

        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusDays(3).toDate(),
                foodstuff1.getId(),
                123);
        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusWeeks(2).toDate(),
                foodstuff2.getId(),
                123);

        // Start
        mActivityRule.launchActivity(null);

        Matcher<View> topHeaderMatcher = withText(R.string.top_header);
        Matcher<View> allHeaderMatcher = withText(R.string.all_foodstuffs_header);
        onView(topHeaderMatcher).check(matches(isDisplayed()));
        onView(allHeaderMatcher).check(matches(isDisplayed()));

        // foodstuff1 в истории менее недели в прошлом
        onView(allOf(
                withText(foodstuff1.getName()),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(matches(isDisplayed()));

        // foodstuff2 в истории более недели в прошлом
        onView(allOf(
                withText(foodstuff2.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(doesNotExist());

        // foodstuff3 не в истории
        onView(allOf(
                withText(foodstuff3.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(doesNotExist());

        // foodstuff4 не в истории
        onView(allOf(
                withText(foodstuff4.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(doesNotExist());
    }

    @Test
    public void topProducts_orderAndLimit() {
        clearAllData();

        List<Foodstuff> foodstuffs = new ArrayList<>();
        int eatenTwiceFoodstuffIndex = 2;

        // TOP_ITEMS_MAX_COUNT x2 products to history
        for (int index = 0; index < MainScreenController.TOP_ITEMS_MAX_COUNT + 2; ++index) {
            Foodstuff foodstuff = Foodstuff.withName("apple" + index).withNutrition(1, 2, 3, 4);
            foodstuff = foodstuffsList.saveFoodstuff(foodstuff).blockingGet();
            foodstuffs.add(foodstuff);

            historyWorker.saveFoodstuffToHistory(
                    timeProvider.now().minusSeconds(index).toDate(),
                    foodstuff.getId(),
                    123);
            // One of the foodstuffs is eaten twice
            if (index == eatenTwiceFoodstuffIndex) {
                historyWorker.saveFoodstuffToHistory(
                        timeProvider.now().minusSeconds(index).toDate(),
                        foodstuff.getId(),
                        123);
            }
        }

        // Start
        mActivityRule.launchActivity(null);

        // eatenTwiceFoodstuffIndex is first
        onView(allOf(
                withText(foodstuffs.get(eatenTwiceFoodstuffIndex).getName()),
                EspressoUtils.matches(isCompletelyBelow(withText(R.string.top_header))),
                EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))
        )).check(matches(isDisplayed()));

        // Other foodstuffs are below one by one
        for (int index = 0; index < MainScreenController.TOP_ITEMS_MAX_COUNT; ++index) {
            if (index == eatenTwiceFoodstuffIndex) {
                continue;
            }
            int foodstuffAboveIndex;
            if (index == 0) {
                foodstuffAboveIndex = eatenTwiceFoodstuffIndex;
            } else {
                foodstuffAboveIndex = index - 1;
            }
            Matcher<View> foodstuffAbove = allOf(
                    withText(foodstuffs.get(foodstuffAboveIndex).getName()),
                    EspressoUtils.matches(isCompletelyBelow(withText(R.string.top_header))),
                    EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))));

            onView(allOf(
                    withText(foodstuffs.get(index).getName()),
                    EspressoUtils.matches(isCompletelyBelow(foodstuffAbove)),
                    EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))
            )).check(matches(isDisplayed()));
        }

        // Only TOP_ITEMS_MAX_COUNT foodstuffs should exist, others - shouldn't
        for (int index = MainScreenController.TOP_ITEMS_MAX_COUNT; index < foodstuffs.size(); ++index) {
            onView(allOf(
                    withText(foodstuffs.get(index).getName()),
                    EspressoUtils.matches(isCompletelyBelow(withText(R.string.top_header))),
                    EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))
            )).check(doesNotExist());
        }
    }

    private void addFoodstuffToDate(DateTime date, long... foodstuffsIds) {
        NewHistoryEntry[] newEntries = new NewHistoryEntry[foodstuffsIds.length];
        for (int index = 0; index < foodstuffsIds.length; ++index) {
            int weight = 100;
            newEntries[index] =
                    new NewHistoryEntry(foodstuffsIds[index], weight, date.toDate());
            date = date.plusMinutes(1);
        }
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);
    }

    private List<Foodstuff> extractFoodstuffsTopFromDB() {
        return topList.getMonthTop().blockingGet();
    }
}
