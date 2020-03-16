package korablique.recipecalculator.outside.userparams

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.ui.TwoOptionsDialog
import java.lang.IllegalStateException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val MOVE_ACCOUNT_DIALOG_TAG = "MOVE_ACCOUNT_DIALOG_TAG"

sealed class ObtainResult {
    data class Success(val params: ServerUserParams) : ObtainResult()
    data class Failure(val exception: Exception) : ObtainResult()
    object CanceledByUser : ObtainResult()
}

/**
 * Интерактивно (взаимодействуя с пользователем) получает ServerUserParams.
 */
@ActivityScope
open class InteractiveServerUserParamsObtainer @Inject constructor(
        private val activity: BaseActivity,
        private val activityCallbacks: ActivityCallbacks,
        private val serverUserParamsRegistry: ServerUserParamsRegistry
) : ActivityCallbacks.Observer {
    init {
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            throw IllegalStateException("Must be instantiated before Activity.onCreate()")
        }
        activityCallbacks.addObserver(this)
    }

    override fun onActivityCreate(savedInstanceState: Bundle?) {
        val existingDialog = TwoOptionsDialog.findDialog(activity.supportFragmentManager, MOVE_ACCOUNT_DIALOG_TAG)
        // Not important dialog
        existingDialog?.dismiss()
    }

    suspend fun obtainUserParams(): ObtainResult {
        val paramsResult1 = serverUserParamsRegistry.getUserParamsMaybeRegister(activity)
        when (paramsResult1) {
            is GetWithRegistrationRequestResult.Success -> {
                return ObtainResult.Success(paramsResult1.user)
            }
            is GetWithRegistrationRequestResult.Failure -> {
                return ObtainResult.Failure(paramsResult1.exception)
            }
            is GetWithRegistrationRequestResult.CanceledByUser -> {
                return ObtainResult.CanceledByUser
            }
            is GetWithRegistrationRequestResult.RegistrationParamsTaken -> {
                // Will handle below
            }
        }

        val userChoice = askUserToMoveAccount()
        if (userChoice == TwoOptionsDialog.ButtonName.NEGATIVE) {
            return ObtainResult.CanceledByUser
        }
        val paramsResult2 = serverUserParamsRegistry.getUserParamsMaybeMoveAccount(activity)
        when (paramsResult2) {
            is GetWithAccountMoveRequestResult.Success -> {
                return ObtainResult.Success(paramsResult2.user)
            }
            is GetWithAccountMoveRequestResult.Failure -> {
                return ObtainResult.Failure(paramsResult2.exception)
            }
            is GetWithAccountMoveRequestResult.CanceledByUser -> {
                return ObtainResult.CanceledByUser
            }
        }
    }

    private suspend fun askUserToMoveAccount(): TwoOptionsDialog.ButtonName = suspendCoroutine { continuation ->
        val dialog = TwoOptionsDialog.showDialog(
                activity.supportFragmentManager,
                MOVE_ACCOUNT_DIALOG_TAG,
                activity.getString(R.string.gp_account_move_request),
                activity.getString(R.string.gp_account_move_request_confirmation),
                activity.getString(R.string.gp_account_move_request_cancellation))

        var receivedButtonClick = false
        dialog.setOnButtonsClickListener {
            receivedButtonClick = true
            dialog.dismiss()
            continuation.resume(it)
        }
        dialog.setOnDismissListener {
            if (!receivedButtonClick) {
                continuation.resume(TwoOptionsDialog.ButtonName.NEGATIVE)
            }
        }
    }
}
