package korablique.recipecalculator.ui.mainactivity.partners.pairing

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.R
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.outside.STATUS_ALREADY_REGISTERED
import korablique.recipecalculator.outside.http.*
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.launch
import javax.inject.Inject

@FragmentScope
class PairingFragmentController @Inject constructor(
        fragmentCallbacks: FragmentCallbacks,
        private val fragment: PairingFragment,
        private val httpContext: BroccalcHttpContext,
        private val userParamsRegistry: ServerUserParamsRegistry) : FragmentCallbacks.Observer {
    init {
        fragmentCallbacks.addObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        fragment.lifecycleScope.launch {
            init(fragmentView)
        }
    }

    // TODO: handle all of the corner cases properly
    // TODO: display countdown (codes have expiration date)
    private suspend fun init(view: View) {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            fragment.close()
            return
        }
        val url = ("https://blazern.me/broccalc/v1/user/start_pairing?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}")
        val response = httpContext.execute {
            val response = httpRequestUnwrapped(url, StartPairingResponse::class)

            view.findViewById<TextView>(
                    R.id.your_pairing_code_text).text = response.pairing_code.toString()
            view.findViewById<View>(
                    R.id.progress_bar_layout).visibility = View.GONE

            val partnerPairingCodeView = view.findViewById<EditText>(R.id.partner_pairing_code_edittext)
            partnerPairingCodeView.addTextChangedListener(object : TextWatcherAdapter() {
                override fun afterTextChanged(view: Editable) {
                    if (view.toString().length == 4) {
                        fragment.lifecycleScope.launch {
                            sendPairingRequest(to = view.toString(), from = userParams)
                        }
                    }
                }
            })

            BroccalcNetJobResult.Ok(Unit)
        }

        if (response is BroccalcNetJobResult.Error) {
            Toast.makeText(fragment.context, "Something went wrong: ${response.unwrapException()}", Toast.LENGTH_LONG).show()
            fragment.close()
        }
    }

    private suspend fun sendPairingRequest(to: String, from: ServerUserParams) {
        val url = ("https://blazern.me/broccalc/v1/user/pairing_request?"
                + "client_token=${from.token}&user_id=${from.uid}&partner_pairing_code=$to")

        val response = httpContext.execute {
            httpRequest(url, PairingRequestResponse::class)
        }
        when (response) {
            is BroccalcNetJobResult.Error -> {
                if (STATUS_ALREADY_REGISTERED != response.tryGetServerErrorStatus()) {
                    Toast.makeText(fragment.context, "Something went wrong: $response", Toast.LENGTH_LONG).show()
                    fragment.close()
                }
            }
        }
        Toast.makeText(fragment.context, "Pairing request is sent, nice!", Toast.LENGTH_LONG).show()
    }
}

@JsonClass(generateAdapter = true)
private data class StartPairingResponse(
        val pairing_code: Int,
        val pairing_code_expiration_date: Long
)

@JsonClass(generateAdapter = true)
private data class PairingRequestResponse(
        val status: String
)