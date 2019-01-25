package korablique.recipecalculator.ui.foodstuffslist;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseHolder;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ListOfFoodstuffsActivityTest {
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private DatabaseHolder databaseHolder;
    private DatabaseWorker databaseWorker;
    private FoodstuffsList foodstuffsList;
    private Long savedFoodstuffId;

    @Rule
    public ActivityTestRule<ListOfFoodstuffsActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(ListOfFoodstuffsActivity.class)
                    .withManualStart()
                    .withSingletones(() -> {
                        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
                        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, new SyncMainThreadExecutor(), databaseThreadExecutor);
                        foodstuffsList = new FoodstuffsList(context, databaseWorker);
                        return Arrays.asList(databaseWorker, foodstuffsList);
                    })
                    .build();

    @Before
    public void setUp() {
        Card.setAnimationDuration(0);

        Context context = InstrumentationRegistry.getTargetContext();
        Foodstuff foodstuff1 = Foodstuff.withName("product1").withNutrition(10, 10, 10, 10);
        foodstuffsList.saveFoodstuff(
                foodstuff1,
                new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                savedFoodstuffId = id;
            }

            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });

        mActivityRule.launchActivity(null);
    }

    @After
    public void tearDown() {
        // TODO: 22.01.19 что делать c id?
//        databaseWorker.deleteFoodstuff(savedFoodstuffId);
        savedFoodstuffId = null;
    }

    @Test
    public void canSaveProductWithoutEditing() {
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.card)).check(matches(isDisplayed()));
        onView(withId(R.id.button_save)).perform(click());
        onView(withId(R.id.card)).check(matches(not(isDisplayed())));
    }

    @Test
    public void itemIsEditable() {
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        String oldName = ((EditText) mActivityRule.getActivity().findViewById(R.id.name_edit_text)).getText().toString();
        onView(withId(R.id.name_edit_text)).perform(replaceText(oldName + "2"));
        onView(withId(R.id.button_save)).perform(click());
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        String newName = ((EditText) mActivityRule.getActivity().findViewById(R.id.name_edit_text)).getText().toString();
        Assert.assertNotEquals(oldName, newName);
    }

    private Matcher<View> anyWithId(final int id) {
        return new TypeSafeMatcher<View>() {
            private boolean found;
            @Override
            protected boolean matchesSafely(View item) {
                if (found) {
                    return false;
                }
                if (item.getId() == id) {
                    found = true;
                    return true;
                }
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("Couldn't find view with id: " + id);
            }
        };
    }
}
