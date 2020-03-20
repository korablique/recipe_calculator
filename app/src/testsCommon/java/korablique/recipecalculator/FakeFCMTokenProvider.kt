package korablique.recipecalculator

import korablique.recipecalculator.outside.fcm.FCMTokenObtainer

class FakeFCMTokenProvider(val token: ()->String?) : FCMTokenObtainer() {
    override suspend fun requestToken() = token()
}