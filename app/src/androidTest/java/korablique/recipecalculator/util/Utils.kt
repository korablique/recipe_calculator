package korablique.recipecalculator.util

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import korablique.recipecalculator.FakeNetworkStateDispatcher
import korablique.recipecalculator.R

fun checkNetworkChangesSnackbarReaction(fakeNetworkStateDispatcher: FakeNetworkStateDispatcher) {
    Espresso.onView(ViewMatchers.withText(R.string.possibly_no_network_connection)).check(EspressoUtils.isNotDisplayed())
    fakeNetworkStateDispatcher.setNetworkAvailable(false)
    Espresso.onView(ViewMatchers.withText(R.string.possibly_no_network_connection)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    fakeNetworkStateDispatcher.setNetworkAvailable(true)
    // For some reason the snackbar takes a long time to become gone
    Thread.sleep(250)
    Espresso.onView(ViewMatchers.withText(R.string.possibly_no_network_connection)).check(EspressoUtils.isNotDisplayed())
}