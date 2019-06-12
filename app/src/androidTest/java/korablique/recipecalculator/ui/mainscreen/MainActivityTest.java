package korablique.recipecalculator.ui.mainscreen;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.github.mikephil.charting.charts.LineChart;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
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
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.FullName;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.GoalCalculator;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.TopList;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.history.HistoryController;
import korablique.recipecalculator.ui.history.HistoryFragment;
import korablique.recipecalculator.ui.profile.NewMeasurementsDialog;
import korablique.recipecalculator.ui.profile.ProfileController;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;
import korablique.recipecalculator.util.TestingTimeProvider;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.BundleMatchers.hasValue;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {
    private DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
    private ComputationThreadsExecutor computationThreadsExecutor =
            new InstantComputationsThreadsExecutor();
    private MainThreadExecutor mainThreadExecutor = new SyncMainThreadExecutor();
    private DatabaseHolder databaseHolder;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private FoodstuffsList foodstuffsList;
    private TopList topList;
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Foodstuff[] foodstuffs;
    private List<Long> foodstuffsIds = new ArrayList<>();
    private BucketList bucketList = BucketList.getInstance();
    private UserParameters userParameters;
    private UserNameProvider userNameProvider;
    private TimeProvider timeProvider;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(MainActivity.class)
                .withManualStart()
                .withSingletones(() -> {
                    databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                    databaseWorker = new DatabaseWorker(
                            databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                    historyWorker = new HistoryWorker(
                            databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                    userParametersWorker = new UserParametersWorker(
                            databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                    foodstuffsList = new FoodstuffsList(
                            databaseWorker, mainThreadExecutor, computationThreadsExecutor);
                    topList = new TopList(databaseWorker, historyWorker);
                    userNameProvider = new UserNameProvider(context);
                    timeProvider = new TestingTimeProvider();
                    return Arrays.asList(databaseWorker, historyWorker, userParametersWorker,
                            foodstuffsList, databaseHolder, userNameProvider,
                            timeProvider);
                })
                .withActivityScoped((injectionTarget) -> {
                    if (!(injectionTarget instanceof MainActivity)) {
                        return Collections.emptyList();
                    }
                    MainActivity activity = (MainActivity) injectionTarget;
                    ActivityCallbacks activityCallbacks = activity.getActivityCallbacks();
                    RxActivitySubscriptions subscriptions = new RxActivitySubscriptions(activityCallbacks);
                    MainActivityController controller = new MainActivityController(
                            activity, activityCallbacks, userParametersWorker, subscriptions);
                    return Collections.singletonList(controller);
                })
                .withFragmentScoped((injectionTarget -> {
                    if (injectionTarget instanceof NewMeasurementsDialog) {
                        return Collections.emptyList();
                    }

                    BaseFragment fragment = (BaseFragment) injectionTarget;
                    FragmentCallbacks fragmentCallbacks = fragment.getFragmentCallbacks();
                    RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                    BaseActivity activity = (BaseActivity) fragment.getActivity();
                    Lifecycle lifecycle = activity.getLifecycle();
                    if (fragment instanceof MainScreenFragment) {
                        MainScreenController mainScreenController = new MainScreenController(
                                activity, fragment, fragment.getFragmentCallbacks(),
                                activity.getActivityCallbacks(), lifecycle, topList, foodstuffsList);
                        return Arrays.asList(subscriptions, mainScreenController);

                    } else if (fragment instanceof ProfileFragment) {
                        ProfileController profileController = new ProfileController(
                                fragment, fragmentCallbacks, userParametersWorker, subscriptions,
                                userNameProvider,
                                timeProvider);
                        return Arrays.asList(subscriptions, profileController);
                    } else if (fragment instanceof HistoryFragment) {
                        HistoryController historyController = new HistoryController(
                                activity, fragment, fragmentCallbacks, historyWorker,
                                userParametersWorker, subscriptions, timeProvider);
                        return Arrays.asList(subscriptions, historyController);
                    } else if (fragment instanceof SearchResultsFragment) {
                        return Arrays.asList(databaseWorker, lifecycle, activity, foodstuffsList, subscriptions);
                    } else {
                        throw new IllegalStateException("There is no such fragment class");
                    }
                }))
                .build();

    @Before
    public void setUp() {
        databaseHolder.getDatabase().clearAllTables();

        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });

        foodstuffs = new Foodstuff[7];
        foodstuffs[0] = Foodstuff.withName("apple").withNutrition(1, 1, 1, 1);
        foodstuffs[1] = Foodstuff.withName("pineapple").withNutrition(1, 1, 1, 1);
        foodstuffs[2] = Foodstuff.withName("plum").withNutrition(1, 1, 1, 1);
        foodstuffs[3] = Foodstuff.withName("water").withNutrition(1, 1, 1, 1);
        foodstuffs[4] = Foodstuff.withName("soup").withNutrition(1, 1, 1, 1);
        foodstuffs[5] = Foodstuff.withName("bread").withNutrition(1, 1, 1, 1);
        foodstuffs[6] = Foodstuff.withName("banana").withNutrition(1, 1, 1, 1);

        databaseWorker.saveGroupOfFoodstuffs(foodstuffs, (ids) -> {
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

        // сохраняем userParameters в БД
        userParameters = new UserParameters(45, Gender.FEMALE, new LocalDate(1993, 9, 27),
                158, 48, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, timeProvider.nowUtc().getMillis());
        userParametersWorker.saveUserParameters(userParameters);

        FullName fullName = new FullName("Yulia", "Zhilyaeva");
        userNameProvider.saveUserName(fullName);

        // каждый тест должен сам сделать launchActivity()
    }

    @Test
    public void topHeaderDoNotDisplayedIfHistoryIsEmpty() {
        databaseHolder.getDatabase().clearAllTables();
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
            List<Fragment> fragments = mActivityRule.getActivity().getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                fragment.onActivityResult(IntentConstants.EDIT_FOODSTUFF_REQUEST, Activity.RESULT_OK, data);
            }
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

    @Test
    public void deletingFoodstuffsWorks() {
        mActivityRule.launchActivity(null);

        Foodstuff deletingFoodstuff = Foodstuff
                .withId(foodstuffsIds.get(0))
                .withName(foodstuffs[0].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]));
        onView(allOf(
                withText(deletingFoodstuff.getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.button_delete)).perform(click());
        onView(withText(deletingFoodstuff.getName())).check(doesNotExist());

        List<Foodstuff> foodstuffsListAfterDeleting = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(100, new DatabaseWorker.FoodstuffsBatchReceiveCallback() {
            @Override
            public void onReceive(List<Foodstuff> foodstuffs) {
                foodstuffsListAfterDeleting.addAll(foodstuffs);
            }
        });
        Assert.assertFalse(foodstuffsListAfterDeleting.contains(deletingFoodstuff));

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(deletingFoodstuff.getName())).check(doesNotExist());
    }

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
    public void profileDisplaysCorrectUserParameters() {
        mActivityRule.launchActivity(null);
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
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
                withParent(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getProtein()))));
        onView(allOf(
                withParent(withId(R.id.fats_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getFats()))));
        onView(allOf(
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
        databaseHolder.getDatabase().clearAllTables();
        // сохраняем параметры на дату в прошлом 12.8+1.2019 12:00
        DateTime lastDate = new DateTime(2019, 8, 12, 12, 12, 0, DateTimeZone.UTC);
        UserParameters lastParams = new UserParameters(45, Gender.FEMALE, new LocalDate(1993, 9, 27),
                158, 49.6f, Lifestyle.ACTIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, lastDate.getMillis());
        userParametersWorker.saveUserParameters(lastParams);

        mActivityRule.launchActivity(null);
        // переходим в профиль
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
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
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
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
        databaseHolder.getDatabase().clearAllTables();
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
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
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

    @Test
    public void todaysFoodstuffsDisplayedInHistory() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);

        onView(withId(R.id.menu_item_history)).perform(click());

        Matcher<View> foodstuffBelowMatcher1 = allOf(
                withText(containsString(foodstuffs[0].getName())),
                matches(isCompletelyBelow(withId(R.id.title_layout))));
        onView(foodstuffBelowMatcher1).check(matches(isDisplayed()));

        Matcher<View> foodstuffBelowMatcher2 = allOf(
                withText(containsString(foodstuffs[5].getName())),
                matches(isCompletelyBelow(withText(containsString(foodstuffs[0].getName())))));
        onView(foodstuffBelowMatcher2).check(matches(isDisplayed()));

        Matcher<View> foodstuffBelowMatcher3 = allOf(
                withText(containsString(foodstuffs[6].getName())),
                matches(isCompletelyBelow(withText(containsString(foodstuffs[5].getName())))));
        onView(foodstuffBelowMatcher3).check(matches(isDisplayed()));
    }

    @Test
    public void deletingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        Foodstuff deletedFoodstuff = foodstuffs[0];
        onView(withText(containsString(deletedFoodstuff.getName()))).perform(click());
        // нажать на кнопку удаления в карточке
        onView(withId(R.id.frame_layout_button_delete)).perform(click());
        // проверить, что элемент удалился
        onView(withText(containsString(deletedFoodstuff.getName()))).check(doesNotExist());
        // проверить заголовок с БЖУ
        Nutrition totalNutrition = Nutrition.of(foodstuffs[5].withWeight(100))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
        // перезапустить активити и убедиться, что элемент удалён
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(containsString(deletedFoodstuff.getName()))).check(doesNotExist());
        // ещё раз проверить заголовок
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
    }

    @Test
    public void editingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        Foodstuff editedFoodstuff = foodstuffs[0];
        onView(withText(containsString(editedFoodstuff.getName()))).perform(click());
        // отредактировать вес
        double newWeight = 200;
        onView(withId(R.id.weight_edit_text)).perform(replaceText(String.valueOf(newWeight)));
        onView(withId(R.id.add_foodstuff_button)).perform(click());
        // проверить, что элемент отредактировался
        onView(withText(containsString(editedFoodstuff.getName())))
                .check(matches(withText(containsString(toDecimalString(newWeight)))));
        // проверить заголовок с БЖУ
        Nutrition totalNutrition = Nutrition.of(editedFoodstuff.withWeight(newWeight))
                .plus(Nutrition.of(foodstuffs[5].withWeight(100)))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
        // перезапустить активити и убедиться, что элемент изменён
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(containsString(editedFoodstuff.getName())))
                .check(matches(withText(containsString(toDecimalString(newWeight)))));
        // ещё раз проверить заголовок
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view)))
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
        Matcher<View> proteinMatcher = allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view));
        onView(proteinMatcher).check(matches(withText(toDecimalString(totalNutrition.getProtein()))));

        Matcher<View> fatsMatcher = allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view));
        onView(fatsMatcher).check(matches(withText(toDecimalString(totalNutrition.getFats()))));

        Matcher<View> carbsMatcher = allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view));
        onView(carbsMatcher).check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));

        Matcher<View> caloriesMatcher = allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view));
        onView(caloriesMatcher).check(matches(withText(toDecimalString(totalNutrition.getCalories()))));

        // проверяем значения норм БЖУК
        Matcher<View> proteinRateMatcher = allOf(withParent(withId(R.id.protein_layout)), withId(R.id.of_n_grams));
        onView(proteinRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getProtein()))))));

        Matcher<View> fatsRateMatcher = allOf(withParent(withId(R.id.fats_layout)), withId(R.id.of_n_grams));
        onView(fatsRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getFats()))))));

        Matcher<View> carbsRateMatcher = allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.of_n_grams));
        onView(carbsRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getCarbs()))))));

        Matcher<View> caloriesRateMatcher = allOf(withParent(withId(R.id.calories_layout)), withId(R.id.of_n_grams));
        onView(caloriesRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(totalNutrition.getCalories()))))));

        // проверяем прогресс
        Activity activity = mActivityRule.getActivity();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ProgressBar proteinProgress = activity.findViewById(R.id.protein_layout).findViewById(R.id.nutrition_progress);
            Assert.assertEquals(Math.round((float)totalNutrition.getProtein()), proteinProgress.getProgress());
            Assert.assertEquals(Math.round(rates.getProtein()), proteinProgress.getMax());

            ProgressBar fatsProgress = activity.findViewById(R.id.fats_layout).findViewById(R.id.nutrition_progress);
            Assert.assertEquals(Math.round((float)totalNutrition.getFats()), fatsProgress.getProgress());
            Assert.assertEquals(Math.round(rates.getFats()), fatsProgress.getMax());

            ProgressBar carbsProgress = activity.findViewById(R.id.carbs_layout).findViewById(R.id.nutrition_progress);
            Assert.assertEquals(Math.round((float)totalNutrition.getCarbs()), carbsProgress.getProgress());
            Assert.assertEquals(Math.round(rates.getCarbs()), carbsProgress.getMax());

            ProgressBar caloriesProgress = activity.findViewById(R.id.calories_layout).findViewById(R.id.nutrition_progress);
            Assert.assertEquals(Math.round((float)totalNutrition.getCalories()), caloriesProgress.getProgress());
            Assert.assertEquals(Math.round(rates.getCalories()), caloriesProgress.getMax());
        });
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
                MainActivity.createAddToHistoryIntent(
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
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.add_foodstuff_button)).perform(click());
        onView(withId(R.id.basket)).perform(click());

        // Проверяем, что была попытка стартовать активити по интенту от BucketListActivity
        Intent expectedIntent =
                BucketListActivity.createStartIntentFor(addedFoodstuffs, mActivityRule.getActivity(), anyDay.toLocalDate());
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasComponent(expectedIntent.getComponent()),
                hasExtras(hasValue(anyDay.toLocalDate())),
                hasExtras(hasValue(addedFoodstuffs))));
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
        Intent intent = MainActivity.createAddToHistoryIntent(context, addedFoodstuffs, date);
        mActivityRule.launchActivity(intent);

        onView(withText(containsString(addedFoodstuffs.get(0).getName()))).check(matches(isDisplayed()));
        onView(withId(R.id.title_text)).check(matches(withText(date.toString("dd.MM.yy"))));
    }

    // поиск

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
                matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))))).perform(click());
        // удаляем его
        onView(withId(R.id.button_delete)).perform(click());
        // нужно проверять не только текст, но и родителя,
        // т к иначе в проверку попадут вьюшки из MainScreen
        onView(allOf(
                withText(searchingFoodstuff.getName()),
                isDescendantOfA(withId(R.id.search_results_recycler_view)),
                matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button)))))
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
                public void onResult(long id) {}

                @Override
                public void onDuplication() {}
            });
        });
        onView(allOf(
                withText(newFoodstuff.getName()),
                matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void onBackPressedLastSearchQueryReturns() {
        mActivityRule.launchActivity(null);

        // ввести название одного продукта
        onView(withId(R.id.search_bar_text)).perform(click());
        onView(withId(R.id.search_bar_text)).perform(replaceText(foodstuffs[0].getName()));
        onView(withId(R.id.search_bar_text)).perform(pressImeActionButton()); // enter

        // другого
        onView(withId(R.id.search_bar_text)).perform(click());
        onView(withId(R.id.search_bar_text)).perform(replaceText(foodstuffs[1].getName()));
        onView(withId(R.id.search_bar_text)).perform(pressImeActionButton());

        // нажать Назад
        onView(isRoot()).perform(ViewActions.pressBack()); // первое нажатие закрывает клавиатуру
        onView(isRoot()).perform(ViewActions.pressBack());

        // убедиться, что в searchView находится название первого продукта
        onView(withId(R.id.search_bar_text)).check(matches(withText(foodstuffs[0].getName())));

        // нажать ещё раз назад и убедиться, что SearchResultsFragment закрылся
        onView(isRoot()).perform(ViewActions.pressBack());
        onView(withId(R.id.search_results_layout)).check(doesNotExist());
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

    private List<Foodstuff> extractFoodstuffsTopFromDB() {
        List<Foodstuff> listedFoodstuffs = new ArrayList<>();
        historyWorker.requestListedFoodstuffsFromHistoryForPeroid(0, Long.MAX_VALUE, listedFoodstuffs::addAll);

        List<PopularProductsUtils.FoodstuffFrequency> foodstuffFrequencies = PopularProductsUtils.getTop(listedFoodstuffs);
        List<Foodstuff> top = new ArrayList<>();
        for (PopularProductsUtils.FoodstuffFrequency frequency : foodstuffFrequencies) {
            top.add(frequency.getFoodstuff());
        }
        return top;
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
}
