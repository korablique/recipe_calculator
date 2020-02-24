package korablique.recipecalculator.ui.mainactivity;

import android.content.Context;
import android.content.Intent;

import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Single;
import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.FoodstuffsTopList;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer;
import korablique.recipecalculator.test.FakeTestActivity;
import korablique.recipecalculator.util.TestingInjector;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static korablique.recipecalculator.util.EspressoUtils.hasValueRecursive;
import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainScreenLoaderTest {
    private Context context;
    private TestingInjector injector;

    @Rule
    public ActivityTestRule<FakeTestActivity> fakeActivityTestRule =
            new ActivityTestRule<>(FakeTestActivity.class, false, false /* launch activity */);

    @Mock
    private FoodstuffsList foodstuffsList;
    @Mock
    private FoodstuffsTopList foodstuffsTopList;

    private MainScreenLoader mainScreenLoader;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        injector = new TestingInjector(
                () -> Collections.singletonList(new CurrentActivityProvider()),
                (activity) -> Arrays.asList(
                        mock(MainActivityController.class),
                        mock(InteractiveServerUserParamsObtainer.class)),
                null);

        // Init all @Mocks
        MockitoAnnotations.initMocks(this);
        doReturn(Single.just(Collections.emptyList())).when(foodstuffsTopList).getMonthTop();
        doReturn(Single.just(Collections.emptyList())).when(foodstuffsTopList).getWeekTop();

        // Init the library which checks intents
        Intents.init();
        // Provide an injector which is capable of giving basic dependencies
        InjectorHolder.setInjector(injector);
        // Create an instance of the tested class.
        mainScreenLoader = new MainScreenLoader(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                foodstuffsList,
                foodstuffsTopList);

        fakeActivityTestRule.launchActivity(null);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void loadsMainScreen() {
        testLoading(5, MainScreenLoader.LOADED_FOODSTUFFS_COUNT*2, MainScreenLoader.LOADED_FOODSTUFFS_COUNT*2);
    }

    @Test
    public void loadsMainScreen_whenFoodstuffsGivenInBatches() {
        // Size of a batch is 2.
        testLoading(5, MainScreenLoader.LOADED_FOODSTUFFS_COUNT*2, 2);
    }

    @Test
    public void loadsMainScreen_whenTopEmpty() {
        testLoading(
                0, // Top is empty.
                MainScreenLoader.LOADED_FOODSTUFFS_COUNT*2,
                MainScreenLoader.LOADED_FOODSTUFFS_COUNT*2);
    }

    @Test
    public void loadsMainScreen_whenNumberOfFoodstuffsLessThanWanted() {
        testLoading(
                5,
                MainScreenLoader.LOADED_FOODSTUFFS_COUNT-5, // Number of foodstuffs is less than wanted by 5
                MainScreenLoader.LOADED_FOODSTUFFS_COUNT*2);
    }

    @Test
    public void loadsMainScreen_when0Foodstuffs() {
        testLoading(0, 0, 0); // No foodstuffs!
    }

    private void testLoading(int topFoodstuffsSize, int fullFoodstuffsListSize, int fullFoodstuffsListBatchSize) {
        // Create lists.
        List<Foodstuff> topFoodstuffs = storeFoodstuffsInTop(topFoodstuffsSize);
        List<Foodstuff> allFoodstuffs =
                storeFoodstuffsInFullFoodstuffsList(
                        fullFoodstuffsListSize,
                        fullFoodstuffsListBatchSize);

        // We expect that foodstuffs from full foodstuffs list are limited to the size of
        // MainScreenLoader.LOADED_FOODSTUFFS_COUNT.
        List<Foodstuff> expectedAllFoodstuffsPart = allFoodstuffs.stream()
                .limit(MainScreenLoader.LOADED_FOODSTUFFS_COUNT)
                .collect(Collectors.toList());
        // We expect top foodstuffs list to be unchanged.
        List<Foodstuff> expectedTop = topFoodstuffs;
        // Create expected bundle.
        Intent expectedIntent = MainActivityController
                .createMainScreenIntent(context,
                        new ArrayList<>(expectedTop),
                        new ArrayList<>(expectedAllFoodstuffsPart));

        // Load main screen activity and verify that a start intent was sent
        // (with expected bundle inside).
        mainScreenLoader.loadMainScreenActivity(fakeActivityTestRule.getActivity()).subscribe();
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasExtras(hasValueRecursive(expectedIntent.getExtras()))));

        // Let's wait for the activity to fully load so it wouldn't crash
        // (InjectableActivityTestRule erases all objects from injectable on tests finish).
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private List<Foodstuff> storeFoodstuffsInTop(int count) {
        List<Foodstuff> topFoodstuffs = new ArrayList<>();
        for (int index = 0; index < count; ++index) {
            topFoodstuffs.add(
                    Foodstuff.withName(String.valueOf(index))
                            .withNutrition(Nutrition.zero()));
        }

        doAnswer(invocation -> {
            return Single.just(topFoodstuffs);
        }).when(foodstuffsTopList).getWeekTop();

        return topFoodstuffs;
    }

    private List<Foodstuff> storeFoodstuffsInFullFoodstuffsList(int count, int batchSize) {
        List<Foodstuff> allFoodstuffs = new ArrayList<>();

        for (int index = 0; index < count; ++index) {
            allFoodstuffs.add(
                    Foodstuff.withName(String.valueOf(index))
                            .withNutrition(Nutrition.zero()));
        }

        doAnswer(invocation -> {
            Callback<List<Foodstuff>> batchCallback = invocation.getArgument(0);
            List<Foodstuff> batch = new ArrayList<>(batchSize);
            for (int index = 0; index < allFoodstuffs.size(); ++index) {
                batch.add(allFoodstuffs.get(index));
                if (batch.size() == batchSize || index+1 == allFoodstuffs.size()) {
                    batchCallback.onResult(new ArrayList<>(batch));
                    batch.clear();
                }
            }

            Callback<List<Foodstuff>> finalCallback = invocation.getArgument(1);
            finalCallback.onResult(allFoodstuffs);
            return null;
        }).when(foodstuffsList).getAllFoodstuffs(any(), any());

        return allFoodstuffs;
    }
}
