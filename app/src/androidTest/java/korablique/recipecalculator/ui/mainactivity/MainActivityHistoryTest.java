package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.util.EspressoUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.util.EspressoUtils.hasMaxProgress;
import static korablique.recipecalculator.util.EspressoUtils.hasProgress;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityHistoryTest extends MainActivityTestsBase {

    @Test
    public void todaysFoodstuffsDisplayedInHistory() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);

        onView(withId(R.id.menu_item_history)).perform(click());

        Matcher<View> foodstuffBelowMatcher1 = allOf(
                withText(containsString(foodstuffs[6].getName())),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.title_layout))),
                isCompletelyDisplayed());
        onView(foodstuffBelowMatcher1).check(matches(isDisplayed()));

        Matcher<View> foodstuffBelowMatcher2 = allOf(
                withText(containsString(foodstuffs[5].getName())),
                EspressoUtils.matches(isCompletelyBelow(foodstuffBelowMatcher1)),
                isCompletelyDisplayed());
        onView(foodstuffBelowMatcher2).check(matches(isDisplayed()));

        Matcher<View> foodstuffBelowMatcher3 = allOf(
                withText(containsString(foodstuffs[0].getName())),
                EspressoUtils.matches(isCompletelyBelow(foodstuffBelowMatcher2)),
                isCompletelyDisplayed());
        onView(foodstuffBelowMatcher3).check(matches(isDisplayed()));
    }

    @Test
    public void deletingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        Foodstuff deletedFoodstuff = foodstuffs[0];
        onView(allOf(
                withText(containsString(deletedFoodstuff.getName())),
                isCompletelyDisplayed())).perform(click());
        // нажать на кнопку удаления в карточке
        onView(withId(R.id.frame_layout_button_delete)).perform(click());
        // проверить, что элемент удалился
        onView(allOf(
                withText(containsString(deletedFoodstuff.getName())),
                isCompletelyDisplayed())).check(doesNotExist());
        // проверить заголовок с БЖУ
        Nutrition totalNutrition = Nutrition.of(foodstuffs[5].withWeight(100))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
        // перезапустить активити и убедиться, что элемент удалён
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(containsString(deletedFoodstuff.getName()))).check(doesNotExist());
        // ещё раз проверить заголовок
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
    }

    @Test
    public void editingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        Foodstuff editedFoodstuff = foodstuffs[0];
        onView(allOf(
                withText(containsString(editedFoodstuff.getName())),
                isCompletelyDisplayed())).perform(click());
        // отредактировать вес
        double newWeight = 200;
        onView(withId(R.id.weight_edit_text)).perform(replaceText(String.valueOf(newWeight)));
        onView(withId(R.id.button1)).perform(click());
        // проверить, что элемент отредактировался
        onView(allOf(
                withText(containsString(editedFoodstuff.getName())),
                isCompletelyDisplayed()))
                .check(matches(withText(containsString(toDecimalString(newWeight)))));
        // проверить заголовок с БЖУ
        Nutrition totalNutrition = Nutrition.of(editedFoodstuff.withWeight(newWeight))
                .plus(Nutrition.of(foodstuffs[5].withWeight(100)))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        onView(allOf(
                withParent(allOf(withId(R.id.protein_layout), isCompletelyDisplayed())),
                withId(R.id.nutrition_text_view),
                isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
        // перезапустить активити и убедиться, что элемент изменён
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(allOf(
                withText(containsString(editedFoodstuff.getName())),
                isCompletelyDisplayed()))
                .check(matches(withText(containsString(toDecimalString(newWeight)))));
        // ещё раз проверить заголовок
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
    }

    @Test
    public void todaysTotalNutritionDisplayedInHistory() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);

        Nutrition totalNutrition = Nutrition.of(foodstuffs[0].withWeight(100))
                .plus(Nutrition.of(foodstuffs[5].withWeight(100)))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        Rates rates = RateCalculator.calculate(userParameters);

        onView(withId(R.id.menu_item_history)).perform(click());

        // проверяем значение съеденного нутриента
        Matcher<View> proteinMatcher = allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(proteinMatcher).check(matches(withText(toDecimalString(totalNutrition.getProtein()))));

        Matcher<View> fatsMatcher = allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(fatsMatcher).check(matches(withText(toDecimalString(totalNutrition.getFats()))));

        Matcher<View> carbsMatcher = allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(carbsMatcher).check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));

        Matcher<View> caloriesMatcher = allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(caloriesMatcher).check(matches(withText(toDecimalString(totalNutrition.getCalories()))));

        // проверяем значения норм БЖУК
        Matcher<View> proteinRateMatcher = allOf(
                withParent(withId(R.id.protein_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(proteinRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getProtein()))))));

        Matcher<View> fatsRateMatcher = allOf(
                withParent(withId(R.id.fats_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(fatsRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getFats()))))));

        Matcher<View> carbsRateMatcher = allOf(
                withParent(withId(R.id.carbs_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(carbsRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getCarbs()))))));

        Matcher<View> caloriesRateMatcher = allOf(
                withParent(withId(R.id.calories_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(caloriesRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(totalNutrition.getCalories()))))));

        // проверяем прогресс
        Matcher<View> proteinProgress = allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(proteinProgress).check(hasProgress(Math.round((float)totalNutrition.getProtein())));
        onView(proteinProgress).check(hasMaxProgress(Math.round(rates.getProtein())));

        Matcher<View> fatsProgress = allOf(
                isDescendantOfA(withId(R.id.fats_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(fatsProgress).check(hasProgress(Math.round((float)totalNutrition.getFats())));
        onView(fatsProgress).check(hasMaxProgress(Math.round(rates.getFats())));

        Matcher<View> carbsProgress = allOf(
                isDescendantOfA(withId(R.id.carbs_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(carbsProgress).check(hasProgress(Math.round((float)totalNutrition.getCarbs())));
        onView(carbsProgress).check(hasMaxProgress(Math.round(rates.getCarbs())));

        Matcher<View> caloriesProgress = allOf(
                isDescendantOfA(withId(R.id.calories_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(caloriesProgress).check(hasProgress(Math.round((float)totalNutrition.getCalories())));
        onView(caloriesProgress).check(hasMaxProgress(Math.round(rates.getCalories())));
    }

    @Test
    public void addingToHistoryFromBucketListWorks() {
        // сохраняем в БД фудстаффы, которые будем потом добавлять в историю
        Foodstuff f1 = Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32);
        Foodstuff f2 = Foodstuff.withName("oil").withNutrition(0, 99.9, 0, 899);
        List<Long> addingFoodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                new Foodstuff[]{f1, f2},
                addingFoodstuffsIds::addAll);
        List<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withId(addingFoodstuffsIds.get(0))
                .withName(f1.getName())
                .withNutrition(f1.getProtein(), f1.getFats(), f1.getCarbs(), f1.getCalories())
                .withWeight(310));
        foodstuffs.add(Foodstuff.withId(addingFoodstuffsIds.get(1))
                .withName(f2.getName())
                .withNutrition(f2.getProtein(), f2.getFats(), f2.getCarbs(), f2.getCalories())
                .withWeight(13));

        Intent startIntent =
                MainActivityController.createOpenHistoryAndAddFoodstuffsIntent(
                        context, foodstuffs, timeProvider.now().toLocalDate());
        mActivityRule.launchActivity(startIntent);

        onView(withText(containsString(f1.getName()))).check(matches(isDisplayed()));
        onView(withText(containsString(f2.getName()))).check(matches(isDisplayed()));
    }

    public void canSwitchDateInHistoryFragment() {
        // сохранить продукты в историю на другую дату (30 января)
        NewHistoryEntry[] newEntries1 = new NewHistoryEntry[3];
        DateTime jan30 = new DateTime(2019, 1, 30, 0, 0, 0);
        newEntries1[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100, jan30.toDate());
        newEntries1[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100, jan30.toDate());
        newEntries1[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100, jan30.toDate());
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries1);

        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        onView(withText(containsString(foodstuffs[0].getName()))).check(doesNotExist());
        onView(withText(containsString(foodstuffs[5].getName()))).check(doesNotExist());
        onView(withText(containsString(foodstuffs[6].getName()))).check(doesNotExist());

        onView(withId(R.id.calendar_button)).perform(click());
        // Change the date of the DatePicker.
        // Don't use "withId" as at runtime Android shares the DatePicker id between several sub-elements
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(jan30.getYear(), jan30.getMonthOfYear(), jan30.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());

        onView(withText(containsString(foodstuffs[0].getName()))).check(matches(isDisplayed()));
        onView(withText(containsString(foodstuffs[5].getName()))).check(matches(isDisplayed()));
        onView(withText(containsString(foodstuffs[6].getName()))).check(matches(isDisplayed()));
    }

    @Test
    public void returnForTodayButtonWorksAndDisappearsOnToday() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // проверяем, что на сегодняшней дате кнопки "Сегодня" нет
        onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));

        // открываем другую дату и проверяем, что кнопка появилась
        DateTime anotherDate = timeProvider.now().minusDays(10);
        onView(withId(R.id.calendar_button)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anotherDate.getYear(), anotherDate.getMonthOfYear(), anotherDate.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.return_for_today_button)).check(matches(isDisplayed()));

        // нажимаем на кнопку "Сегодня" и проверяем, что она пропадает
        onView(withId(R.id.return_for_today_button)).perform(click());
        onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));

        // нажимаем на календарь и проверяем, что выбрана сегодняшняя дата
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime now = timeProvider.now();
        onView(withClassName(equalTo(DatePicker.class.getName()))).check(matches(matchesDate(
                now.getYear(), now.getMonthOfYear(), now.getDayOfMonth())));
    }

    @Test
    public void showsDatesInHistoryToolbar() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // проверяем, что надпись Сегодня
        onView(withId(R.id.title_text)).check(matches(withText(R.string.today)));
        // выбираем вчера, проверяем
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime today = timeProvider.now();
        DateTime yesterday = today.minusDays(1);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        yesterday.getYear(), yesterday.getMonthOfYear(), yesterday.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.yesterday)));
        // выбираем позавчера
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime dayBeforeYesterday = today.minusDays(2);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        dayBeforeYesterday.getYear(), dayBeforeYesterday.getMonthOfYear(), dayBeforeYesterday.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.day_before_yesterday)));
        // выбираем завтра
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime tomorrow = today.plusDays(1);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        tomorrow.getYear(), tomorrow.getMonthOfYear(), tomorrow.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.tomorrow)));
        // выбираем послезавтра
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime dayAfterTomorrow = today.plusDays(2);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        dayAfterTomorrow.getYear(), dayAfterTomorrow.getMonthOfYear(), dayAfterTomorrow.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.day_after_tomorrow)));
        // выбираем случайную дату (-50 дней)
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = today.minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(anyDay.toString("dd.MM.yy"))));
        // нажимаем на кнопку Сегодня
        onView(withId(R.id.return_for_today_button)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.today)));
    }

    @Test
    public void toolbarDateIsCorrectOnScreenRotation() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());
        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = timeProvider.now().minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        // повернуть экран, проверить
        Activity activity = mActivityRule.getActivity();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withId(R.id.title_text)).check(matches(withText(anyDay.toString("dd.MM.yy"))));

        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay2 = timeProvider.now().minusDays(30);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay2.getYear(), anyDay2.getMonthOfYear(), anyDay2.getDayOfMonth()));
        // повернуть экран и нажать в календаре ОК
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onView(withId(android.R.id.button1)).perform(click());
        // проверить, что дата правильная
        onView(withId(R.id.title_text)).check(matches(withText(anyDay2.toString("dd.MM.yy"))));
    }

    @Test
    public void addingFoodstuffsToCertainDateWorks() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = timeProvider.now().minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        // проверим, что дата выбрана
        onView(withText(anyDay.toString("dd.MM.yy"))).check(matches(isDisplayed()));

        // добавить продукт
        ArrayList<WeightedFoodstuff> addedFoodstuffs = new ArrayList<>(1);
        addedFoodstuffs.add(
                Foodstuff.withId(foodstuffsIds.get(0))
                        .withName(foodstuffs[0].getName())
                        .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]))
                        .withWeight(123));
        onView(withId(R.id.history_fab)).perform(click());
        onView(allOf(
                withText(addedFoodstuffs.get(0).getName()),
                EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button1)).perform(click());

        // подтверждаем намерение добавить продукт на не текущаю дату
        onView(withId(R.id.positive_button)).perform(click());

        // переключаем обратно на Историю
        onView(withId(R.id.menu_item_history)).perform(click());
        // проверим, что дата всё ещё выбрана
        onView(withText(anyDay.toString("dd.MM.yy"))).check(matches(isDisplayed()));
        // проверим, что продукт есть на экране
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(addedFoodstuffs.get(0).getName()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void addingFoodstuffsToCertainDate_canBeSwitchedToAddingToToday() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = timeProvider.now().minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        // проверим, что дата выбрана
        onView(withText(anyDay.toString("dd.MM.yy"))).check(matches(isDisplayed()));

        // добавить продукт
        ArrayList<WeightedFoodstuff> addedFoodstuffs = new ArrayList<>(1);
        addedFoodstuffs.add(
                Foodstuff.withId(foodstuffsIds.get(0))
                        .withName(foodstuffs[0].getName())
                        .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]))
                        .withWeight(123));
        onView(withId(R.id.history_fab)).perform(click());
        onView(allOf(
                withText(addedFoodstuffs.get(0).getName()),
                EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button1)).perform(click());

        // передумываем - добавляем продукт на сегодня вместо выбранной даты
        onView(withId(R.id.negative_button)).perform(click());

        // переключаем обратно на Историю
        onView(withId(R.id.menu_item_history)).perform(click());
        // проверим, что дата в прошлом уже не выбрана, а выбрано "Сегодня"
        onView(withText(anyDay.toString("dd.MM.yy"))).check(doesNotExist());
        onView(allOf(
                withText(R.string.today),
                isDescendantOfA(withId(R.id.title_layout))))
                .check(matches(isDisplayed()));
        // проверим, что продукт есть на экране
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(addedFoodstuffs.get(0).getName()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void foodstuffsAddedOnCertainDate_ShownInHistory() {
        ArrayList<WeightedFoodstuff> addedFoodstuffs = new ArrayList<>(1);
        addedFoodstuffs.add(
                Foodstuff.withId(foodstuffsIds.get(0))
                        .withName(foodstuffs[0].getName())
                        .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]))
                        .withWeight(200));
        LocalDate date = timeProvider.now().minusDays(25).toLocalDate();
        Intent intent = MainActivityController.createOpenHistoryAndAddFoodstuffsIntent(context, addedFoodstuffs, date);
        mActivityRule.launchActivity(intent);

        onView(withText(containsString(addedFoodstuffs.get(0).getName()))).check(matches(isDisplayed()));
        onView(withId(R.id.title_text)).check(matches(withText(date.toString("dd.MM.yy"))));
    }

    @Test
    public void mainScreenFoodstuffCard_addsFoodstuffToHistory() {
        mActivityRule.launchActivity(null);

        // Клик на продукт и ввод массы
        onView(allOf(
                withText(foodstuffs[0].getName()),
                EspressoUtils.matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));

        // Жмём на кнопку добавления в Историю
        onView(withId(R.id.button1)).perform(click());

        // Переходим в Историю и убеждаемся, что продукт там
        onView(withId(R.id.menu_item_history)).perform(click());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(matches(isDisplayed()));

        // Делаем рестарт и проверяем ещё раз
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(matches(isDisplayed()));
    }

    // https://stackoverflow.com/a/44840330
    public static Matcher<View> matchesDate(final int year, final int month, final int day) {
        return new BoundedMatcher<View, DatePicker>(DatePicker.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("matches date:");
            }

            @Override
            protected boolean matchesSafely(DatePicker item) {
                return (year == item.getYear() && month == item.getMonth() + 1 && day == item.getDayOfMonth());
            }
        };
    }

    private void addFoodstuffsToday() {
        NewHistoryEntry[] newEntries = new NewHistoryEntry[3];
        DateTime today = timeProvider.now();
        newEntries[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 8, 0).toDate());
        newEntries[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 9, 0).toDate());
        newEntries[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 10, 0).toDate());
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);
    }
}
