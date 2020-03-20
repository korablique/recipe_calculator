package korablique.recipecalculator.ui.mainactivity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.userparams.ObtainResult
import korablique.recipecalculator.outside.userparams.ServerUserParams
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityAuthTest : MainActivityTestsBase() {
    @Test
    fun noAccMoveServAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "uid",
                    "client_token": "token"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val result = serverUserParamsObtainer.obtainUserParams()
        assertTrue(result is ObtainResult.Success)
        val success = result as ObtainResult.Success
        assertEquals(ServerUserParams("uid", "token"), success.params)
    }

    @Test
    fun accMoveServAuth() {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "already_registered",
                    "error_description": "wow such error"
                }
            """.trimIndent()))
        }
        fakeHttpClient.setResponse(".*move_device_account.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "uid",
                    "client_token": "token",
                    "user_name": "general kenobi"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val resultFuture = GlobalScope.async(mainThreadExecutor) {
            serverUserParamsObtainer.obtainUserParams()
        }
        onView(withId(R.id.positive_button)).perform(click())

        val result = runBlocking { resultFuture.await() }
        assertEquals("$result", ObtainResult.Success::class, result::class)
        val success = result as ObtainResult.Success
        assertEquals(ServerUserParams("uid", "token"), success.params)
    }

    @Test
    fun cancelledByUserAccMoveServAuth() {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "already_registered",
                    "error_description": "wow such error"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val resultFuture = GlobalScope.async(mainThreadExecutor) {
            serverUserParamsObtainer.obtainUserParams()
        }
        onView(withId(R.id.negative_button)).perform(click())

        val result = runBlocking { resultFuture.await() }
        assertEquals("$result", ObtainResult.CanceledByUser::class, result::class)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*move_device_account.*").size)
    }

    @Test
    fun cancelledByUserGPAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.CanceledByUser
        mActivityRule.launchActivity(null)
        val result = serverUserParamsObtainer.obtainUserParams()
        assertEquals("$result", ObtainResult.CanceledByUser::class, result::class)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*register.*").size)
    }

    @Test
    fun failedGPAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.Failure(Exception("such exception wow"))
        mActivityRule.launchActivity(null)
        val result = serverUserParamsObtainer.obtainUserParams()
        assertEquals("$result", ObtainResult.Failure::class, result::class)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*register.*").size)
    }

    @Test
    fun failedServAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*") {
            RequestResult.Success(Response("""
                {
                    "status": "internal_error",
                    "error_description": "wow such error"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val result = serverUserParamsObtainer.obtainUserParams()
        assertEquals("$result", ObtainResult.Failure::class, result::class)
    }
}
