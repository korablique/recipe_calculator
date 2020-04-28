package korablique.recipecalculator.ui.mainactivity;

import android.app.Instrumentation;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.CreateRecipeResult;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Recipe;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityMainScreenSnackbarTest extends MainActivityTestsBase {
    @Test
    public void snackbarUpdatesAfterChangingBucketList() {
        // добавляем в bucket list продукты, запускаем активити, в снекбаре должно быть 3 фудстаффа
        Ingredient ingr0 = Ingredient.create(foodstuffs[0].withWeight(100), "");
        Ingredient ingr1 = Ingredient.create(foodstuffs[1].withWeight(100), "");
        Ingredient ingr2 = Ingredient.create(foodstuffs[2].withWeight(100), "");

        mainThreadExecutor.execute(() -> {
            bucketList.add(ingr0);
            bucketList.add(ingr1);
            bucketList.add(ingr2);
        });

        mActivityRule.launchActivity(null);
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("3")));

        // убираем один продукт, перезапускаем активити, в снекбаре должно быть 2 фудстаффа
        mainThreadExecutor.execute(() -> {
            bucketList.remove(Ingredient.create(foodstuffs[0].withWeight(100), ""));
        });

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        instrumentation.waitForIdleSync();
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(isDisplayed()));
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("2")));
        Assert.assertTrue(bucketList.getList().contains(ingr1));
        Assert.assertTrue(bucketList.getList().contains(ingr2));

        // убираем все продукты, перезапускаем активити, снекбара быть не должно
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        instrumentation.waitForIdleSync();
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(not(isDisplayed())));
    }

    @Test
    public void swipedOutSnackbar_cleansBucketList() throws InterruptedException {
        // добавляем в bucket list продукты, запускаем активити
        Ingredient ingr0 = Ingredient.create(foodstuffs[0].withWeight(100), "");
        Ingredient ingr1 = Ingredient.create(foodstuffs[1].withWeight(100), "");

        mainThreadExecutor.execute(() -> {
            bucketList.add(ingr0);
            bucketList.add(ingr1);
            assertEquals(2, bucketList.getList().size());
        });

        mActivityRule.launchActivity(null);

        // "Высвайпываем" снекбар
        onView(withId(R.id.snackbar)).perform(swipeRight());
        Thread.sleep(500); // Ждем 0.5с, потому что SwipeDismissBehaviour криво уведомляет о высвайпывании

        // Убеждаемся, что в бакетлисте пусто и снекбар не показан
        onView(withId(R.id.snackbar)).check(matches(not(isDisplayed())));
        mainThreadExecutor.execute(() -> {
            assertTrue(bucketList.getList().isEmpty());
        });
    }

    @Test
    public void swipedOutSnackbar_canBeCanceled() throws InterruptedException {
        // добавляем в bucket list продукты, запускаем активити
        Ingredient ingr0 = Ingredient.create(foodstuffs[0].withWeight(100), "");
        Ingredient ingr1 = Ingredient.create(foodstuffs[1].withWeight(100), "");

        mainThreadExecutor.execute(() -> {
            bucketList.add(ingr0);
            bucketList.add(ingr1);
            assertEquals(2, bucketList.getList().size());
        });

        mActivityRule.launchActivity(null);

        // "Высвайпываем" снекбар
        onView(withId(R.id.snackbar)).perform(swipeRight());
        Thread.sleep(500); // Ждем 0.5с, потому что SwipeDismissBehaviour криво уведомляет о высвайпывании

        // Тут же отменяем "высвайпывание"
        onView(withText(R.string.undo)).perform(click());

        // Убеждаемся, что в бакетлисте по-прежнему есть продукты и снекбар показан
        onView(withId(R.id.snackbar)).check(matches(isDisplayed()));
        mainThreadExecutor.execute(() -> {
            assertEquals(2, bucketList.getList().size());
        });
    }

    @Test
    public void snackbarRecipeCreationText() {
        mActivityRule.launchActivity(null);

        onView(withText(R.string.selected_foodstuffs_snackbar_title_recipe_creation))
                .check(doesNotExist());

        Ingredient ingr = Ingredient.create(foodstuffs[1].withWeight(100), "");
        mainThreadExecutor.execute(() -> bucketList.add(ingr));

        onView(withText(R.string.selected_foodstuffs_snackbar_title_recipe_creation))
                .check(matches(isDisplayed()));
    }

    @Test
    public void snackbarRecipeEditingText() {
        mActivityRule.launchActivity(null);

        onView(withText(R.string.selected_foodstuffs_snackbar_title_recipe_editing))
                .check(doesNotExist());

        Ingredient ingr = Ingredient.create(foodstuffs[1].withWeight(100), "");
        Recipe notSavedRecipe = Recipe.create(
                Foodstuff.withName("Cake").withNutrition(1, 2, 3, 4),
                Collections.singletonList(ingr),
                123,
                "comment");
        CreateRecipeResult recipeResult = recipesRepository.saveRecipeRx(notSavedRecipe).blockingGet();
        Recipe recipe = ((CreateRecipeResult.Ok) recipeResult).getRecipe();
        mainThreadExecutor.execute(() -> bucketList.setRecipe(recipe));

        onView(withText(R.string.selected_foodstuffs_snackbar_title_recipe_editing))
                .check(matches(isDisplayed()));
    }
}
