package korablique.recipecalculator.outside.fcm

import com.google.firebase.iid.FirebaseInstanceId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
open class FCMTokenObtainer @Inject constructor() {
    open suspend fun requestToken(): String? = suspendCoroutine { continuation ->
        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        continuation.resume(null)
                        return@addOnCompleteListener
                    }

                    continuation.resume(task.result?.token)
                }
    }
}