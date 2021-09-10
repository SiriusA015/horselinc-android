package com.horselinc.views.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.github.razir.progressbutton.*
import com.horselinc.*
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.event.HLSubmitInstantPayoutEvent
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_instant_payout.*
import me.abhinay.input.CurrencySymbols
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.concurrent.TimeUnit

class HLInstantPayoutActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable ()

    private val MAXIMUM_AMOUNT: Float = 9999.0f
    private var maximumAmount: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instant_payout)

        initActivity ()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            hideKeyboard()
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initActivity () {
        // title
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_white)

        val title = SpannableString(getString(R.string.instant_payout))
        title.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        supportActionBar?.title = title

        // edit text
        amountEditText?.setCurrency(CurrencySymbols.USA)
        amountEditText?.hint = "${CurrencySymbols.USA} 0.00 Maximum"

        HLGlobalData.paymentAccount?.let { account ->
            amountEditText?.setCurrency(Currency.getInstance(account.balance.currency).symbol)

            val amount = account.balance.amount / 100
            maximumAmount = if (amount > MAXIMUM_AMOUNT) MAXIMUM_AMOUNT else amount
            amountEditText?.hint = "${maximumAmount.toCurrencyString(account.balance.currency)} Maximum"
        }

        setReactiveX ()

        // submit button
        submitButton.isEnabled = false
        setProgressButton ()

        // event handlers
        submitButton.setOnClickListener { onClickSubmit () }
    }

    private fun setReactiveX () {
        disposable.add(
            RxTextView.textChanges(amountEditText)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val amount = amountEditText.cleanDoubleValue
                    if (amount > 0 && amount <= maximumAmount ) {
                        submitButton.setBackgroundResource(R.drawable.corner22_background_ec7185)
                        submitButton.isEnabled = true
                    } else {
                        submitButton.setBackgroundResource(R.drawable.corner22_background_33cbcbcb)
                        submitButton.isEnabled = false
                    }
                }
        )
    }


    /**
     *  Event Handlers
     */
    private fun onClickSubmit () {
        hideKeyboard()

        HLGlobalData.me?.serviceProvider?.account?.let { account ->
            showProgressButton()
            HLFirebaseService.instance.instantPayout(account.id, amountEditText.cleanDoubleValue.toFloat() * 100, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    hideProgressButton()
                    EventBus.getDefault().post(HLSubmitInstantPayoutEvent())
                    finish()
                }

                override fun onFailure(error: String) {
                    hideProgressButton()
                    showErrorMessage(error)
                }
            })
        }
    }


    /**
     *  Progress Button Handlers
     */
    private fun setProgressButton () {
        bindProgressButton(submitButton)
        submitButton.attachTextChangeAnimator {
            fadeInMills = 300
            fadeOutMills = 300
        }
    }

    private fun showProgressButton () {
        submitButton.showProgress {
            this.progressColor = Color.WHITE
            this.gravity = DrawableButton.GRAVITY_CENTER
        }
        submitButton.isEnabled = false
    }

    private fun hideProgressButton () {
        submitButton.run {
            hideProgress(R.string.submit)
            isEnabled = true
        }
    }

}
