package korablique.recipecalculator.ui.mainactivity

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.STATUS_PARTNER_USER_NOT_FOUND
import korablique.recipecalculator.outside.fcm.FCMManager
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.partners.SERV_MSG_PAIRED_WITH_PARTNER
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.util.EspressoUtils.isNotDisplayed
import korablique.recipecalculator.util.checkNetworkChangesSnackbarReaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.not
import org.junit.Assert.assertFalse
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@LargeTest
class PairingTest : MainActivityTestsBase() {
    @Before
    override fun setUp() {
        super.setUp()
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        setUpStartPairingResponse(timeProvider.now().plusMinutes(10).millis)
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
                    "partners": []
                }
            """
            RequestResult.Success(Response(body))
        }
        mActivityRule.launchActivity(null)

        // Interactive params obtainer will register through GP
        GlobalScope.launch(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }

        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())
    }

    @Test
    fun stateWhenJustOpened() {
        onView(withId(R.id.add_fab)).perform(click())

        onView(withId(R.id.your_pairing_code_text)).check(matches(withText("1234")))
        onView(withId(R.id.partner_pairing_code_edittext)).check(matches(withText("")))
        // Assert that countdown seconds text is displayed
        mainThreadExecutor.execute {
            val countDownText =
                    mActivityRule.activity.findViewById<TextView>(R.id.countdown_seconds_value_text)
                            .text.toString()
            val countDownVal = countDownText.toInt()
            Assert.assertTrue("Val: $countDownVal", countDownVal in 550..600)
        }
    }

    @Test
    fun stateUntilPairingCodeObtained() {
        setUpStartPairingResponse(timeProvider.now().plusMinutes(10).millis, delay = 1000)

        onView(withId(R.id.add_fab)).perform(click())

        onView(withId(R.id.progress_bar_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.countdown_seconds_title)).check(isNotDisplayed())
        onView(withId(R.id.countdown_seconds_value_text)).check(isNotDisplayed())
        onView(withId(R.id.partner_pairing_code_edittext)).check(matches(not(isEnabled())))
        Thread.sleep(1500)
        onView(withId(R.id.progress_bar_layout)).check(isNotDisplayed())
        onView(withId(R.id.countdown_seconds_title)).check(matches(isDisplayed()))
        onView(withId(R.id.countdown_seconds_value_text)).check(matches(isDisplayed()))
        onView(withId(R.id.partner_pairing_code_edittext)).check(matches(isEnabled()))
    }

    @Test
    fun pairingProcess() {
        onView(withId(R.id.add_fab)).perform(click())

        // Prepare to pairing request
        fakeHttpClient.setResponse(".*pairing_request.*") {
            val body = """{ "status": "ok" }"""
            RequestResult.Success(Response(body))
        }

        assertEquals(0, fakeHttpClient.getRequestsMatching(".*pairing_request.*").size)

        // Type partner's code and verify sent request
        onView(withId(R.id.partner_pairing_code_edittext)).perform(typeText("4321"))

        // Check request
        assertEquals(1, fakeHttpClient.getRequestsMatching(".*pairing_request.*").size)
        val request = fakeHttpClient.getRequestsMatching(".*pairing_request.*")[0]
        assertTrue("Url: $request.url", request.url.contains("partner_pairing_code=4321"))

        // Check shown snackbar
        onView(withText(R.string.pairing_request_sent)).check(matches(isDisplayed()))

        // Prepare to respond with updated partners list
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

        // Verify we still have 0 partners and still are in pairing process
        assertEquals(0, partnersRegistry.getPartnersCache().size)
        onView(withId(R.id.pairing_fragment)).check(matches(isDisplayed()))

        // Simulate successful pairing msg
        fcmManager.onMessageReceived(FCMManager.createMsgForTests(SERV_MSG_PAIRED_WITH_PARTNER))

        // Verify the partner is received
        assertEquals(1, partnersRegistry.getPartnersCache().size)
        assertEquals(Partner("uid", "partner name"), partnersRegistry.getPartnersCache().first())

        // Verify that pairing fragment is closed
        onView(withId(R.id.pairing_fragment)).check(isNotDisplayed())

        // Verify that partner is added to list
        onView(withId(R.id.partners_list_fragment)).check(matches(isDisplayed()))
        onView(allOf(
                isDescendantOfA(withId(R.id.partners_list_recycler_view)),
                withText("partner name")
        )).check(matches(isDisplayed()))

        // Verify that a snackbar with partner name is shown
        onView(withText(
                context.getString(R.string.successfully_paired_with, "partner name")))
                .check(matches(isDisplayed()))
        assertFalse(softKeyboardStateWatcher.isSoftKeyboardShown)
    }

    @Test
    fun generateAnotherCodeAfterExpirationDate() {
        // Expired immediately
        setUpStartPairingResponse(timeProvider.now().millis)
        onView(withId(R.id.add_fab)).perform(click())

        // Just in case
        Thread.sleep(100)

        verifyCommonInactiveState()
        setUpStartPairingResponse(timeProvider.now().plusMinutes(10).millis)
        onView(withId(R.id.request_code_button)).perform(click())
        verifyCommonActiveState()

        // Assert that countdown seconds text is displayed
        mainThreadExecutor.execute {
            val countDownText =
                    mActivityRule.activity.findViewById<TextView>(R.id.countdown_seconds_value_text)
                            .text.toString()
            val countDownVal = countDownText.toInt()
            Assert.assertTrue("Val: $countDownVal", countDownVal in 550..600)
        }
    }

    @Test
    fun networkErrorOnPairingStart() {
        fakeHttpClient.setResponse(".*start_pairing.*") {
            RequestResult.Failure(IOException())
        }

        onView(withId(R.id.add_fab)).perform(click())

        verifyCommonInactiveState()
        onView(withText(R.string.possibly_no_network_connection)).check(matches(isDisplayed()))

        onView(withId(R.id.request_code_button)).perform(click())
        // Retry doesn't work on continuous errors
        verifyCommonInactiveState()
        onView(withText(R.string.possibly_no_network_connection)).check(matches(isDisplayed()))

        // Get rid of server errors and verify retry button behaviour
        setUpStartPairingResponse(timeProvider.now().plusMinutes(10).millis)
        onView(withId(R.id.request_code_button)).perform(click())
        verifyCommonActiveState()
    }

    @Test
    fun serverErrorOnPairingStart() {
        fakeHttpClient.setResponse(".*start_pairing.*") {
            val body = """
                    {
                        "status": "internal_error",
                        "error_description": "hello there"
                    }
                """
            RequestResult.Success(Response(body))
        }

        onView(withId(R.id.add_fab)).perform(click())
        // Closed immediately
        onView(withId(R.id.pairing_fragment)).check(isNotDisplayed())
        onView(withText(R.string.something_went_wrong)).check(matches(isDisplayed()))
    }

    @Test
    fun networkErrorOnPairingRequest() {
        // Prepare to pairing request
        fakeHttpClient.setResponse(".*pairing_request.*")  {
            RequestResult.Failure(IOException())
        }

        onView(withId(R.id.add_fab)).perform(click())

        onView(withText(R.string.possibly_no_network_connection)).check(isNotDisplayed())
        onView(withId(R.id.partner_pairing_code_edittext)).perform(typeText("4321"))
        onView(withText(R.string.possibly_no_network_connection)).check(matches(isDisplayed()))
        assertFalse(softKeyboardStateWatcher.isSoftKeyboardShown)
    }

    @Test
    fun serverErrorOnPairingRequest() {
        // Prepare to pairing request
        fakeHttpClient.setResponse(".*pairing_request.*") {
            val body = """
                    {
                        "status": "internal_error",
                        "error_description": "hello there"
                    }
                """
            RequestResult.Success(Response(body))
        }

        onView(withId(R.id.add_fab)).perform(click())

        onView(withText(R.string.something_went_wrong)).check(isNotDisplayed())
        onView(withId(R.id.partner_pairing_code_edittext)).perform(typeText("4321"))

        // Closed
        onView(withId(R.id.pairing_fragment)).check(isNotDisplayed())
        onView(withText(R.string.something_went_wrong)).check(matches(isDisplayed()))
    }

    @Test
    fun pairingPartnerNotFound() {
        // Prepare to pairing request
        fakeHttpClient.setResponse(".*pairing_request.*") {
            val body = """
                    {
                        "status": "$STATUS_PARTNER_USER_NOT_FOUND",
                        "error_description": "hello there"
                    }
                """
            RequestResult.Success(Response(body))
        }

        onView(withId(R.id.add_fab)).perform(click())

        val errorMsg = context.getString(R.string.partner_with_code_not_found, "4321")
        onView(withText(errorMsg)).check(isNotDisplayed())
        onView(withId(R.id.partner_pairing_code_edittext)).perform(typeText("4321"))
        onView(withText(errorMsg)).check(matches(isDisplayed()))
        assertFalse(softKeyboardStateWatcher.isSoftKeyboardShown)
    }


    @Test
    fun networkUnavailableMessage() {
        // NOTE: currently the test works because the pairing fragment has a
        // partners list fragment behind of it, but that can change in the future
        // and the test will fail if it changes.
        onView(withId(R.id.add_fab)).perform(click())
        checkNetworkChangesSnackbarReaction(fakeNetworkStateDispatcher)
    }

    private fun setUpStartPairingResponse(expirationDate: Long, delay: Long? = null) {
        val body = """
                    {
                        "status": "ok",
                        "pairing_code": "1234",
                        "pairing_code_expiration_date": ${expirationDate/1000}
                    }
                """
        val response = RequestResult.Success(Response(body))

        if (delay == null) {
            fakeHttpClient.setResponse(".*start_pairing.*") { response }
        } else {
            fakeHttpClient.setDelayedResponse(".*start_pairing.*", delay) { response }
        }
    }

    private fun verifyCommonActiveState() {
        onView(withId(R.id.your_pairing_code_text)).check(matches(isDisplayed()))
        onView(withId(R.id.countdown_seconds_title)).check(matches(isDisplayed()))
        onView(withId(R.id.countdown_seconds_value_text)).check(matches(isDisplayed()))
        onView(withId(R.id.request_code_button)).check(isNotDisplayed())
        onView(withId(R.id.partner_pairing_code_edittext)).check(matches(isEnabled()))
    }

    private fun verifyCommonInactiveState() {
        onView(withId(R.id.your_pairing_code_text)).check(isNotDisplayed())
        onView(withId(R.id.countdown_seconds_title)).check(isNotDisplayed())
        onView(withId(R.id.countdown_seconds_value_text)).check(isNotDisplayed())
        onView(withId(R.id.request_code_button)).check(matches(isDisplayed()))
        onView(withId(R.id.partner_pairing_code_edittext)).check(matches(not(isEnabled())))
    }
}
