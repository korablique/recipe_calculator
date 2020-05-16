package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Recipe;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.util.Collections.emptyList;
import static junit.framework.Assert.assertEquals;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityBucketListTest extends MainActivityTestsBase {
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
        onView(withId(R.id.button2)).perform(click());

        onView(allOf(
                withText(clickedFoodstuffs.get(1).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        onView(allOf(
                withText(clickedFoodstuffs.get(2).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        // Кликаем на корзинку в снэкбаре
        onView(withId(R.id.basket)).perform(click());

        List<WeightedFoodstuff> clickedWeightedFoodstuffs = new ArrayList<>();
        for (Foodstuff foodstuff : clickedFoodstuffs) {
            clickedWeightedFoodstuffs.add(foodstuff.withWeight(123));
        }

        // Проверяем, что была попытка стартовать активити по интенту от BucketListActivity,
        // также что BucketList содержит все необходимые продукты.
        Intent expectedIntent =
                BucketListActivity.createIntent(mActivityRule.getActivity());
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasComponent(expectedIntent.getComponent())));

        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(Ingredient.create(clickedFoodstuffs.get(0), 123, ""));
        ingredients.add(Ingredient.create(clickedFoodstuffs.get(1), 123, ""));
        ingredients.add(Ingredient.create(clickedFoodstuffs.get(2), 123, ""));
        mainThreadExecutor.execute(() -> {
            assertEquals(ingredients, bucketList.getList());
        });
    }

    @Test
    public void showsCardWhenBucketListActivityCreatesFoodstuff() {
        mActivityRule.launchActivity(null);

        // Проверяем, что сперва карточка не показана
        onView(withId(R.id.foodstuff_card_layout)).check(doesNotExist());

        // Создаём продукт и сообщаем фрагментам, что его создал бакетлист
        Foodstuff foodstuff = Foodstuff
                .withName("new_foodstuff_with_new_name")
                .withNutrition(1, 2, 3, 4);
        List<Fragment> fragments = mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            f.onActivityResult(RequestCodes.MAIN_SCREEN_BUCKET_LIST_CREATE_FOODSTUFF,
                    Activity.RESULT_OK,
                    BucketListActivity.createRecipeResultIntent(
                            Recipe.create(foodstuff, emptyList(), 123f, "")));
        }
        // Убеждаемся, что показана карточка с новым продуктом
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withText(containsString("new_foodstuff_with_new_name"))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void mainScreenFoodstuffCard_hidesDirectAdditionToHistory_whenBucketListNotEmpty() {
        mActivityRule.launchActivity(null);

        // Клик на продукт
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());

        // Обе кнопки в карточке должны быть видны
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1))).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));

        // Жмём на кнопку создания блюда
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        // Снова клик на продукт
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());

        // Только кнопка добавления блюда должна быть видна
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1))).check(matches(not(isDisplayed())));

        // Закрываем карточку
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button_close))).perform(click());

        // Очищаем бакетлист
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });

        // Снова клик на продукт
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());

        // Обе кнопки в карточке снова должны быть видны
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1))).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));
    }

    @Test
    public void changesTotalIngredientsWeight_whenIngredientAddedToBucketList() {
        mActivityRule.launchActivity(null);

        mainThreadExecutor.execute(() -> {
            assertEquals(0.f, bucketList.getTotalWeight(), 0.0001f);
        });

        // Добавим продукт 1
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));
        onView(withId(R.id.weight_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.button2)).perform(click());

        // Добавим продукт 2
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));
        onView(withId(R.id.weight_edit_text)).perform(replaceText("20"));
        onView(withId(R.id.button2)).perform(click());

        mainThreadExecutor.execute(() -> {
            assertEquals(30f, bucketList.getTotalWeight(), 0.0001f);
        });
    }

    private List<Foodstuff> extractFoodstuffsTopFromDB() {
        return topList.getMonthTop().blockingGet();
    }
}
