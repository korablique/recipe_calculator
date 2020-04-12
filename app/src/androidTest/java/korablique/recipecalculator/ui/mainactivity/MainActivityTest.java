package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Nutrition;
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
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static java.util.Collections.emptyList;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest extends MainActivityTestsBase {
    @Test
    public void topAndAllFoodstuffsHeadersDisplayedIfHistoryIsNotEmpty() {
        mActivityRule.launchActivity(null);
        assertContains(mActivityRule.getActivity().getString(R.string.top_header));
        assertContains(mActivityRule.getActivity().getString(R.string.all_foodstuffs_header));
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
            Assert.assertEquals(ingredients, bucketList.getList());
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
                    BucketListActivity.createFoodstuffResultIntent(
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
    public void editedFoodstuffReplacesInAllFoodstuffs() {
        mActivityRule.launchActivity(null);
        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        Matcher<View> topMatcher = allOf(
                withText(topFoodstuffs.get(1).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).perform(click());
        onView(withId(R.id.button_edit)).perform(click());

        // Редактируем
        String newName = topFoodstuffs.get(1).getName() + "1";
        onView(withId(R.id.foodstuff_name)).perform(replaceText(newName));
        onView(withId(R.id.save_button)).perform(click());

        // Закрываем карточку
        onView(withId(R.id.button_close)).perform(click());

        // Проверяем отредактированное
        Matcher<View> allFoodstuffsMatcher = allOf(
                withText(newName),
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
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());
        onView(withId(R.id.positive_button)).perform(click());
        onView(withText(deletingFoodstuff.getName())).check(doesNotExist());

        List<Foodstuff> foodstuffsListAfterDeleting = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(100, foodstuffs -> {
            foodstuffsListAfterDeleting.addAll(foodstuffs);
        });
        Assert.assertFalse(foodstuffsListAfterDeleting.contains(deletingFoodstuff));

        // Recreate activity and check again
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(deletingFoodstuff.getName())).check(doesNotExist());
    }


    @Test
    public void foodstuffDeletionCancellationWorks() {
        mActivityRule.launchActivity(null);

        Foodstuff deletingFoodstuff = Foodstuff
                .withId(foodstuffsIds.get(0))
                .withName(foodstuffs[0].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]));
        onView(allOf(
                withText(deletingFoodstuff.getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());

        // Cancel deletion!
        onView(withId(R.id.negative_button)).perform(click());
        // Return to main screen
        onView(isRoot()).perform(ViewActions.pressBack());

        onView(withText(deletingFoodstuff.getName())).check(matches(isDisplayed()));

        List<Foodstuff> foodstuffsListAfterDeleting = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(100, foodstuffs -> {
            foodstuffsListAfterDeleting.addAll(foodstuffs);
        });
        Assert.assertTrue(foodstuffsListAfterDeleting.contains(deletingFoodstuff));

        // Recreate activity and check again
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(deletingFoodstuff.getName())).check(matches(isDisplayed()));
    }

    @Test
    public void onBackPressedInMainScreen() {
        mActivityRule.launchActivity(null);

        // главная разметка есть на экране
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));
        // бэк
        Espresso.pressBackUnconditionally();
        // мы мертвы
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void onBackPressedInHistory() {
        mActivityRule.launchActivity(null);
        // история
        onView(withId(R.id.menu_item_history)).perform(click());
        onView(withId(R.id.fragment_history)).check(matches(isDisplayed()));

        // бэк должен вернуть мейн-скрин
        Espresso.pressBackUnconditionally();
        onView(withId(R.id.fragment_history)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));

        // бэк и мы мертвы
        Espresso.pressBackUnconditionally();
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void onBackPressedInProfile() {
        mActivityRule.launchActivity(null);
        // профиль
        onView(withId(R.id.menu_item_profile)).perform(click());
        onView(withId(R.id.fragment_profile)).check(matches(isDisplayed()));

        // бэк должен вернуть мейн-скрин
        Espresso.pressBackUnconditionally();
        onView(withId(R.id.fragment_profile)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));

        // бэк и мы мертвы
        Espresso.pressBackUnconditionally();
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void onBackPressedIn_afterMultipleFragmentSwitches() {
        mActivityRule.launchActivity(null);
        // меняем несколько раз фрагменты туда-сюда
        for (int i = 0; i < 2; ++i) {
            onView(withId(R.id.menu_item_profile)).perform(click());
            onView(withId(R.id.menu_item_foodstuffs)).perform(click());
            onView(withId(R.id.menu_item_history)).perform(click());
        }
        // последней открывали историю
        onView(withId(R.id.menu_item_history)).check(matches(isDisplayed()));

        // бэк должен вернуть мейн-скрин
        Espresso.pressBackUnconditionally();
        onView(withId(R.id.fragment_history)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));

        // бэк и мы мертвы
        Espresso.pressBackUnconditionally();
        assertTrue(mActivityRule.getActivity().isDestroyed());
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
    public void mainScreenDisplaysCard_afterFoodstuffEditing() {
        mActivityRule.launchActivity(null);
        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        Matcher<View> topMatcher = allOf(
                withText(topFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).perform(click());
        onView(withId(R.id.button_edit)).perform(click());

        String newName = topFoodstuffs.get(0).getName() + "1";
        onView(withId(R.id.foodstuff_name)).perform(replaceText(newName));
        onView(withId(R.id.save_button)).perform(click());

        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
    }

    @Test
    public void mainScreenDisplaysCard_afterFoodstuffCreation() {
        mActivityRule.launchActivity(null);

        String name = "111first_foodstuff";
        onView(withText(name)).check(doesNotExist());

        onView(withId(R.id.add_new_foodstuff)).perform(click());

        onView(withId(R.id.foodstuff_name)).perform(replaceText(name));
        onView(withId(R.id.protein_value)).perform(replaceText("10"));
        onView(withId(R.id.fats_value)).perform(replaceText("10"));
        onView(withId(R.id.carbs_value)).perform(replaceText("10"));
        onView(withId(R.id.calories_value)).perform(replaceText("10"));
        onView(withId(R.id.save_button)).perform(click());

        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withText(name))).check(matches(isDisplayed()));
    }

    private List<Foodstuff> extractFoodstuffsTopFromDB() {
        return topList.getMonthTop().blockingGet();
    }
}
