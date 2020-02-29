package korablique.recipecalculator.ui.mainactivity.partners.pairing

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import korablique.recipecalculator.R
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.TypedRequestResult
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.launch
import javax.inject.Inject

@FragmentScope
class PairingFragmentController @Inject constructor(
        fragmentCallbacks: FragmentCallbacks,
        private val fragment: PairingFragment,
        private val httpClient: HttpClient,
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
        val responseFull = httpClient.requestWithTypedResponse(url, StartPairingResponseFull::class)
        val response = when (responseFull) {
            is TypedRequestResult.Failure -> {
                Toast.makeText(fragment.context, "Something went wrong: $responseFull", Toast.LENGTH_LONG).show()
                fragment.close()
                return
            }
            is TypedRequestResult.Success -> {
                responseFull.result.simplify()
            }
        }

        when (response) {
            is StartPairingResponse.ServerError -> {
                Toast.makeText(fragment.context, "Something went wrong: $response", Toast.LENGTH_LONG).show()
                fragment.close()
                return
            }
            is StartPairingResponse.ParseError -> {
                Toast.makeText(fragment.context, "Something went wrong: $response", Toast.LENGTH_LONG).show()
                fragment.close()
                return
            }
            is StartPairingResponse.Ok -> {
                view.findViewById<TextView>(
                        R.id.your_pairing_code_text).text = response.pairing_code.toString()
                view.findViewById<View>(
                        R.id.progress_bar_layout).visibility = View.GONE
            }
        }

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
    }

    private suspend fun sendPairingRequest(to: String, from: ServerUserParams) {
        val url = ("https://blazern.me/broccalc/v1/user/pairing_request?"
                + "client_token=${from.token}&user_id=${from.uid}&partner_pairing_code=$to")
        val responseFull = httpClient.requestWithTypedResponse(url, PairingRequestResponseFull::class)
        val response = when (responseFull) {
            is TypedRequestResult.Failure -> {
                Toast.makeText(fragment.context, "Something went wrong: $responseFull", Toast.LENGTH_LONG).show()
                fragment.close()
                return
            }
            is TypedRequestResult.Success -> {
                responseFull.result.simplify()
            }
        }

        when (response) {
            is PairingRequestResponse.ServerError -> {
                if (response.err.status != "partner_user_not_found") {
                    Toast.makeText(fragment.context, "Something went wrong: $response", Toast.LENGTH_LONG).show()
                    fragment.close()
                } else {
                    Toast.makeText(fragment.context, "Pairing request is sent, nice!", Toast.LENGTH_LONG).show()
                }
            }
            is PairingRequestResponse.ParseError -> {
                Toast.makeText(fragment.context, "Something went wrong: $response", Toast.LENGTH_LONG).show()
                fragment.close()
            }
            is PairingRequestResponse.Ok -> {
                Toast.makeText(fragment.context, "Pairing request is sent, nice!", Toast.LENGTH_LONG).show()
            }
        }
    }
}