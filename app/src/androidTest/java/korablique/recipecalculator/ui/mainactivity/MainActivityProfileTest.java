package korablique.recipecalculator.ui.mainactivity;

import android.app.Instrumentation;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.github.mikephil.charting.charts.LineChart;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.GoalCalculator;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityProfileTest extends MainActivityTestsBase {
    @Test
    public void profileDisplaysCorrectUserParameters() {
        mActivityRule.launchActivity(null);
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());
        // сейчас в UserParametersWorker'е только одни параметры.
        // проверяем, что они отображаются в профиле
        String ageString = String.valueOf(userParameters.getAge());
        onView(withId(R.id.age)).check(matches((withText(containsString(ageString)))));
        onView(withId(R.id.height)).check(matches(withText(String.valueOf(userParameters.getHeight()))));

        String targetWeightString = toDecimalString(userParameters.getTargetWeight());
        onView(withId(R.id.target_weight)).check(matches(withText(targetWeightString)));

        String currentWeightString = toDecimalString(userParameters.getWeight());
        onView(withId(R.id.current_weight_measurement_value)).check(matches(withText(currentWeightString)));

        onView(withId(R.id.user_name)).check(matches(withText(userNameProvider.getUserName().toString())));
        DateTime measurementsDate = new DateTime(userParameters.getMeasurementsTimestamp());
        String measurementsDateString = measurementsDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.last_measurement_date_measurement_value)).check(matches(withText(measurementsDateString)));

        // проверяем, что отображаются правильные нормы
        Rates rates = RateCalculator.calculate(userParameters);
        onView(withId(R.id.calorie_intake)).check(matches(withText(toDecimalString(rates.getCalories()))));
        onView(allOf(
                withEffectiveVisibility(VISIBLE),
                withParent(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getProtein()))));
        onView(allOf(
                withEffectiveVisibility(VISIBLE),
                withParent(withId(R.id.fats_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getFats()))));
        onView(allOf(
                withEffectiveVisibility(VISIBLE),
                withParent(withId(R.id.carbs_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getCarbs()))));

        // проверяем процент достижения цели
        int percent = GoalCalculator.calculateProgressPercentage(
                userParameters.getWeight(), userParameters.getWeight(), userParameters.getTargetWeight());
        onView(withId(R.id.done_percent)).check(matches(withText(String.valueOf(percent))));
    }

    @Test
    public void measurementsCardDisplaysPreviousDate() {
        clearAllData();
        // сохраняем параметры на дату в прошлом 12.8+1.2019 12:00
        DateTime lastDate = new DateTime(2019, 8, 12, 12, 12, 0, DateTimeZone.UTC);
        UserParameters lastParams = new UserParameters(45, Gender.FEMALE, new LocalDate(1993, 9, 27),
                158, 49.6f, Lifestyle.ACTIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, lastDate.getMillis());
        userParametersWorker.saveUserParameters(lastParams);

        mActivityRule.launchActivity(null);
        // переходим в профиль
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());
        // открываем карточку
        onView(withId(R.id.set_current_weight)).perform(click());
        // сверяем, что дата прошлых измерений правильная
        String dateMustBe = lastDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.last_measurement_header)).check(matches(withText(containsString(dateMustBe))));
    }

    @Test
    public void measurementsCardDisplaysTodaysDate() {
        mActivityRule.launchActivity(null);
        // переходим в профиль
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());
        // открываем карточку
        onView(withId(R.id.set_current_weight)).perform(click());
        // проверяем сегодняшнюю дату
        DateTime todaysDate = timeProvider.nowUtc();
        String dateMustBe = todaysDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.new_measurement_header)).check(matches(withText(containsString(dateMustBe))));
    }

    @Test
    public void canChangeMeasurementsPeriodsInProfileChart() {
        // Clear DB again (remove existing user parameters added in setUp).
        clearAllData();
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });
        
        // Add 5 measurements to each month for last 2 years
        // Note that measurement time is NOW minus 1 minute - this is to avoid clashes
        // with time periods starts/ends. 
        DateTime measurementTime = timeProvider.now().minusMinutes(1);
        for (int monthIndex = 24; monthIndex >= 1; --monthIndex) {
            for (int measurementIndex = 0; measurementIndex < 5; ++measurementIndex) {
                UserParameters userParameters = new UserParameters(
                        65, Gender.MALE, new LocalDate(1993, 7, 20), 165, 65+monthIndex+measurementIndex,
                        Lifestyle.PROFESSIONAL_SPORTS, Formula.MIFFLIN_JEOR, measurementTime.getMillis());
                userParametersWorker.saveUserParameters(userParameters);
            }
            measurementTime = measurementTime.minusMonths(1);
        }
        mActivityRule.launchActivity(null);
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());

        // Check that there're 120 dots when all the time is open
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(120, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });

        // Check that there're 60 dots when year view is open
        onView(withId(R.id.measurements_period_spinner)).perform(click());
        onView(withText(R.string.user_measurements_period_array_year)).perform(click());
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(60, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });

        // Check that there're 30 dots when 6 months view is open
        onView(withId(R.id.measurements_period_spinner)).perform(click());
        onView(withText(R.string.user_measurements_period_array_6_months)).perform(click());
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(30, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });

        // Check that there're 5 dots when 1 month view is open
        onView(withId(R.id.measurements_period_spinner)).perform(click());
        onView(withText(R.string.user_measurements_period_array_month)).perform(click());
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(5, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });
    }
}
