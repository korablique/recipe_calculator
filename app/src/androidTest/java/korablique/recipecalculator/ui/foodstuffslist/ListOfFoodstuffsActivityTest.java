package korablique.recipecalculator.ui.foodstuffslist;

import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.Card;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ListOfFoodstuffsActivityTest {
    private DatabaseWorker databaseWorker;

    @Rule
    public ActivityTestRule<ListOfFoodstuffsActivity> mActivityRule =
            new ActivityTestRule<>(ListOfFoodstuffsActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        databaseWorker = new DatabaseWorker();
        Card.setAnimationDuration(0);
        Foodstuff foodstuff1 = new Foodstuff("product1", -1, 10, 10, 10, 10);
        final CountDownLatch mutex = new CountDownLatch(1);
        databaseWorker.saveFoodstuff(
                mActivityRule.getActivity(),
                foodstuff1,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                mutex.countDown();
            }
            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        mutex.await();

        final CountDownLatch mutex2 = new CountDownLatch(1);
        mActivityRule.getActivity().reload(new Runnable() {
            @Override
            public void run() {
                mutex2.countDown();
            }
        });
        mutex2.await();
    }

    @Test
    public void canSaveProductWithoutEditing() {
        onView(ViewMatchers.withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
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
