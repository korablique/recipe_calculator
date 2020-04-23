package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
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

                        return Arrays.asList(mainThreadExecutor, databaseThreadExecutor, databaseWorker,
                                historyWorker, userParametersWorker, foodstuffsList, userNameProvider,
                                timeProvider, currentActivityProvider, bucketList,
                                new CalcKeyboardController(), recipesRepository);
                    })
                    .withActivityScoped((target) -> {
                        if (target instanceof BucketListActivity) {
                            return Collections.emptyList();
                        }
                        MainActivity activity = (MainActivity) target;
                        ActivityCallbacks activityCallbacks = new ActivityCallbacks();
                        MainActivityController controller = new MainActivityController(activity,
                                activityCallbacks,
                                mock(MainActivityFragmentsController.class));
                        return Arrays.asList(new RxActivitySubscriptions(activity.getActivityCallbacks()),
                                controller);
                    })
                    .withFragmentScoped(target -> {
                        if (target instanceof BaseBottomDialog) {
                            return Collections.emptyList();
                        }
                        BaseFragment fragment = (BaseFragment) target;
                        FragmentCallbacks fragmentCallbacks = new FragmentCallbacks();
                        RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                        if (fragment instanceof HistoryFragment) {
                            historyController = new HistoryController((BaseActivity) fragment.getActivity(),
                                    fragment, fragmentCallbacks, historyWorker, timeProvider,
                                    mock(MainActivityFragmentsController.class),
                                    mock(MainActivitySelectedDateStorage.class));
                            return Arrays.asList(subscriptions, historyController);
                        }
                        return Collections.emptyList();
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
    public void savesDishToFoodstuffsList() {
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
    public void setsActivityResultWhenSavesDish() {
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
        Recipe resultRecipe = resultIntent.getParcelableExtra(BucketListActivity.EXTRA_PRODUCED_RECIPE);
        Assert.assertEquals("new super carrot", resultRecipe.getFoodstuff().getName());
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
        Foodstuff ingredientFoodstuff1 = Foodstuff.withName("dough").withNutrition(1, 2, 3, 4);
        Foodstuff ingredientFoodstuff2 = Foodstuff.withName("oil").withNutrition(5, 6, 7, 8);
        ingredientFoodstuff1 = foodstuffsList.saveFoodstuff(ingredientFoodstuff1).blockingGet();
        ingredientFoodstuff2 = foodstuffsList.saveFoodstuff(ingredientFoodstuff2).blockingGet();
        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(ingredientFoodstuff1, 111, "comment1"));
        ingredients.add(Ingredient.create(ingredientFoodstuff2, 222, "comment2"));

        Foodstuff foodstuff = Foodstuff.withName("cake").withNutrition(1, 2, 3, 4);
        Recipe recipe = Recipe.create(foodstuff, ingredients, 123f, "comment");
        CreateRecipeResult recipeResult = recipesRepository.saveRecipeRx(recipe).blockingGet();
        recipe = ((CreateRecipeResult.Ok)recipeResult).getRecipe();

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext(),
                        recipe);
        activityRule.launchActivity(startIntent);

        // Verify valid values
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe)));
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText("cake")));
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("123")));

        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.ingredients_list))),
                withText("dough")
        )).check(matches(isDisplayed()));
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.ingredients_list))),
                withText("oil")
        )).check(matches(isDisplayed()));

        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).check(matches(withText("111")));
        onView(withId(R.id.button_close)).perform(click());

        onView(withText("oil")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).check(matches(withText("222")));
        onView(withId(R.id.button_close)).perform(click());

        // Edit
        onView(withText("oil")).perform(longClick());
        onView(withText(R.string.delete_ingredient)).perform(click());
        onView(withText("dough")).perform(click());
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("1"));
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
        assertEquals(1f, updatedRecipe.getIngredients().get(0).getWeight(), 0.001f);
    }
}
