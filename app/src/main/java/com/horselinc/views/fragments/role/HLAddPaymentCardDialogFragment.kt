package com.horselinc.views.fragments.role


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RelativeLayout
import androidx.fragment.app.DialogFragment
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLStripeCustomerModel
import com.horselinc.models.data.HLUserModel
import com.horselinc.views.listeners.HLAddPaymentCardListener
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.TokenCallback
import com.stripe.android.model.Token
import com.stripe.android.view.CardMultilineWidget


class HLAddPaymentCardDialogFragment(private val cardUser: HLUserModel? = HLGlobalData.me, private val addPaymentCardListener: HLAddPaymentCardListener) : DialogFragment() {

    private lateinit var cardWidget: CardMultilineWidget
    private lateinit var addButton: Button
    private lateinit var contentView: View

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity!!)

        contentView = LayoutInflater.from(context).inflate(R.layout.fragment_add_payment_card_dialog, null)
        dialog.setContentView(contentView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)

        // initialize controls
        cardWidget = contentView.findViewById(R.id.cardWidget)
        addButton = contentView.findViewById(R.id.addButton)

        // bind button
        setProgressButton(addButton)

        // event handler
        addButton.setOnClickListener { onClickAdd () }
        contentView.findViewById<Button>(R.id.cancelButton).setOnClickListener { dismiss() }

        return dialog
    }

    private fun onClickAdd () {

        val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(contentView.windowToken, 0)

        val card = cardWidget.card

        if (card == null) {
            showErrorMessage("Invalid card information")
        } else {
            context?.let { ctx ->
                showProgressButton(addButton, Color.parseColor("#335c77"))

                val stripe = Stripe(ctx, PaymentConfiguration.getInstance().publishableKey)
                stripe.createToken(card, object : TokenCallback {
                    override fun onSuccess(result: Token) {
                        addCardToCustomer(result)
                    }

                    override fun onError(error: Exception) {
                        hideProgressButton(addButton, stringResId = R.string.add)
                    }
                })
            }
        }
    }

    private fun addCardToCustomer (token: Token) {
        cardUser?.let { user ->
            user.horseManager?.customer?.let {
                HLFirebaseService.instance.addCardToCustomer(user.uid, it.id, token.id, object: ResponseCallback<HLStripeCustomerModel> {
                    override fun onSuccess(data: HLStripeCustomerModel) {
                        addPaymentCardListener.onAddToCustomer(data)
                        dismiss()
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(addButton, stringResId = R.string.add)
                        showErrorMessage(error)
                    }
                })
            }
        }
    }
}
