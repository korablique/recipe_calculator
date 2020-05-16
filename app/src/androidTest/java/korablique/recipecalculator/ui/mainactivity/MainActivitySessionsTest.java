package korablique.recipecalculator.ui.mainactivity;

import android.app.Instrumentation;
import android.widget.DatePicker;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.util.SessionTestingHelper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static korablique.recipecalculator.util.EspressoUtils.isNotDisplayed;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class MainActivitySessionsTest extends MainActivityTestsBase {
    @After
    public void tearDown() {
        // Некоторые тесты открывают клавиатуру
        Espresso.closeSoftKeyboard();
        mActivityRule.finishActivity();
    }

    /**
     * Функция вызывается для каждой конкретной фичи, обладающей сессионностью.
     * @param firstSessionAction - первая сессия, в ходе которой фича должна настроить себя.
     * @param checkOfRestartWithoutSessionChange - проверка состояния фичи после рестарта экрана БЕЗ смены сессии.
     * @param checkOfRestartWithSessionChange - проверка состояния фичи после рестарта экрана СО сменой сессии.
     */
    private void performSessionsTest(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithoutSessionChange,
            Runnable checkOfRestartWithSessionChange,
            Runnable checkOfRestartToNextDayWithSessionChange) {
        mActivityRule.launchActivity(null);
        test_activityRecreation_withoutSessionChange(firstSessionAction, checkOfRestartWithoutSessionChange);
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(null);
        test_stopAndStart_withoutSessionChange(firstSessionAction, checkOfRestartWithoutSessionChange);
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(null);
        test_activityRecreation_withSessionChange(firstSessionAction, checkOfRestartWithSessionChange);
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(null);
        test_stopAndStart_withSessionChange(firstSessionAction, checkOfRestartWithSessionChange);
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(null);
        test_activityRecreation_withDayAndSessionChange(firstSessionAction, checkOfRestartToNextDayWithSessionChange);
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(null);
        test_stopAndStart_withDayAndSessionChange(firstSessionAction, checkOfRestartToNextDayWithSessionChange);
    }

    /**
     * @see #performSessionsTest
     */
    private void test_activityRecreation_withoutSessionChange(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithoutSessionChange) {
        firstSessionAction.run();

        // Рестарт
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());

        checkOfRestartWithoutSessionChange.run();
    }

    /**
     * @see #performSessionsTest
     */
    private void test_activityRecreation_withSessionChange(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithSessionChange) {
        SessionTestingHelper.testSessionWith(mActivityRule, timeProvider, currentActivityProvider)
                .withFirstSession(firstSessionAction)
                .withSecondSession(checkOfRestartWithSessionChange)
                .performActivityRecreation();
    }

    /**
     * @see #performSessionsTest
     */
    private void test_stopAndStart_withoutSessionChange(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithoutSessionChange) {
        firstSessionAction.run();

        // Рестарт
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            instrumentation.callActivityOnPause(mActivityRule.getActivity());
            instrumentation.callActivityOnStop(mActivityRule.getActivity());
            instrumentation.callActivityOnStart(mActivityRule.getActivity());
            instrumentation.callActivityOnResume(mActivityRule.getActivity());
        });

        checkOfRestartWithoutSessionChange.run();
    }

    /**
     * @see #performSessionsTest
     */
    private void test_stopAndStart_withSessionChange(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithSessionChange) {
        SessionTestingHelper.testSessionWith(mActivityRule, timeProvider, currentActivityProvider)
                .withFirstSession(firstSessionAction)
                .withSecondSession(checkOfRestartWithSessionChange)
                .performActivityStopAndStart();
    }

    /**
     * @see #performSessionsTest
     */
    private void test_activityRecreation_withDayAndSessionChange(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithSessionChange) {
        SessionTestingHelper.testSessionWith(mActivityRule, timeProvider, currentActivityProvider)
                .withFirstSession(firstSessionAction)
                .withSecondSession(checkOfRestartWithSessionChange)
                .withTimeUntilSecondSession(Duration.standardDays(1))
                .performActivityRecreation();
    }

    /**
     * @see #performSessionsTest
     */
    private void test_stopAndStart_withDayAndSessionChange(
            Runnable firstSessionAction,
            Runnable checkOfRestartWithSessionChange) {
        SessionTestingHelper.testSessionWith(mActivityRule, timeProvider, currentActivityProvider)
                .withFirstSession(firstSessionAction)
                .withSecondSession(checkOfRestartWithSessionChange)
                .withTimeUntilSecondSession(Duration.standardDays(1))
                .performActivityStopAndStart();
    }

    @Test
    public void activeFragment() {
        Runnable init = () -> {
            // профиль
            onView(withId(R.id.menu_item_profile)).perform(click());
            onView(withId(R.id.fragment_profile)).check(matches(isDisplayed()));
        };

        Runnable whenSessionNotChanged = () -> {
            // Должен быть всё ещё показан профиль
            onView(withId(R.id.fragment_profile)).check(matches(isDisplayed()));
            AtomicInteger selectedItem = new AtomicInteger();
            mainThreadExecutor.execute(() -> {
                BottomNavigationView bottomBar = mActivityRule.getActivity().findViewById(R.id.navigation);
                selectedItem.set(bottomBar.getSelectedItemId());
            });
            Assert.assertEquals(R.id.menu_item_profile, selectedItem.get());
        };

        Runnable whenSessionChanged = () -> {
            // Должен быть показан главный экран, т.к. прошла сессия
            onView(withId(R.id.fragment_profile)).check(matches(not(isDisplayed())));
            onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));
            AtomicInteger selectedItem = new AtomicInteger();
            mainThreadExecutor.execute(() -> {
                BottomNavigationView bottomBar = mActivityRule.getActivity().findViewById(R.id.navigation);
                selectedItem.set(bottomBar.getSelectedItemId());
            });
            Assert.assertEquals(R.id.menu_item_foodstuffs, selectedItem.get());
        };

        Runnable whenDayChanged = whenSessionChanged;

        performSessionsTest(init, whenSessionNotChanged, whenSessionChanged, whenDayChanged);
    }

    @Test
    public void mainScreenSelectedDate() {
        Runnable init = () -> {
            clearAllData();

            // добавляем продукт на вчера и на сегодня
            Foodstuff foodstuff1 = Foodstuff.withName("apple").withNutrition(1, 1, 1, 1);
            Foodstuff foodstuff2 = Foodstuff.withName("banana").withNutrition(1, 1, 1, 1);
            foodstuff1 = foodstuffsList.saveFoodstuff(foodstuff1).blockingGet();
            foodstuff2 = foodstuffsList.saveFoodstuff(foodstuff2).blockingGet();
            historyWorker.saveFoodstuffToHistory(
                    timeProvider.now().minusDays(1).toDate(), foodstuff1.getId(), 123f);
            historyWorker.saveFoodstuffToHistory(
                    timeProvider.now().toDate(), foodstuff2.getId(), 123f);

            // открываем историю
            onView(withId(R.id.menu_item_history)).perform(click());

            // проверяем, что на сегодняшней дате кнопки "Сегодня" нет, ...
            onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));
            // ... что сегодня съеден banana
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("banana")))).check(matches(isDisplayed()));
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("apple")))).check(isNotDisplayed());

            // открываем другую дату, ...
            DateTime anotherDate = timeProvider.now().minusDays(1);
            onView(withId(R.id.calendar_button)).perform(click());
            onView(withClassName(equalTo(DatePicker.class.getName())))
                    .perform(PickerActions.setDate(
                            anotherDate.getYear(), anotherDate.getMonthOfYear(), anotherDate.getDayOfMonth()));
            onView(withId(android.R.id.button1)).perform(click());
            // ... проверяем, что есть кнопка "Сегодня", ...
            onView(withId(R.id.return_for_today_button)).check(matches(isDisplayed()));
            // ... что съеден apple
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("banana")))).check(isNotDisplayed());
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("apple")))).check(matches(isDisplayed()));
        };

        Runnable whenSessionNotChanged = () -> {
            // Кнопка всё ещё должна быть на экране
            onView(withId(R.id.return_for_today_button)).check(matches(isDisplayed()));
            // apple должен быть всё ещё показан
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("banana")))).check(isNotDisplayed());
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("apple")))).check(matches(isDisplayed()));
        };

        Runnable whenSessionChanged = () -> {
            // открываем историю
            onView(withId(R.id.menu_item_history)).perform(click());

            // Кнопка должна пропасть с экрана, т.к. новая сессия
            onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));
            // И banana должен быть показан
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("banana")))).check(matches(isDisplayed()));
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("apple")))).check(isNotDisplayed());
        };

        Runnable whenDayChanged = () -> {
            // открываем историю
            onView(withId(R.id.menu_item_history)).perform(click());

            // Кнопка должна пропасть с экрана, т.к. новая сессия
            onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));
            // Показан новый день - продукты в него мы не добавляли, должно быть пусто
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("banana")))).check(isNotDisplayed());
            onView(allOf(
                    isDescendantOfA(withId(R.id.fragment_history)),
                    withText(containsString("apple")))).check(isNotDisplayed());
        };

        performSessionsTest(init, whenSessionNotChanged, whenSessionChanged, whenDayChanged);
    }
}
