package korablique.recipecalculator.util

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import korablique.recipecalculator.FakeNetworkStateDispatcher
import korablique.recipecalculator.R
import korablique.recipecalculator.util.EspressoUtils.isNotDisplayed

fun checkNetworkChangesSnackbarReaction(fakeNetworkStateDispatcher: FakeNetworkStateDispatcher) {
    onView(withText(R.string.possibly_no_network_connection)).check(isNotDisplayed())
    fakeNetworkStateDispatcher.setNetworkAvailable(false)
    onView(withText(R.string.possibly_no_network_connection)).check(matches(isDisplayed()))
    fakeNetworkStateDispatcher.setNetworkAvailable(true)
    // For some reason the snackbar takes a long time to become gone
    Thread.sleep(250)
    onView(withText(R.string.possibly_no_network_connection)).check(isNotDisplayed())
}
