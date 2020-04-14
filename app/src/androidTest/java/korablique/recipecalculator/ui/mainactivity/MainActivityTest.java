package korablique.recipecalculator.ui.mainactivity;

import android.app.Instrumentation;
import android.view.View;

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
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
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
