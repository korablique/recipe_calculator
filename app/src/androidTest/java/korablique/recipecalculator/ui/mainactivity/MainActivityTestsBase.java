package korablique.recipecalculator.ui.mainactivity;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.SoftKeyboardStateWatcher;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.FoodstuffsTopList;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.FullName;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.session.SessionController;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.mainactivity.history.HistoryController;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenCardController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenReadinessDispatcher;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenSearchController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.UpFABController;
import korablique.recipecalculator.ui.mainactivity.profile.NewMeasurementsDialog;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileController;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileFragment;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;
import korablique.recipecalculator.util.TestingTimeProvider;

public class MainActivityTestsBase {
    protected DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
    protected ComputationThreadsExecutor computationThreadsExecutor =
            new InstantComputationsThreadsExecutor();
    protected MainThreadExecutor mainThreadExecutor = new SyncMainThreadExecutor();
    protected DatabaseHolder databaseHolder;
    protected DatabaseWorker databaseWorker;
    protected HistoryWorker historyWorker;
    protected UserParametersWorker userParametersWorker;
    protected FoodstuffsList foodstuffsList;
    protected FoodstuffsTopList topList;
    protected Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    protected Foodstuff[] foodstuffs;
    protected List<Long> foodstuffsIds = new ArrayList<>();
    protected BucketList bucketList;
    protected UserParameters userParameters;
    protected UserNameProvider userNameProvider;
    protected TestingTimeProvider timeProvider;
    protected MainActivityFragmentsController fragmentsController;
    protected MainActivitySelectedDateStorage mainActivitySelectedDateStorage;
    protected RxActivitySubscriptions activitySubscriptions;
    protected CurrentActivityProvider currentActivityProvider;
    protected SessionController sessionController;
    protected MainScreenCardController mainScreenCardController;
    protected SharedPrefsManager prefsManager;
    protected SoftKeyboardStateWatcher softKeyboardStateWatcher;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(MainActivity.class)
                    .withManualStart()
                    .withSingletones(() -> {
                        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);
                        prefsManager = new SharedPrefsManager(context);
                        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        historyWorker = new HistoryWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        userParametersWorker = new UserParametersWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        foodstuffsList = new FoodstuffsList(
                                databaseWorker, mainThreadExecutor, computationThreadsExecutor);
                        timeProvider = new TestingTimeProvider();
                        topList = new FoodstuffsTopList(historyWorker, timeProvider);
                        userNameProvider = new UserNameProvider(context);
                        currentActivityProvider = new CurrentActivityProvider();
                        sessionController = new SessionController(context, timeProvider, currentActivityProvider);
                        bucketList = new BucketList(prefsManager, foodstuffsList);
                        return Arrays.asList(databaseWorker, historyWorker, userParametersWorker,
                                foodstuffsList, databaseHolder, userNameProvider,
                                timeProvider, currentActivityProvider, sessionController,
                                new CalcKeyboardController(), bucketList);
                    })
                    .withActivityScoped((injectionTarget) -> {
                        if (!(injectionTarget instanceof MainActivity)) {
                            return Collections.emptyList();
                        }
                        MainActivity activity = (MainActivity) injectionTarget;
                        ActivityCallbacks activityCallbacks = activity.getActivityCallbacks();
                        mainActivitySelectedDateStorage = new MainActivitySelectedDateStorage(
                                activity, activityCallbacks, sessionController, timeProvider);
                        fragmentsController = new MainActivityFragmentsController(
                                activity, sessionController, activityCallbacks);
                        softKeyboardStateWatcher = new SoftKeyboardStateWatcher(activity, mainThreadExecutor);
                        MainActivityController controller = new MainActivityController(
                                activity, activityCallbacks, fragmentsController);
                        activitySubscriptions = new RxActivitySubscriptions(activityCallbacks);
                        return Collections.singletonList(controller);
                    })
                    .withFragmentScoped((injectionTarget -> {
                        if (injectionTarget instanceof NewMeasurementsDialog
                                || injectionTarget instanceof CardDialog) {
                            return Collections.emptyList();
                        }

                        BaseFragment fragment = (BaseFragment) injectionTarget;
                        FragmentCallbacks fragmentCallbacks = fragment.getFragmentCallbacks();
                        RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                        BaseActivity activity = (BaseActivity) fragment.getActivity();
                        Lifecycle lifecycle = activity.getLifecycle();
                        mainScreenCardController = new MainScreenCardController(
                                activity, fragment, fragmentCallbacks, lifecycle, bucketList,
                                historyWorker, timeProvider, mainActivitySelectedDateStorage);

                        if (fragment instanceof MainScreenFragment) {
                            MainScreenReadinessDispatcher readinessDispatcher =
                                    new MainScreenReadinessDispatcher();

                            mainScreenCardController = new MainScreenCardController(
                                    activity, fragment, fragmentCallbacks, lifecycle,
                                    bucketList, historyWorker, timeProvider,
                                    mainActivitySelectedDateStorage);

                            MainScreenSearchController searchController = new MainScreenSearchController(
                                    mainThreadExecutor, bucketList, foodstuffsList, (MainScreenFragment) fragment, activity.getActivityCallbacks(),
                                    fragmentCallbacks, mainScreenCardController, readinessDispatcher,
                                    activitySubscriptions, softKeyboardStateWatcher);

                            UpFABController upFABController = new UpFABController(
                                    fragmentCallbacks, readinessDispatcher);

                            MainScreenController mainScreenController = new MainScreenController(
                                    activity, fragment, fragmentCallbacks,
                                    activity.getActivityCallbacks(), bucketList, topList,
                                    foodstuffsList, mainActivitySelectedDateStorage,
                                    mainScreenCardController, readinessDispatcher);
                            return Arrays.asList(subscriptions, mainScreenController,
                                    upFABController, mainScreenCardController, searchController);

                        } else if (fragment instanceof ProfileFragment) {
                            ProfileController profileController = new ProfileController(
                                    fragment, fragmentCallbacks, userParametersWorker, subscriptions,
                                    userNameProvider,
                                    timeProvider);
                            return Arrays.asList(subscriptions, profileController);
                        } else if (fragment instanceof HistoryFragment) {
                            HistoryController historyController = new HistoryController(
                                    activity, fragment, fragmentCallbacks, historyWorker,
                                    userParametersWorker, subscriptions, timeProvider,
                                    fragmentsController, mainActivitySelectedDateStorage);
                            return Arrays.asList(subscriptions, historyController);
                        } else if (fragment instanceof SearchResultsFragment) {
                            return Arrays.asList(databaseWorker, lifecycle, activity,
                                    foodstuffsList, subscriptions, mainScreenCardController);
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
            foodstuffs[0] = foodstuffs[0].recreateWithId(foodstuffsIds.get(0));
            foodstuffs[1] = foodstuffs[1].recreateWithId(foodstuffsIds.get(1));
            foodstuffs[2] = foodstuffs[2].recreateWithId(foodstuffsIds.get(2));
            foodstuffs[3] = foodstuffs[3].recreateWithId(foodstuffsIds.get(3));
            foodstuffs[4] = foodstuffs[4].recreateWithId(foodstuffsIds.get(4));
            foodstuffs[5] = foodstuffs[5].recreateWithId(foodstuffsIds.get(5));
            foodstuffs[6] = foodstuffs[6].recreateWithId(foodstuffsIds.get(6));
        });

        NewHistoryEntry[] newEntries = new NewHistoryEntry[10];
        // 1 day: apple, bread, banana
        newEntries[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100, timeProvider.now().minusYears(10).toDate());
        newEntries[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100, timeProvider.now().minusYears(10).toDate());
        newEntries[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100, timeProvider.now().minusYears(10).toDate());
        // 2 day: apple, water
        newEntries[3] = new NewHistoryEntry(foodstuffsIds.get(0), 100, timeProvider.now().minusYears(9).toDate());
        newEntries[4] = new NewHistoryEntry(foodstuffsIds.get(3), 100, timeProvider.now().minusYears(9).toDate());
        // 3 day: bread, soup
        newEntries[5] = new NewHistoryEntry(foodstuffsIds.get(5), 100, timeProvider.now().minusYears(8).toDate());
        newEntries[6] = new NewHistoryEntry(foodstuffsIds.get(4), 100, timeProvider.now().minusYears(8).toDate());
        // 4 day: apple, pineapple, water
        newEntries[7] = new NewHistoryEntry(foodstuffsIds.get(0), 100, timeProvider.now().minusDays(7).toDate());
        newEntries[8] = new NewHistoryEntry(foodstuffsIds.get(1), 100, timeProvider.now().minusDays(7).toDate());
        newEntries[9] = new NewHistoryEntry(foodstuffsIds.get(3), 100, timeProvider.now().minusDays(7).toDate());
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);

        // сохраняем userParameters в БД
        userParameters = new UserParameters(45, Gender.FEMALE, new LocalDate(1993, 9, 27),
                158, 48, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, timeProvider.nowUtc().getMillis());
        userParametersWorker.saveUserParameters(userParameters);

        FullName fullName = new FullName("Yulia", "Zhilyaeva");
        userNameProvider.saveUserName(fullName);

        // каждый тест должен сам сделать launchActivity()
    }
}
