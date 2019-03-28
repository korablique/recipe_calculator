package korablique.recipecalculator.ui.mainscreen;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import junit.framework.Assert;

import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
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

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import korablique.recipecalculator.IntentConstants;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
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
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.TopList;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.DecimalUtils;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.history.HistoryController;
import korablique.recipecalculator.ui.history.HistoryFragment;
import korablique.recipecalculator.ui.profile.ProfileController;
import korablique.recipecalculator.ui.profile.ProfileFragment;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;
import korablique.recipecalculator.util.TimeUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
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
                    foodstuffsList =
                            new FoodstuffsList(databaseWorker, mainThreadExecutor, computationThreadsExecutor);
                    topList = new TopList(context, databaseWorker, historyWorker);
                    userNameProvider = new UserNameProvider(context);
                    return Arrays.asList(databaseWorker, historyWorker, userParametersWorker,
                            foodstuffsList, databaseHolder, userNameProvider);
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
                    BaseFragment fragment = (BaseFragment) injectionTarget;
                    FragmentCallbacks fragmentCallbacks = fragment.getFragmentCallbacks();
                    RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                    BaseActivity activity = (BaseActivity) fragment.getActivity();
                    Lifecycle lifecycle = activity.getLifecycle();
                    if (fragment instanceof MainScreenFragment) {
                        MainScreenController mainScreenController = new MainScreenController(
                                activity, fragment, fragment.getFragmentCallbacks(), lifecycle, topList, foodstuffsList);
                        return Arrays.asList(subscriptions, mainScreenController);

                    } else if (fragment instanceof ProfileFragment) {
                        ProfileController profileController = new ProfileController(
                                fragment, fragmentCallbacks, userParametersWorker, subscriptions, userNameProvider);
                        return Arrays.asList(subscriptions, profileController);
                    } else if (fragment instanceof HistoryFragment) {
                        HistoryController historyController = new HistoryController(
                                activity, (HistoryFragment) fragment, fragmentCallbacks, historyWorker);
                        return Arrays.asList(subscriptions, historyController);
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
                158, 48, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, TimeUtils.currentMillis());
        userParametersWorker.saveUserParameters(userParameters);

        FullName fullName = new FullName("Yulia", "Zhilyaeva");
        userNameProvider.saveUserName(fullName);

        // каждый тест должен сам сделать launchActivity()
    }

    @After
    public void tearDown() {
        databaseWorker = null;
        historyWorker = null;
        userParametersWorker = null;
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
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("2")));
        Assert.assertTrue(bucketList.getList().contains(wf1));
        Assert.assertTrue(bucketList.getList().contains(wf2));

        // убираем все продукты, перезапускаем активити, снекбара быть не должно
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
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
        DateTime todaysDate = DateTime.now(DateTimeZone.UTC);
        String dateMustBe = todaysDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.new_measurement_header)).check(matches(withText(containsString(dateMustBe))));
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
        onView(withText(containsString(foodstuffs[0].getName()))).perform(click());
        // нажать на кнопку удаления в карточке
        onView(withId(R.id.button_delete)).perform(click());
        // проверить, что элемент удалился
        onView(withText(containsString(foodstuffs[0].getName()))).check(doesNotExist());
    }

    @Test
    public void editingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        onView(withText(containsString(foodstuffs[0].getName()))).perform(click());
        // отредактировать вес
        int newWeight = 200;
        onView(withId(R.id.weight_edit_text)).perform(replaceText(String.valueOf(newWeight)));
        onView(withId(R.id.add_foodstuff_button)).perform(click());
        // проверить, что элемент отредактировался
        onView(withText(containsString(foodstuffs[0].getName()))).perform(click());
        onView(withId(R.id.weight_edit_text)).check(matches(withText(String.valueOf(newWeight))));
    }

    private void addFoodstuffsToday() {
        NewHistoryEntry[] newEntries = new NewHistoryEntry[3];
        DateTime today = DateTime.now();
        newEntries[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 8, 0).toDate());
        newEntries[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 9, 0).toDate());
        newEntries[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 10, 0).toDate());
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);
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
        databaseWorker.requestFoodstuffsByIds(topIds, (foodstuffs) -> {
            topFoodstuffs.addAll(foodstuffs);
        });
        return topFoodstuffs;
    }
}
