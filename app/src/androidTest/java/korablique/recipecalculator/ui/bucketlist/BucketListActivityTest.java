package korablique.recipecalculator.ui.bucketlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.core.util.Pair;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import korablique.recipecalculator.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.InstantIOExecutor;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.base.executors.IOExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.CreateRecipeResult;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.RecipeDatabaseWorker;
import korablique.recipecalculator.database.RecipeDatabaseWorkerImpl;
import korablique.recipecalculator.database.RecipesRepository;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Recipe;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.mainactivity.MainActivity;
import korablique.recipecalculator.ui.mainactivity.MainActivityController;
import korablique.recipecalculator.ui.mainactivity.MainActivityFragmentsController;
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage;
import korablique.recipecalculator.ui.mainactivity.history.HistoryController;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment;
import korablique.recipecalculator.util.DBTestingUtils;
import korablique.recipecalculator.util.FloatUtils;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.SyncMainThreadExecutor;
import korablique.recipecalculator.util.TestingTimeProvider;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static korablique.recipecalculator.ui.bucketlist.BucketListActivityKt.EXTRA_PRODUCED_RECIPE;
import static korablique.recipecalculator.util.EspressoUtils.isNotDisplayed;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BucketListActivityTest {
    private Context context = InstrumentationRegistry.getTargetContext();
    private DatabaseHolder databaseHolder;
    private MainThreadExecutor mainThreadExecutor;
    private IOExecutor ioExecutor = new InstantIOExecutor();
    private DatabaseThreadExecutor databaseThreadExecutor;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private FoodstuffsList foodstuffsList;
    private RecipeDatabaseWorker recipeDatabaseWorker;
    private RecipesRepository recipesRepository;
    private UserNameProvider userNameProvider;
    private HistoryController historyController;
    private TimeProvider timeProvider;
    private CurrentActivityProvider currentActivityProvider;
    private BucketList bucketList;
    private SharedPrefsManager prefsManager;

    @Rule
    public ActivityTestRule<BucketListActivity> activityRule =
            InjectableActivityTestRule.forActivity(BucketListActivity.class)
                    .withManualStart()
                    .withSingletones(() -> {
                        context = InstrumentationRegistry.getTargetContext();
                        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);
                        databaseThreadExecutor = new InstantDatabaseThreadExecutor();
                        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                        databaseHolder.getDatabase().clearAllTables();

                        prefsManager = new SharedPrefsManager(context);
                        mainThreadExecutor = new SyncMainThreadExecutor();
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        timeProvider = new TestingTimeProvider();
                        historyWorker = new HistoryWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                                timeProvider);
                        userParametersWorker = new UserParametersWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        foodstuffsList = new FoodstuffsList(databaseWorker, mainThreadExecutor,
                                new InstantComputationsThreadsExecutor());
                        recipeDatabaseWorker = new RecipeDatabaseWorkerImpl(
                                ioExecutor, databaseHolder, databaseWorker);
                        recipesRepository = new RecipesRepository(
                                recipeDatabaseWorker, foodstuffsList, mainThreadExecutor);
                        userNameProvider = new UserNameProvider(context);
                        currentActivityProvider = new CurrentActivityProvider();
                        bucketList = new BucketList(prefsManager);

                        return asList(mainThreadExecutor, databaseThreadExecutor, databaseWorker,
                                historyWorker, userParametersWorker, foodstuffsList, userNameProvider,
                                timeProvider, currentActivityProvider, bucketList,
                                new CalcKeyboardController(), recipesRepository);
                    })
                    .withActivityScoped((target) -> {
                        if (target instanceof BucketListActivity) {
                            BucketListActivity activity = (BucketListActivity) target;
                            BucketListActivityController controller =
                                    new BucketListActivityController(
                                            activity, recipesRepository, bucketList,
                                            mainThreadExecutor);
                            return asList(controller);
                        }
                        MainActivity activity = (MainActivity) target;
                        ActivityCallbacks activityCallbacks = new ActivityCallbacks();
                        MainActivityController controller = new MainActivityController(activity,
                                activityCallbacks,
                                mock(MainActivityFragmentsController.class));

                        return asList(new RxActivitySubscriptions(activity.getActivityCallbacks()),
                                controller);
                    })
                    .withFragmentScoped(target -> {
                        if (target instanceof BaseBottomDialog) {
                            return emptyList();
                        }
                        BaseFragment fragment = (BaseFragment) target;
                        FragmentCallbacks fragmentCallbacks = new FragmentCallbacks();
                        RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                        if (fragment instanceof HistoryFragment) {
                            historyController = new HistoryController((BaseActivity) fragment.getActivity(),
                                    fragment, fragmentCallbacks, historyWorker, timeProvider,
                                    mock(MainActivityFragmentsController.class),
                                    mock(MainActivitySelectedDateStorage.class));
                            return asList(subscriptions, historyController);
                        }
                        return emptyList();
                    })
                    .build();

    @Test
    public void containsFoodstuffsFromBucketList() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("apple").withNutrition(1, 2, 3, 4)).blockingGet();
        Foodstuff f2 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("water").withNutrition(1, 2, 3, 4)).blockingGet();
        Foodstuff f3 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("beer").withNutrition(1, 2, 3, 4)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 123, ""));
        ingredients.add(Ingredient.create(f2, 123, ""));
        ingredients.add(Ingredient.create(f3, 123, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("apple")).check(matches(isDisplayed()));
        onView(withText("water")).check(matches(isDisplayed()));
        onView(withText("beer")).check(matches(isDisplayed()));
    }

    @Test
    public void usesWeightAndNameFromBucketList() {
        mainThreadExecutor.execute(() -> {
            bucketList.setTotalWeight(123f);
            bucketList.setName("Banana");
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("123")));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText("Banana")));
    }

    @Test
    public void savesRecipeFoodstuffToFoodstuffsList() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32)).blockingGet();
        Foodstuff f2 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("oil").withNutrition(0, 99.9, 0, 899)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 310, ""));
        ingredients.add(Ingredient.create(f2, 13, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("323"));
        String dishName = "carrot with oil";
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText(dishName), closeSoftKeyboard());
        onView(withId(R.id.save_as_recipe_button)).perform(click());

        List<Foodstuff> foodstuffs = foodstuffsList
                .getAllFoodstuffs()
                .filter(foodstuff -> foodstuff.getName().equals(dishName))
                .toList()
                .blockingGet();

        Assert.assertEquals(1, foodstuffs.size());
    }

    @Test
    public void setsActivityResultWhenSavesCreatedRecipe() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 310, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("323"));
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("new super carrot"), closeSoftKeyboard());
        onView(withId(R.id.save_as_recipe_button)).perform(click());

        Intent resultIntent = activityRule.getActivityResult().getResultData();
        Recipe resultRecipe = resultIntent.getParcelableExtra(EXTRA_PRODUCED_RECIPE);
        Assert.assertEquals("new super carrot", resultRecipe.getFoodstuff().getName());
    }

    @Test
    public void setsActivityResultAsCanceled_whenUserEditsRecipeSavesAndExits() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("323"));
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("new super cake"), closeSoftKeyboard());
        onView(withId(R.id.save_as_recipe_button)).perform(click());

        onView(withId(R.id.button_close)).perform(click());

        assertEquals(Activity.RESULT_CANCELED, activityRule.getActivityResult().getResultCode());
    }

    @Test
    public void setsActivityResultAsCanceled_whenUserCancelsRecipeCreation() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 310, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.button_close)).perform(click());
        onView(withId(R.id.positive_button)).perform(click()); // Yes, close

        assertEquals(Activity.RESULT_CANCELED, activityRule.getActivityResult().getResultCode());
    }

    @Test
    public void changingFoodstuffWeightChangesBucketList() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 310, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("carrot")).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.button1)).perform(click());

        mainThreadExecutor.execute(() -> {
            Assert.assertEquals(1, bucketList.getList().size());
            Assert.assertTrue(FloatUtils.areFloatsEquals(10, bucketList.getList().get(0).getWeight()));
        });
    }

    @Test
    public void changingNameChangesBucketList() {
        mainThreadExecutor.execute(() -> {
            bucketList.setName("original name");
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("new name"));

        mainThreadExecutor.execute(() -> {
            Assert.assertEquals("new name", bucketList.getName());
        });
    }

    @Test
    public void updatesBucketListTotalWeight_whenUserEditsIt() {
        mainThreadExecutor.execute(() -> {
            bucketList.setTotalWeight(123f);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("321"));

        mainThreadExecutor.execute(() -> {
            Assert.assertEquals(321f, bucketList.getTotalWeight(), 0.001f);
        });
    }

    @Test
    public void updatesBucketListTotalWeight_whenUserRemovesIngredient() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32)).blockingGet();
        Foodstuff f2 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("oil").withNutrition(0, 99.9, 0, 899)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 10, ""));
        ingredients.add(Ingredient.create(f2, 10, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
            bucketList.setTotalWeight(20);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        // Deleting product
        onView(withText("carrot")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());

        mainThreadExecutor.execute(() -> {
            // The left product has a weight of 10
            Assert.assertEquals(10f, bucketList.getTotalWeight(), 0.001);
        });
    }

    @Test
    public void updatesBucketListTotalWeight_whenUserChangesIngredientWeight() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 123, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
            bucketList.setTotalWeight(123f);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("carrot")).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.button1)).perform(click());

        mainThreadExecutor.execute(() -> {
            Assert.assertEquals(10f, bucketList.getTotalWeight(), 0.001f);
        });
    }

    @Test
    public void nutritionUpdates_whenTotalWeightIsEdited() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("carrot").withNutrition(10, 1, 1, 1)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 100, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
            bucketList.setTotalWeight(100f);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("10")));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("50"));
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("20")));
    }

    @Test
    public void invalidNutritionIsAlteredToValid() {
        Foodstuff f1 =
                foodstuffsList.saveFoodstuff(
                        Foodstuff.withName("potato").withNutrition(10, 10, 10, 1)).blockingGet();

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(f1, 100, ""));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
            bucketList.setTotalWeight(100f);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        // Initial weight
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("100")));
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("10")));

        // Dividing total weight by 2 gives the resulted recipe
        // nutrition: 20 + 20 + 20 = 60, which is valid
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("50"));
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("20")));

        // Nutrition would be 40 + 40 + 40 = 120 if the activity didn't alter it, but it's not
        // a valid nutrition since 100 grams of a product cannot have 120 grams of nutrition.
        // Thus, the activity is expected to alter the nutrition to make the nutrition fit
        // the 100 grams limit.
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("25"));
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("33.3")));
    }

    @Test
    public void editRecipe() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);

        // Verify valid values
        verifyRecipeDisplayingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        // Edit
        onView(withId(R.id.button_edit)).perform(click());

        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"));
        onView(withId(R.id.button1)).perform(click());
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.save_as_recipe_button)).perform(click());

        // Validate updated recipe
        List<Recipe> allRecipes = new ArrayList<>(recipesRepository.getAllRecipesRx().blockingGet());
        assertEquals(1, allRecipes.size());
        Recipe updatedRecipe = allRecipes.get(0);

        assertEquals("cake without oil", updatedRecipe.getFoodstuff().getName());
        assertEquals(10, updatedRecipe.getWeight(), 0.001f);
        assertEquals(1, updatedRecipe.getIngredients().size());
        assertEquals("dough", updatedRecipe.getIngredients().get(0).getFoodstuff().getName());
        assertEquals(3f, updatedRecipe.getIngredients().get(0).getWeight(), 0.001f);

        // Validate updated recipe UI
        verifyRecipeDisplayingState(
                "cake without oil", "10",
                asList(Pair.create("dough", 3)),
                asList(Pair.create("oil", 222)));
    }

    @Test
    public void recipeModificationStateRestore() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);

        // Edit
        onView(withId(R.id.button_edit)).perform(click());
        verifyRecipeEditingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"));
        onView(withId(R.id.button1)).perform(click());
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"));

        mainThreadExecutor.execute(() -> activityRule.getActivity().recreate());

        verifyRecipeEditingState(
                "cake without oil", "10",
                asList(Pair.create("dough", 3)),
                asList(Pair.create("oil", 222)));
    }

    @Test
    public void recipeCreationStateRestore() {
        // Start creating recipe (put not saved recipe into bucket list)
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> {
            bucketList.setRecipe(recipe);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        // Verify displayed data, verify creating state, edit the recipe
        verifyRecipeCreatingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"));
        onView(withId(R.id.button1)).perform(click());
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"));

        mainThreadExecutor.execute(() -> activityRule.getActivity().recreate());

        verifyRecipeCreatingState(
                "cake without oil", "10",
                asList(Pair.create("dough", 3)),
                asList(Pair.create("oil", 222)));
    }

    @Test
    public void recipeDisplayingStateRestore() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);
        verifyRecipeDisplayingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        // Edit and save
        onView(withId(R.id.button_edit)).perform(click());
        verifyRecipeEditingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"));
        onView(withId(R.id.button1)).perform(click());
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"));

        onView(withId(R.id.save_as_recipe_button)).perform(click());

        mainThreadExecutor.execute(() -> activityRule.getActivity().recreate());

        verifyRecipeDisplayingState(
                "cake without oil", "10",
                asList(Pair.create("dough", 3)),
                asList(Pair.create("oil", 222)));
    }

    @Test
    public void ingredientsClicksInDisplayAndEditRecipeModes() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);
        verifyRecipeDisplayingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        // Display mode
        onView(withText("dough")).perform(click());
        onView(withId(R.id.foodstuff_card_layout)).check(isNotDisplayed());
        onView(withText("dough")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).check(isNotDisplayed());

        // Edit mode
        onView(withId(R.id.button_edit)).perform(click());
        onView(withText("dough")).perform(click());
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.button_close)).perform(click());
        onView(withText("dough")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).check(matches(isDisplayed()));
    }

    @Test
    public void switchingBetweenDisplayAndEditStates_withRecipeEditing() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);
        verifyRecipeDisplayingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        // Switch states without editing recipe
        onView(withId(R.id.button_edit)).perform(click());
        verifyRecipeEditingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());
        onView(withId(R.id.button_close)).perform(click());
        verifyRecipeDisplayingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        // Switch states with editing recipe
        onView(withId(R.id.button_edit)).perform(click());

        verifyRecipeEditingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"));
        onView(withId(R.id.button1)).perform(click());
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"));

        verifyRecipeEditingState(
                "cake without oil", "10",
                asList(Pair.create("dough", 3)),
                asList(Pair.create("oil", 222)));

        onView(withId(R.id.save_as_recipe_button)).perform(click());
        verifyRecipeDisplayingState(
                "cake without oil", "10",
                asList(Pair.create("dough", 3)),
                asList(Pair.create("oil", 222)));
    }

    @Test
    public void cancelRecipeEditingByBackPress() {
        cancelRecipeEditing(true);
    }

    @Test
    public void cancelRecipeEditingByButtonCloseClick() {
        cancelRecipeEditing(false);
    }

    private void cancelRecipeEditing(boolean byBackPress) {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);
        verifyRecipeDisplayingState("cake", "123", emptyList(), emptyList());

        // Edit, try cancel, but don't cancel
        onView(withId(R.id.button_edit)).perform(click());
        verifyRecipeEditingState("cake", "123", emptyList(), emptyList());
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("novel cake"));
        if (byBackPress) {
            Espresso.pressBack();
        } else {
            onView(withId(R.id.button_close)).perform(click());
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_editing_dialog_title)
        )).perform(click());
        onView(withId(R.id.negative_button)).perform(click());
        verifyRecipeEditingState("novel cake", "123", emptyList(), emptyList());

        // This time - confirm cancellation
        if (byBackPress) {
            Espresso.pressBack();
        } else {
            onView(withId(R.id.button_close)).perform(click());
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_editing_dialog_title)
        )).perform(click());
        onView(withId(R.id.positive_button)).perform(click());
        verifyRecipeDisplayingState("cake", "123", emptyList(), emptyList());

        // Verify that the recipe is not changed
        Set<Recipe> allRecipes = recipesRepository.getAllRecipesRx().blockingGet();
        assertEquals(1, allRecipes.size());
        assertEquals("cake", allRecipes.iterator().next().getFoodstuff().getName());
        assertEquals(recipe, allRecipes.iterator().next());
    }

    @Test
    public void cancelRecipeCreationByBackPress() {
        cancelRecipeCreation(true);
    }

    @Test
    public void cancelRecipeCreationByButtonCloseClick() {
        cancelRecipeCreation(false);
    }

    private void cancelRecipeCreation(boolean byBackPress) {
        // Start recipe creation
        mainThreadExecutor.execute(() -> {
            bucketList.setName("cake");
            bucketList.setTotalWeight(123);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);
        verifyRecipeCreatingState("cake", "123", emptyList(), emptyList());

        // Edit, try cancel, but don't cancel
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("novel cake"));
        if (byBackPress) {
            Espresso.pressBack();
        } else {
            onView(withId(R.id.button_close)).perform(click());
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_creation_dialog_title)
        )).perform(click());
        onView(withId(R.id.negative_button)).perform(click());
        verifyRecipeCreatingState("novel cake", "123", emptyList(), emptyList());

        // This time - confirm cancellation
        if (byBackPress) {
            Espresso.pressBack();
        } else {
            onView(withId(R.id.button_close)).perform(click());
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_creation_dialog_title)
        )).perform(click());
        mainThreadExecutor.execute(() -> assertFalse(activityRule.getActivity().isFinishing()));
        onView(withId(R.id.positive_button)).perform(click());
        mainThreadExecutor.execute(() -> assertTrue(activityRule.getActivity().isFinishing()));

        // Verify that the recipe was not created and bucket list is cleaned
        Set<Recipe> allRecipes = recipesRepository.getAllRecipesRx().blockingGet();
        assertEquals(0, allRecipes.size());
        assertEquals("", bucketList.getName());
        assertEquals(0f, bucketList.getTotalWeight(), 0.0001f);
    }

    @Test
    public void cancelRecipeEditingWithoutRecipeModification_byCloseButton() {
        cancelRecipeEditingWithoutRecipeModification(false);
    }

    private void cancelRecipeEditingWithoutRecipeModification(boolean byBackPress) {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);
        verifyRecipeDisplayingState("cake", "123", emptyList(), emptyList());

        onView(withId(R.id.button_edit)).perform(click());
        verifyRecipeEditingState("cake", "123", emptyList(), emptyList());
        mainThreadExecutor.execute(() -> assertEquals(recipe, bucketList.getRecipe()));

        if (byBackPress) {
            onView(withId(R.id.button_close)).perform(click());
        } else {
            Espresso.pressBack();
        }
        onView(withId(R.id.two_options_dialog_layout)).check(isNotDisplayed());
        verifyRecipeDisplayingState("cake", "123", emptyList(), emptyList());
        // BucketList expected to be cleaned
        mainThreadExecutor.execute(() -> assertNotEquals(recipe, bucketList.getRecipe()));
        mainThreadExecutor.execute(() -> assertEquals("", bucketList.getName()));
    }

    @Test
    public void cancelRecipeEditingWithoutRecipeModification_byBackPress() {
        cancelRecipeEditingWithoutRecipeModification(true);
    }

    @Test
    public void cancelRecipeCreationWithoutRecipeModification_byCloseButton() {
        cancelRecipeCreationWithoutRecipeModification(false);
    }

    private void cancelRecipeCreationWithoutRecipeModification(boolean byBackPress) {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> bucketList.setRecipe(recipe));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);
        verifyRecipeCreatingState("cake", "123", emptyList(), emptyList());
        mainThreadExecutor.execute(() -> assertEquals(recipe, bucketList.getRecipe()));

        if (byBackPress) {
            onView(withId(R.id.button_close)).perform(click());
        } else {
            Espresso.pressBack();
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.positive_button)).perform(click());

        // BucketList expected to be cleaned
        mainThreadExecutor.execute(() -> assertNotEquals(recipe, bucketList.getRecipe()));
        mainThreadExecutor.execute(() -> assertEquals("", bucketList.getName()));
        // Activtiy expected to be finished
        assertEquals(Activity.RESULT_CANCELED, activityRule.getActivityResult().getResultCode());
    }

    @Test
    public void cancelRecipeCreationWithoutRecipeModification_byBackPress() {
        cancelRecipeCreationWithoutRecipeModification(true);
    }

    @Test
    public void openWithEditedExistingRecipeInBucketList() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> bucketList.setRecipe(recipe));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        verifyRecipeEditingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());
    }

    @Test
    public void cancelRecipeEditing_whenOpenedWithAlreadyChangedEditedRecipe() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Recipe changedRecipe = new Recipe(
                recipe.getId(),
                recipe.getFoodstuff().recreateWithName("novel cake"),
                recipe.getIngredients(),
                123,
                recipe.getComment());
        mainThreadExecutor.execute(() -> bucketList.setRecipe(changedRecipe));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        verifyRecipeEditingState(
                "novel cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withId(R.id.button_close)).perform(click());
        // Cancellation dialog is expected
        onView(withText(R.string.cancel_recipe_editing_dialog_title)).check(matches(isDisplayed()));
        onView(withId(R.id.positive_button)).perform(click());

        verifyRecipeDisplayingState(
                "cake", "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());
    }

    @Test
    public void addIngredientButtonBehaviour_onRecipeCreation() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe notSavedRecipe = createRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> {
            bucketList.setRecipe(notSavedRecipe);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.bucket_list_add_ingredient_button)).perform(click());
        // Button closes activity
        assertEquals(Activity.RESULT_CANCELED, activityRule.getActivityResult().getResultCode());
        // But doesn't clean BucketList
        mainThreadExecutor.execute(() -> assertEquals(notSavedRecipe, bucketList.getRecipe()));
    }

    @Test
    public void addIngredientButtonBehaviour_onRecipeEditing() {
        // Create recipe
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe recipe = createSavedRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);
        onView(withId(R.id.button_edit)).perform(click());

        onView(withId(R.id.bucket_list_add_ingredient_button)).perform(click());
        // Button closes activity
        assertEquals(Activity.RESULT_CANCELED, activityRule.getActivityResult().getResultCode());
        // But doesn't clean BucketList
        mainThreadExecutor.execute(() -> assertEquals(recipe, bucketList.getRecipe()));
    }

    @Test
    public void saveAsRecipeButtonEnabledAndDisabledStates() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe notSavedRecipe = createRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> {
            bucketList.setRecipe(notSavedRecipe);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        verifyRecipeCreatingState(
                "cake",
                "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()));

        // Name
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText(""));
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("novel cake"));
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()));

        // Weight
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText(""));
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("321"));
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()));

        // Ingredients
        onView(withText("dough")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()));
        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void saveAsRecipeButtonEnabled_whenBucketListOpenedWithFilledCreatingRecipe() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe notSavedRecipe = createRecipe(
                "cake", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> bucketList.setRecipe(notSavedRecipe));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        verifyRecipeCreatingState(
                "cake",
                "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()));
    }

    @Test
    public void saveAsRecipeButtonNotEnabled_whenBucketListOpenedWithCreatingRecipe_withoutName() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe notSavedRecipe = createRecipe(
                "", 123,
                asList(Pair.create("dough", 111), Pair.create("oil", 222)));
        mainThreadExecutor.execute(() -> bucketList.setRecipe(notSavedRecipe));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        verifyRecipeCreatingState(
                "",
                "123",
                asList(Pair.create("dough", 111), Pair.create("oil", 222)),
                emptyList());

        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void saveAsRecipeButtonNotEnabled_whenBucketListOpenedWithCreatingRecipe_withoutIngredients() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
        Recipe notSavedRecipe = createRecipe("cake", 123, emptyList());
        mainThreadExecutor.execute(() -> bucketList.setRecipe(notSavedRecipe));

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        verifyRecipeCreatingState(
                "cake",
                "123",
                emptyList(),
                emptyList());

        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())));
    }

    private Recipe createSavedRecipe(
            String name,
            Integer weight,
            List<Pair<String, Integer>> ingredientsNamesAndWeights) {
        Recipe notSavedRecipe = createRecipe(name, weight, ingredientsNamesAndWeights);
        CreateRecipeResult recipeResult = recipesRepository.saveRecipeRx(notSavedRecipe).blockingGet();
        return ((CreateRecipeResult.Ok)recipeResult).getRecipe();
    }

    private Recipe createRecipe(
            String name,
            Integer weight,
            List<Pair<String, Integer>> ingredientsNamesAndWeights) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (Pair<String, Integer> ingredientPair : ingredientsNamesAndWeights) {
            Foodstuff ingredient = Foodstuff.withName(ingredientPair.first).withNutrition(1, 2, 3, 4);
            ingredient = foodstuffsList.saveFoodstuff(ingredient).blockingGet();
            ingredients.add(Ingredient.create(ingredient, ingredientPair.second, "comment"));
        }

        Foodstuff foodstuff = Foodstuff.withName(name).withNutrition(1, 2, 3, 4);
        return Recipe.create(foodstuff, ingredients, weight, "comment");
    }


    private void verifyRecipeDisplayingState(
            String recipeName,
            String weightText,
            List<Pair<String, Integer>> expectedIngredients,
            List<Pair<String, Integer>> notExpectedIngredients) {
        onView(withId(R.id.button_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe)));
        onView(withId(R.id.save_as_recipe_button)).check(isNotDisplayed());
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipeName)));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(not(isEnabled())));
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(weightText)));
        onView(withId(R.id.total_weight_edit_text)).check(matches(not(isEnabled())));
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(isNotDisplayed());

        verifyDisplayedIngredients(expectedIngredients, notExpectedIngredients);
    }

    private void verifyDisplayedIngredients(
            List<Pair<String, Integer>> expectedIngredients,
            List<Pair<String, Integer>> notExpectedIngredients) {
        for (Pair<String, Integer> ingredient : expectedIngredients) {
            String expectedGramsText = activityRule.getActivity().getString(
                    R.string.n_gramms,
                    ingredient.second);
            onView(allOf(
                    withParent(isDescendantOfA(withId(R.id.ingredients_list))),
                    hasDescendant(withText(ingredient.first)),
                    hasDescendant(withText(expectedGramsText))))
                    .check(matches(isDisplayed()));
        }

        for (Pair<String, Integer> ingredient : notExpectedIngredients) {
            String expectedGramsText = activityRule.getActivity().getString(
                    R.string.n_gramms,
                    ingredient.second);
            onView(allOf(
                    withParent(isDescendantOfA(withId(R.id.ingredients_list))),
                    hasDescendant(withText(ingredient.first)),
                    hasDescendant(withText(expectedGramsText))))
                    .check(isNotDisplayed());
        }
    }

    private void verifyRecipeEditingState(
            String recipeName,
            String weightText,
            List<Pair<String, Integer>> expectedIngredients,
            List<Pair<String, Integer>> notExpectedIngredients) {
        onView(withId(R.id.button_edit)).check(isNotDisplayed());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe_modification)));
        onView(withId(R.id.save_as_recipe_button)).check(matches(isDisplayed()));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipeName)));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(isEnabled()));
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(weightText)));
        onView(withId(R.id.total_weight_edit_text)).check(matches(isEnabled()));
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(matches(isDisplayed()));

        verifyDisplayedIngredients(expectedIngredients, notExpectedIngredients);
    }

    private void verifyRecipeCreatingState(
            String recipeName,
            String weightText,
            List<Pair<String, Integer>> expectedIngredients,
            List<Pair<String, Integer>> notExpectedIngredients) {
        onView(withId(R.id.button_edit)).check(isNotDisplayed());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe_creation)));
        onView(withId(R.id.save_as_recipe_button)).check(matches(isDisplayed()));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipeName)));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(isEnabled()));
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(weightText)));
        onView(withId(R.id.total_weight_edit_text)).check(matches(isEnabled()));
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(matches(isDisplayed()));

        verifyDisplayedIngredients(expectedIngredients, notExpectedIngredients);
    }
}
