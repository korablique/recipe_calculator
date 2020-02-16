package korablique.recipecalculator.ui.mainactivity;

import android.view.View;

import androidx.test.espresso.Espresso;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.util.EspressoUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class MainActivitySearchTest extends MainActivityTestsBase {
    @Before
    public void setUp() {
        super.setUp();

        // Чтоб топ в результатах поиска не мешал
        historyWorker.requestAllHistoryFromDb(historyEntries -> {
            historyEntries = new ArrayList<>(historyEntries);
            for (HistoryEntry entry : historyEntries) {
                historyWorker.deleteEntryFromHistory(entry);
            }
        });
    }

    @Test
    public void switchingFragmentClosesSearchResults() {
        mActivityRule.launchActivity(null);

        // В начале результатов поиска быть не должно
        onView(withId(R.id.search_results_layout)).check(doesNotExist());

        // Поиск
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("banana"));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Результаты поиска должны появиться
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // Меняем фрагмент на Историю, возвращаемся обратно
        onView(withId(R.id.menu_item_history)).perform(click());
        onView(withId(R.id.menu_item_foodstuffs)).perform(click());

        // Результаты поиска должны пропасть
        onView(withId(R.id.search_results_layout)).check(doesNotExist());
        // Введённый текст "banana" тоже
        onView(withHint(R.string.search)).check(matches(not(withText("banana"))));
    }

    @Test
    public void transliteratedQueriesSearch() {
        Foodstuff foodstuff = Foodstuff.withName("шоколад Ritter Sport").withNutrition(1, 2, 3, 4);
        foodstuffsList.saveFoodstuff(foodstuff, new FoodstuffsList.SaveFoodstuffCallback() {
            @Override public void onResult(Foodstuff addedFoodstuff) {}
            @Override public void onDuplication() {}
        });

        mActivityRule.launchActivity(null);

        onView(allOf(
                isDescendantOfA(withId(R.id.search_layout)),
                withText(containsString("Ritter Sport")))).check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("риттер спорт"));

        onView(allOf(
                isDescendantOfA(withId(R.id.search_layout)),
                withText(containsString("Ritter Sport")))).check(matches(isDisplayed()));

        Espresso.closeSoftKeyboard();
    }

    @Test
    public void deletingFromSearchResultsWorks() {
        mActivityRule.launchActivity(null);

        Foodstuff searchingFoodstuff = foodstuffs[0];
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(searchingFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter
        // нажимаем на результат поиска
        onView(allOf(
                withText(searchingFoodstuff.getName()),
                isDescendantOfA(withId(R.id.search_results_recycler_view)),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))))).perform(click());
        // удаляем его
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());
        onView(withId(R.id.positive_button)).perform(click());
        // нужно проверять не только текст, но и родителя,
        // т к иначе в проверку попадут вьюшки из MainScreen
        onView(allOf(
                withText(searchingFoodstuff.getName()),
                isDescendantOfA(withId(R.id.search_results_recycler_view)),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button)))))
                .check(doesNotExist());
    }

    @Test
    public void whenSavingNewFoodstuffFromSearchResultsItAppearsInSearchResults() {
        mActivityRule.launchActivity(null);

        Foodstuff newFoodstuff = Foodstuff.withName("granola").withNutrition(10, 10, 60, 450);
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(newFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter
        mainThreadExecutor.execute(() -> {
            foodstuffsList.saveFoodstuff(newFoodstuff, new FoodstuffsList.SaveFoodstuffCallback() {
                @Override
                public void onResult(Foodstuff addedFoodstuff) {}

                @Override
                public void onDuplication() {}
            });
        });
        onView(allOf(
                withText(newFoodstuff.getName()),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void searchQueryCleaned_whenFocusedLost() {
        mActivityRule.launchActivity(null);

        // Клик на строку поиска и ввод строки поиска
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("word"));
        // Убеждаемся, что текст на месте
        onView(withHint(R.string.search)).check(matches(withText("word")));

        // Убираем фокус нажатием на Back и проверяем, что текст пропал
        Espresso.pressBack();
        onView(withHint(R.string.search)).check(matches(withText("")));
    }

    @Test
    public void searchQueryNotCleaned_whenFocusedLost_whenSearchResultsArePresent() {
        mActivityRule.launchActivity(null);

        // Делаем поиск продукта
        Foodstuff searchingFoodstuff = foodstuffs[0];
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(searchingFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Убеждаемся, что показаны результаты поиска
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // На всякий случай кликаем на строку поиска ещё раз, чтобы она точно была в фокусе
        onView(withHint(R.string.search)).perform(click());
        // Убираем фокус со строки поиска
        Espresso.pressBack();
        // Убеждаемся, что текст никуда не делся
        onView(withHint(R.string.search)).check(matches(withText(searchingFoodstuff.getName())));
    }

    @Test
    public void searchQueryCleaned_whenSearchResultsGone() {
        mActivityRule.launchActivity(null);

        // Делаем поиск продукта
        Foodstuff searchingFoodstuff = foodstuffs[0];
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(searchingFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Убеждаемся, что показаны результаты поиска
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // На всякий случай кликаем на строку поиска ещё раз, чтобы она точно была в фокусе
        onView(withHint(R.string.search)).perform(click());
        // Убираем фокус со строки поиска
        Espresso.pressBack();
        // Закрываем экран поиска
        Espresso.pressBack();
        // Убеждаемся, что результат поиска пропал
        onView(withId(R.id.search_results_layout)).check(doesNotExist());
        // Убеждаемся, что текст запроса пропал
        onView(withHint(R.string.search)).check(matches(withText("")));
    }

    @Test
    public void mainScreenDisplaysCard_afterFoodstuffCreation_fromSearchFragment() {
        mActivityRule.launchActivity(null);

        String name = "111first_foodstuff";
        onView(withText(name)).check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(name));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // We expect the foodstuff to not exist yet
        onView(withId(R.id.nothing_found_view)).check(matches(isDisplayed()));
        onView(withId(R.id.add_new_foodstuff_button)).perform(click());

        onView(withId(R.id.foodstuff_name)).perform(replaceText(name));
        onView(withId(R.id.protein_value)).perform(replaceText("10"));
        onView(withId(R.id.fats_value)).perform(replaceText("10"));
        onView(withId(R.id.carbs_value)).perform(replaceText("10"));
        onView(withId(R.id.calories_value)).perform(replaceText("10"));
        onView(withId(R.id.save_button)).perform(click());

        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withText(name))).check(matches(isDisplayed()));
    }

    @Test
    public void searchResultsFragmentChangesResultDynamicallyWhenQueryChanges() {
        mActivityRule.launchActivity(null);

        // Banana!
        String name1 = "banana";
        onView(allOf(
                withText(name1),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(name1));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter
        onView(allOf(
                withText(name1),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));

        // Apple!
        String name2 = "apple";
        onView(allOf(
                withText(name2),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(name2));
        // No pressing enter!
//        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Apple is expected
        onView(allOf(
                withText(name2),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));
        // Banana is not expected
        onView(allOf(
                withText(name1),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(doesNotExist());
    }

    @Test
    public void searchSuggestionsAndQueryDisappear_whenFoodstuffCardClosedWithResult() {
        mActivityRule.launchActivity(null);

        String name = foodstuffs[0].getName();
        // Query - не всё имя целиком, а то если они будут эквивалентны,
        // будет сложно разбираться check'ами на экране, где запрос, а где подсказка.
        String query = name.substring(0, name.length()-1);
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(query));

        // Клик на подсказку
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.search_layout))))
                .perform(click());

        // Добавляем продукт в Историю
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button1)).perform(click());

        // Подсказки не должно больше быть
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(doesNotExist());
        // Запрос должен очиститься
        onView(allOf(
                withText(query),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(doesNotExist());
    }

    @Test
    public void searchSuggestionsAndQueryDoNotDisappear_whenFoodstuffCardDismissed() {
        mActivityRule.launchActivity(null);

        String name = foodstuffs[0].getName();
        // Query - не всё имя целиком, а то если они будут эквивалентны,
        // будет сложно разбираться check'ами на экране, где запрос, а где подсказка.
        String query = name.substring(0, name.length()-1);
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(query));

        // Клик на подсказку
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.search_layout))))
                .perform(click());

        // Закроем карточку без какого-либо полезного действия в ней
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.button_close)).perform(click());

        // Подсказка должна остаться на месте
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(matches(isDisplayed()));
        // Запрос тоже должен остаться на месте
        onView(allOf(
                withText(query),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void searchResultsAndQueryDisappear_whenFoodstuffCardClosedWithResult() {
        mActivityRule.launchActivity(null);

        String name = foodstuffs[0].getName();
        // Query - не всё имя целиком, а то если они будут эквивалентны,
        // будет сложно разбираться check'ами на экране, где запрос, а где подсказка.
        String query = name.substring(0, name.length()-1);
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(query));

        // enter
        onView(withHint(R.string.search)).perform(pressImeActionButton());
        // Должны появиться результаты
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // Клик на результат
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.search_results_layout))))
                .perform(click());

        // Добавляем продукт в Историю
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button1)).perform(click());

        // Результатов не должно больше быть
        onView(withId(R.id.search_results_layout)).check(doesNotExist());
        // Запрос должен очиститься
        onView(allOf(
                withText(query),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(doesNotExist());
    }

    @Test
    public void searchResultsAndQueryDoNotDisappear_whenFoodstuffCardDismissed() {
        mActivityRule.launchActivity(null);

        String name = foodstuffs[0].getName();
        // Query - не всё имя целиком, а то если они будут эквивалентны,
        // будет сложно разбираться check'ами на экране, где запрос, а где подсказка.
        String query = name.substring(0, name.length()-1);
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(query));

        // enter
        onView(withHint(R.string.search)).perform(pressImeActionButton());
        // Должны появиться результаты
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // Клик на результат
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.search_results_layout))))
                .perform(click());

        // Закроем карточку без какого-либо полезного действия в ней
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.button_close)).perform(click());

        // Результаты должны остаться на месте
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));
        // Запрос тоже должен остаться на месте
        onView(allOf(
                withText(query),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void monthlyTop_usedInSearchSuggestions() {
        clearAllData();
        mActivityRule.launchActivity(null);

        // add 4 similarly named products to DB
        Foodstuff foodstuff1 = Foodstuff.withName("apple1").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff2 = Foodstuff.withName("apple2").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff3 = Foodstuff.withName("apple3").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff4 = Foodstuff.withName("apple4").withNutrition(1, 2, 3, 4);
        foodstuffsList.saveFoodstuff(foodstuff1);
        foodstuffsList.saveFoodstuff(foodstuff2);
        foodstuffsList.saveFoodstuff(foodstuff3);
        foodstuffsList.saveFoodstuff(foodstuff4);

        // add 5th, which is somewhat similar, but wouldn't be the best match
        Foodstuff foodstuff5 = Foodstuff.withName("apdle5").withNutrition(1, 2, 3, 4);
        foodstuff5 = foodstuffsList.saveFoodstuff(foodstuff5).blockingGet();

        // verify that when similarly named products searched, 5th is not shown in suggestions
        String query = "apple";
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(query));
        onView(allOf(
                withText(foodstuff5.getName()),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(doesNotExist());
        Espresso.pressBack();

        // add 5th to history
        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusWeeks(3).toDate(),
                foodstuff5.getId(),
                123);

        // verify that now when similarly named products searched, 5th is shown in suggestions
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(query));
        onView(allOf(
                withText(foodstuff5.getName()),
                isDescendantOfA(withId(R.id.search_layout))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void monthlyTop_usedInSearchResultsFragments() {
        clearAllData();
        mActivityRule.launchActivity(null);

        Foodstuff foodstuff1 = Foodstuff.withName("apple1").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff2 = Foodstuff.withName("apple2").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff3 = Foodstuff.withName("apple3").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff4 = Foodstuff.withName("apple4").withNutrition(1, 2, 3, 4);
        foodstuff1 = foodstuffsList.saveFoodstuff(foodstuff1).blockingGet();
        foodstuff2 = foodstuffsList.saveFoodstuff(foodstuff2).blockingGet();
        foodstuffsList.saveFoodstuff(foodstuff3);
        foodstuffsList.saveFoodstuff(foodstuff4);

        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusWeeks(3).toDate(),
                foodstuff1.getId(),
                123);
        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusMonths(2).toDate(),
                foodstuff2.getId(),
                123);

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("apple"));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        Matcher<View> topHeaderMatcher = allOf(
                withText(R.string.top_header),
                isDescendantOfA(withId(R.id.search_results_layout)));
        Matcher<View> allHeaderMatcher = allOf(
                withText(R.string.all_foodstuffs_header),
                isDescendantOfA(withId(R.id.search_results_layout)));
        onView(topHeaderMatcher).check(matches(isDisplayed()));
        onView(allHeaderMatcher).check(matches(isDisplayed()));

        // foodstuff1 в истории менее месяца в прошлом
        onView(allOf(
                withText(foodstuff1.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(matches(isDisplayed()));

        // foodstuff2 в истории более месяца в прошлом
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
    public void noHeaders_displayedInSearchResultsFragment_whenFoodstuffsNotFoundInTop() {
        clearAllData();
        mActivityRule.launchActivity(null);

        Foodstuff foodstuff1 = Foodstuff.withName("apple1").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff2 = Foodstuff.withName("apple2").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff3 = Foodstuff.withName("apple3").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff4 = Foodstuff.withName("apple4").withNutrition(1, 2, 3, 4);
        foodstuff1 = foodstuffsList.saveFoodstuff(foodstuff1).blockingGet();
        foodstuff2 = foodstuffsList.saveFoodstuff(foodstuff2).blockingGet();
        foodstuffsList.saveFoodstuff(foodstuff3);
        foodstuffsList.saveFoodstuff(foodstuff4);

        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusMonths(2).toDate(),
                foodstuff1.getId(),
                123);
        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusMonths(3).toDate(),
                foodstuff2.getId(),
                123);

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("apple"));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // В истории нет продукта, добавленного в течение месяца - заголовки "Найдено в топе"
        // и "Найдено во всём" должны отсутствовать.
        Matcher<View> topHeaderMatcher = allOf(
                withText(R.string.top_header),
                isDescendantOfA(withId(R.id.search_results_layout)));
        Matcher<View> allHeaderMatcher = allOf(
                withText(R.string.all_foodstuffs_header),
                isDescendantOfA(withId(R.id.search_results_layout)));
        onView(topHeaderMatcher).check(doesNotExist());
        onView(allHeaderMatcher).check(doesNotExist());
    }
}
