package korablique.recipecalculator.outside.thirdparty

import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import korablique.recipecalculator.RequestCodes
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseActivity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val GP_APP_TOKEN = "560504820389-e0pvlp32fn3kn10ud6md0fp533f0170f.apps.googleusercontent.com"

sealed class GPAuthResult {
    data class Success(val token: String) : GPAuthResult()
    data class Failure(val exception: Exception) : GPAuthResult()
    object CanceledByUser : GPAuthResult()
}

private sealed class SilentGPAuthResult {
    data class Success(val token: String) : SilentGPAuthResult()
    data class Failure(val exception: Exception) : SilentGPAuthResult()
    object ExplicitSignInRequired : SilentGPAuthResult()
}

@Singleton
open class GPAuthorizer @Inject constructor() {
    open suspend fun auth(context: BaseActivity): GPAuthResult {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(GP_APP_TOKEN)
                .build()
        val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
        val silentResult = silentSignIn(googleSignInClient)

        return when (silentResult) {
            is SilentGPAuthResult.ExplicitSignInRequired -> {
                explicitSignIn(context, googleSignInClient)
            }
            is SilentGPAuthResult.Success -> {
                GPAuthResult.Success(silentResult.token)
            }
            is SilentGPAuthResult.Failure -> {
                GPAuthResult.Failure(silentResult.exception)
            }
        }
    }

    private suspend fun explicitSignIn(
            context: BaseActivity,
            googleSignInClient: GoogleSignInClient): GPAuthResult = suspendCoroutine { continuation ->
        // Let's start the GP auth activity!
        val intent = googleSignInClient.getSignInIntent()
        context.startActivityForResult(intent, RequestCodes.GOOGLE_SIGN_IN)
        // And subscribe to it
        context.activityCallbacks.addObserver(object : ActivityCallbacks.Observer {
            override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
                if (requestCode != RequestCodes.GOOGLE_SIGN_IN) {
                    return
                }
                context.activityCallbacks.removeObserver(this)

                val signInTask = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = signInTask.getResult(ApiException::class.java)
                    val token = account?.idToken
                    if (account == null || token == null) {
                        continuation.resume(
                                GPAuthResult.Failure(
                                        NullPointerException("Account or token is null: $account $token")))
                    } else {
                        continuation.resume(GPAuthResult.Success(token))
                    }
                } catch (api: ApiException) {
                    // The ApiException status code indicates the detailed failure reason.
                    // Please refer to the GoogleSignInStatusCodes class reference for more information.
                    if (api.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                        continuation.resume(GPAuthResult.CanceledByUser)
                    } else {
                        continuation.resume(GPAuthResult.Failure(api))
                    }
                }
            }
        })
    }

    private suspend fun silentSignIn(
            googleSignInClient: GoogleSignInClient): SilentGPAuthResult = suspendCoroutine { continuation ->
        val accountHandler = { signInAccount: GoogleSignInAccount? ->
            val token = signInAccount?.idToken
            if (token != null) {
                continuation.resume(SilentGPAuthResult.Success(token))
            } else {
                continuation.resume(
                        SilentGPAuthResult.Failure(
                                NullPointerException("Account or token is null: $signInAccount $token")))
            }
        }

        val task = googleSignInClient.silentSignIn()
        if (task.isSuccessful) {
            val signInAccount = task.result
            accountHandler.invoke(signInAccount)
        } else {
            // There's no immediate result ready
            task.addOnCompleteListener { task ->
                try {
                    val signInAccount = task.getResult(ApiException::class.java)
                    accountHandler.invoke(signInAccount)
                } catch (apiException: ApiException) {
                    if (apiException.statusCode == GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                        continuation.resume(SilentGPAuthResult.ExplicitSignInRequired)
                    } else {
                        continuation.resume(SilentGPAuthResult.Failure(apiException))
                    }
                }
            }
        }
    }
}