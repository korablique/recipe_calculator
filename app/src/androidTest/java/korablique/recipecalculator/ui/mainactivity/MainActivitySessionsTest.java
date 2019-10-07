package korablique.recipecalculator.ui.mainactivity;

import android.app.Instrumentation;
import android.widget.DatePicker;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.platform.app.InstrumentationRegistry;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;

import korablique.recipecalculator.R;
import korablique.recipecalculator.util.SessionTestingHelper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
            Runnable checkOfRestartWithSessionChange) {
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
        };

        Runnable whenSessionChanged = () -> {
            // Должен быть показан главный экран, т.к. прошла сессия
            onView(withId(R.id.fragment_profile)).check(matches(not(isDisplayed())));
            onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));
        };

        performSessionsTest(init, whenSessionNotChanged, whenSessionChanged);
    }

    @Test
    public void mainScreenSelectedDate() {
        Runnable init = () -> {
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
        };

        Runnable whenSessionNotChanged = () -> {
            // Кнопка всё ещё должна быть на экране
            onView(withId(R.id.return_for_today_button)).check(matches(isDisplayed()));
        };

        Runnable whenSessionChanged = () -> {
            // Кнопка должна пропасть с экрана, т.к. новая сессия
            onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));
        };

        performSessionsTest(init, whenSessionNotChanged, whenSessionChanged);
    }
}
