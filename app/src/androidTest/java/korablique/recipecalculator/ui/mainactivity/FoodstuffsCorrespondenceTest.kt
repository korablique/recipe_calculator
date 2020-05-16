package korablique.recipecalculator.ui.mainactivity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.partners.direct.FoodstuffsCorrespondenceManager
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.util.EspressoUtils.isNotDisplayed
import korablique.recipecalculator.util.EspressoUtils.matches
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FoodstuffsCorrespondenceTest : MainActivityTestsBase() {
    @Before
    override fun setUp() {
        super.setUp()
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "uid",
                    "client_token": "token"
                }
            """))
        }
        fakeHttpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid",
                            "partner_name": "partner name"
                        }
                    ]
                }
            """
            RequestResult.Success(Response(body))
        }
        fakeHttpClient.setResponse(".*direct_partner_msg.*") {
            val body = """{ "status": "ok"}"""
            RequestResult.Success(Response(body))
        }
        mActivityRule.launchActivity(null)
    }

    private fun auth() {
        // Interactive params obtainer will register through GP
        GlobalScope.launch(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }
    }

    @Test
    fun canSendFoodstuff() {
        auth()

        assertEquals(0, fakeHttpClient.getRequestsMatching(".*direct_partner_msg.*").size)

        onView(allOf(
                withText(foodstuffs[0].name),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))
        ).perform(longClick())
        onView(withText(R.string.send_to_partner)).perform(click())

        onView(withId(R.id.partners_list_fragment)).check(matches(isDisplayed()))
        onView(withText("partner name")).perform(click())

        onView(withId(R.id.partners_list_fragment)).check(isNotDisplayed())
        onView(withText(R.string.foodstuff_is_sent)).check(matches(isDisplayed()))

        val request = fakeHttpClient.getRequestsMatching(".*direct_partner_msg.*").first()
        assertTrue("URL: ${request.url}", request.url.contains("partner_user_id=uid"))
    }

    @Test
    fun cannotSendFoodstuff_whenNotLoggedIn() {
        onView(allOf(
                withText(foodstuffs[0].name),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))
        ).perform(longClick())
        onView(withText(R.string.send_to_partner)).check(isNotDisplayed())
    }

    @Test
    fun receivedFoodstuff_whenMainScreenOpened() {
        testReceivedFoodstuffInMainScreen()
    }

    private fun testReceivedFoodstuffInMainScreen() {
        val name = "newcoolfoodstuff"
        val foodstuff = Foodstuff.withName(name).withNutrition(1f, 2f, 3f, 4f)
        val msg = FoodstuffsCorrespondenceManager.createFoodstuffDirectMsg(foodstuff)
        directMsgsManager.onNewDirectMsg(msg.first, msg.second)

        val snackbarMsg = context.getString(R.string.foodstuff_is_received, name)
        onView(withText(snackbarMsg)).check(matches(isDisplayed()))
        onView(withText(R.string.show_received_foodstuff)).perform(click())

        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()))
        onView(allOf(
                withText(name),
                isDescendantOfA(withId(R.id.foodstuff_card_layout))
        )).check(matches(isDisplayed()))
        onView(allOf(
                withText("1"),
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                isDescendantOfA(withId(R.id.protein_layout))
        )).check(matches(isDisplayed()))
        onView(allOf(
                withText("2"),
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                isDescendantOfA(withId(R.id.fats_layout))
        )).check(matches(isDisplayed()))
        onView(allOf(
                withText("3"),
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                isDescendantOfA(withId(R.id.carbs_layout))
        )).check(matches(isDisplayed()))
        onView(allOf(
                withText("4"),
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                isDescendantOfA(withId(R.id.calories_layout))
        )).check(matches(isDisplayed()))

        onView(withId(R.id.button_close)).perform(click())
        // Main screen fragment is expected to be always opened after "SHOW" click,
        // even if other fragment was shown before the click.
        //
        // NOTE also that we check the fragment after we close the foodstuff card -
        // that is because the card is a window, and it doesn't contain the main fragment,
        // which would cause a test fail.
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()))
    }

    @Test
    fun receivedFoodstuff_whenHistoryOpened() {
        onView(withId(R.id.menu_item_history)).perform(click())
        testReceivedFoodstuffInMainScreen()
    }

    @Test
    fun receivedFoodstuff_whenProfileOpened() {
        onView(withId(R.id.menu_item_profile)).perform(click())
        testReceivedFoodstuffInMainScreen()
    }
}