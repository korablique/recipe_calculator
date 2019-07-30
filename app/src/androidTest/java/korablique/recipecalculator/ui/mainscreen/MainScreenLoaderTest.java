package korablique.recipecalculator.ui.mainscreen;

import android.os.Bundle;

import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.Injector;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.FoodstuffsTopList;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.util.EspressoUtils;
import korablique.recipecalculator.util.InjectableActivityTestRule;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.BundleMatchers.hasEntry;
import static androidx.test.espresso.intent.matcher.BundleMatchers.hasValue;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static korablique.recipecalculator.util.EspressoUtils.hasValueRecursive;
import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainScreenLoaderTest {
    @Mock
    private FoodstuffsList foodstuffsList;
    @Mock
    private FoodstuffsTopList foodstuffsTopList;

    private MainScreenLoader mainScreenLoader;

    @Before
    public void setUp() {
        // Init all @Mocks
        MockitoAnnotations.initMocks(this);
        // Init the library which checks intents
        Intents.init();
        // Provide a mock injector (we don't care about real injectable objects in this test)
        InjectorHolder.setInjector(mock(Injector.class));
        // Create an instance of the tested class.
        mainScreenLoader = new MainScreenLoader(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                foodstuffsList,
                foodstuffsTopList);
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
        Bundle expectedFoodstuffsBundle = MainScreenController.createInitialDataBundle(
                new ArrayList<>(expectedTop), new ArrayList<>(expectedAllFoodstuffsPart));

        // Load main screen activity and verify that a start intent was sent
        // (with expected bundle inside).
        mainScreenLoader.loadMainScreenActivity();
        intended(allOf(
                hasAction(MainActivityController.ACTION_OPEN_MAIN_SCREEN),
                hasExtras(hasValueRecursive(expectedFoodstuffsBundle))));

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
            Callback<List<Foodstuff>> callback = invocation.getArgument(0);
            callback.onResult(topFoodstuffs);
            return null;
        }).when(foodstuffsTopList).getTopList(any());

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
