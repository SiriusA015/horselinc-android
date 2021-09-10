package com.horselinc.views.fragments.common

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.*
import com.horselinc.models.event.*
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.activities.HLHorseManagerMainActivity
import com.horselinc.views.activities.HLServiceProviderMainActivity
import com.horselinc.views.customs.HLSeparatorView
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLEventBusFragment
import com.horselinc.views.fragments.manager.HLManagerEditPaymentOptionsFragment
import com.horselinc.views.fragments.provider.HLProviderEditInvoiceFragment
import com.horselinc.views.listeners.HLInvoiceUserInfoViewListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.makeramen.roundedimageview.RoundedImageView
import com.squareup.picasso.Picasso
import com.stripe.android.model.Card
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


/** Created by jcooperation0137 on 2019-08-29.
 */
class HLInvoiceDetailFragment(var invoice: HLInvoiceModel): HLEventBusFragment() {

    private var toolBar: Toolbar? = null
    private var invoiceUserInfoView: HLInvoiceUserInfoView? = null
    private var contentsLayout: LinearLayout? = null
    private var invoiceServiceRequestsView: HLInvoiceServiceRequestsView? = null
    private var invoiceTipView: HLInvoiceTipView? = null
    private var invoiceTotalView: HLInvoiceServiceView? = null
    private var invoiceBalancePaidView: HLInvoiceServiceView? = null
    private var invoiceOutstandingBalanceView: HLInvoiceServiceView? = null
    private var invoiceActionView: HLInvoiceActionView? = null

    private val disposable = CompositeDisposable ()
    private var shouldShowTipView = false
    private var paymentSucceeded = false
    private var payersDefaultCards = HashMap<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_invoice_detail, container, false)

            toolBar = rootView?.findViewById(R.id.toolBar)
            contentsLayout = rootView?.findViewById(R.id.contentsLayout)

            addObserver()
            initControls()
            setReactiveX()
        }

        return rootView
    }


    override fun onDestroy() {
        disposable.dispose()
        HLFirebaseService.instance.removeServiceRequestListeners()
        super.onDestroy()
    }

    // MARK: - Actions


    // MARK: - Event Handlers
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceHorseProfileClicked(event: HLEventInvoiceHorseProfileClicked) {
        // Go to public horse profile
        if (context == null) return
        replaceFragment(HLPublicHorseProfileFragment(event.horse))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentInvoiceRefresh(event: HLEventPaymentInvoiceRefresh) {
        if (context == null) return
        this.invoice = event.invoice.copy()
        getPaymentInfo()
        initControls()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentInvoiceUpdated(event: HLEventPaymentInvoiceUpdated) {

        if (event.invoiceId.isEmpty()) {
            hideProgressDialog()
            return
        }

        HLFirebaseService.instance.getInvoices(arrayListOf(event.invoiceId),
            object : ResponseCallback<List<HLInvoiceModel>> {
                override fun onSuccess(data: List<HLInvoiceModel>) {
                    data.firstOrNull()?.let {
                        invoice = it
                        getPayersPaymentOptions()
                        getPaymentInfo()
                    }
                }

                override fun onFailure(error: String) {
                    Log.e("PaymentInvoiceUpdated", error)
                }
            })
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceEditInvoiceButtonClicked(event: HLEventInvoiceEditInvoiceButtonClicked) {
        // Go to edit invoice screen.
        replaceFragment(HLProviderEditInvoiceFragment(invoice.copy()))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceMarkAsPaidButtonClicked(event: HLEventInvoiceMarkAsPaidButtonClicked) {
        hideKeyboard()
        // If this invoice is draft, main service provider submits invoice
        if (invoice.status == HLInvoiceStatusType.DRAFT) {
            if (context == null) return

            val dialog = AlertDialog.Builder(activity)
            dialog.apply {
                setTitle("Submit Invoice?")
                setMessage("Are you sure you want to submit this invoice for payment?")
                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()

                    invoice.run {
                        amount = 0.0
                        tip = 0.0
                        createdAt = Calendar.getInstance().timeInMillis
                        isInvoiceCreating = false
                        invoiceCreated = false
                    }

                    invoiceActionView?.setData(invoice)

                    invoice.status = HLInvoiceStatusType.SUBMITTED
                    invoice.requests?.let { requests ->
                        if (requests.isNotEmpty()) {
                            invoice.shareInfo = requests.first().shareInfo
                        }
                    }
                    invoice.requests?.forEach { it.status = HLServiceRequestStatus.INVOICED}

                    HLFirebaseService.instance.putInvoice(invoice, false, object : ResponseCallback<HLInvoiceModel> {
                        override fun onSuccess(data: HLInvoiceModel) {
                            invoice.isInvoiceCreating = false
                            invoice.invoiceCreated = true

                            if (data.getHorseManager() == null) {
                                shareInviteLink (data)
                            } else {
                                this@HLInvoiceDetailFragment.showSuccessMessage("Invoice submitted")
                                EventBus.getDefault().post(HLEventPaymentInvoiceCreated(data))
                                popFragment()
                            }
                        }

                        override fun onFailure(error: String) {
                            this@HLInvoiceDetailFragment.showErrorMessage(error)
                            invoice.run {
                                isInvoiceCreating = false
                                invoiceCreated = false
                                status = HLInvoiceStatusType.DRAFT
                            }
                            invoiceActionView?.setData(invoice)
                        }
                    })
                }
                show()
            }
        } else {

            if (HLGlobalData.me?.type == HLUserType.PROVIDER && invoice.getHorseManager() == null) {
                shareInviteLink(invoice, false)
            } else {
                // Main service provider, set mark as rea
                val dialog = AlertDialog.Builder(activity)
                dialog.apply {
                    setTitle("Mark Invoice As Paid?")
                    setMessage("This means you have received payment outside the app and the horse owner(s) will no longer be able to submit payment through the app.")
                    setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        invoice.isMarkingAsPaid = true
                        invoiceActionView?.setData(invoice)

                        if (HLGlobalData.me?.type == HLUserType.PROVIDER && HLGlobalData.me?.serviceProvider?.userId != null) {

                            HLFirebaseService.instance.markInvoiceAsPaid(HLGlobalData.me?.serviceProvider?.userId!!,
                                invoice.uid,
                                object: ResponseCallback<Boolean> {
                                    override fun onSuccess(data: Boolean) {
                                        this@HLInvoiceDetailFragment.showSuccessMessage("Your invoice has been marked as paid.")
                                        invoice.isMarkingAsPaid = false
                                        invoice.markAsPaid = true
                                        EventBus.getDefault().post(HLEventPaymentInvoiceMarkAsPaid(invoice))
                                        popFragment()
                                    }

                                    override fun onFailure(error: String) {
                                        this@HLInvoiceDetailFragment.showErrorMessage( error)

                                        invoice.isMarkingAsPaid = false
                                        invoice.markAsPaid = false
                                        invoiceActionView?.setData(invoice)
                                    }
                                })
                        }
                    }.show()
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceSendPaymentReminderButtonClicked(event: HLEventInvoiceSendPaymentReminderButtonClicked) {
        hideKeyboard()

        // If this invoice is draft, an assigner requests invoice submission to a main service provider.
        if (invoice.status == HLInvoiceStatusType.DRAFT) {
            if (!invoice.isMainServiceProvider() && HLGlobalData.me?.type == HLUserType.PROVIDER) {

                val requestIds = invoice.requestIds
                val assignerId = HLGlobalData.me?.serviceProvider?.userId
                val serviceProviderId = invoice.getServiceProvider()?.userId

                if (requestIds.isNullOrEmpty() || assignerId.isNullOrEmpty() || serviceProviderId.isNullOrEmpty()) return

                invoice.isSubmissionRequesting = true
                invoiceActionView?.setData(invoice)

                HLFirebaseService.instance.requestPaymentSubmission(assignerId,
                    serviceProviderId,
                    requestIds,
                    object: ResponseCallback<Boolean> {
                        override fun onSuccess(data: Boolean) {
                            this@HLInvoiceDetailFragment.showSuccessMessage("Payment has been requested.")

                            invoice.isSubmissionRequesting = false
                            invoice.submissionRequested = true
                            invoiceActionView?.setData(invoice)
                        }

                        override fun onFailure(error: String) {
                            this@HLInvoiceDetailFragment.showErrorMessage(error)

                            invoice.isSubmissionRequesting = false
                            invoice.submissionRequested = false
                            invoiceActionView?.setData(invoice)
                        }
                    })
            }
        } else {
            val dialog = AlertDialog.Builder(activity)
            dialog.apply {
                setTitle("Request Payment?")
                setMessage("Are you sure you want to request payment for these services?\n A notification will be sent to the horse trainer(s).")
                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    invoice.isPaymentRequesting = true
                    invoice.paymentRequested = false
                    invoiceActionView?.setData(invoice)

                    HLFirebaseService.instance.requestPayment(invoice.uid,
                        object : ResponseCallback<Boolean> {
                            override fun onSuccess(data: Boolean) {
                                this@HLInvoiceDetailFragment.showSuccessMessage("Payment has been requested.")

                                invoice.isPaymentRequesting = false
                                invoice.paymentRequested = true
                                invoiceActionView?.setData(invoice)
                            }

                            override fun onFailure(error: String) {
                                this@HLInvoiceDetailFragment.showErrorMessage(error)

                                invoice.isPaymentRequesting = false
                                invoice.paymentRequested = false
                                invoiceActionView?.setData(invoice)
                            }
                        })
                }.show()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceCardChangeButtonClicked(event: HLEventInvoiceCardChangeButtonClicked) {
        hideKeyboard()

        if (event.cards.isNotEmpty()) {
            if (event.cards.size == 1) {
                this@HLInvoiceDetailFragment.showInfoMessage("You have to add other payment method before change")
            } else {
                showChangePaymentMethodAlert (event.payer, event.cards)
            }

        } else {
            this@HLInvoiceDetailFragment.showInfoMessage("You have to add a payment method")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedInvoiceFullApproverCardChangeButtonClicked(event: HLEventInvoiceFullApproverCardChangeButtonClicked) {
        hideKeyboard()

        if (event.cards.isNotEmpty()) {
            if (event.cards.size == 1) {
                this@HLInvoiceDetailFragment.showInfoMessage("You can't change the payer's payment method because there is only one payment method.")
            } else {
                showChangePayerPaymentMethodAlert(event.payer, event.cards)
            }

        } else {
            this@HLInvoiceDetailFragment.showInfoMessage("The payer has not yet set the payment method.")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoicePayerSubmitPaymentButtonClicked(event: HLEventInvoicePayerSubmitPaymentButtonClicked) {
        hideKeyboard()

        HLGlobalData.me?.horseManager?.customer?.cards?.let { cards ->
            if (cards.isEmpty()) {
                showAddPaymentAlert ()
            } else {
                submitPayment(event.payer.userId, null, null)
            }
        } ?: showAddPaymentAlert ()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceFullApproverSubmitPaymentButtonClicked(event: HLEventInvoiceFullApproverSubmitPaymentButtonClicked) {
        hideKeyboard()

        if (HLGlobalData.me?.type != HLUserType.MANAGER) return
        val approverId = HLGlobalData.me?.horseManager?.userId

        submitPayment(event.payer.userId, approverId, event.payer.customer?.defaultSource)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoicePartialApproverRequestApprovalButtonClicked(event: HLEventInvoicePartialApproverRequestApprovalButtonClicked) {

        if (HLGlobalData.me?.type != HLUserType.MANAGER) return

        val approvalAmount = invoice.getApprovalAmountFor(event.payer)
        val managerId = HLGlobalData.me?.horseManager?.userId

        if (approvalAmount == null || managerId.isNullOrEmpty()) return

        invoice.isSubmissionRequesting = true
        invoice.submissionRequested = false
        invoiceActionView?.setData(invoice)

        HLFirebaseService.instance.requestPaymentApproval(managerId,
            event.payer.userId,
            approvalAmount,
            object : ResponseCallback<Boolean> {
                override fun onSuccess(data: Boolean) {
                    this@HLInvoiceDetailFragment.showSuccessMessage("Payment has been requested.")

                    invoice.isSubmissionRequesting = false
                    invoice.submissionRequested = true
                    invoiceActionView?.setData(invoice)
                }

                override fun onFailure(error: String) {
                    this@HLInvoiceDetailFragment.showErrorMessage(error)

                    invoice.isSubmissionRequesting = false
                    invoice.submissionRequested = false
                    invoiceActionView?.setData(invoice)
                }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventInvoiceObserverRequestApprovalButtonClicked(event: HLEventInvoiceObserverRequestApprovalButtonClicked) {
        invoice.isPaymentRequesting = true
        invoice.paymentRequested = false
        invoiceActionView?.setData(invoice)


        HLFirebaseService.instance.requestPayment(invoice.uid,
            object : ResponseCallback<Boolean> {
                override fun onSuccess(data: Boolean) {
                    this@HLInvoiceDetailFragment.showSuccessMessage("Payment has been requested.")

                    invoice.isPaymentRequesting = false
                    invoice.paymentRequested = true
                    invoiceActionView?.setData(invoice)
                }

                override fun onFailure(error: String) {
                    this@HLInvoiceDetailFragment.showErrorMessage(error)

                    invoice.isPaymentRequesting = false
                    invoice.paymentRequested = false
                    invoiceActionView?.setData(invoice)
                }
            })
    }

    /**
     * Show Payment Alert
     */
    private fun showAddPaymentAlert () {
        AlertDialog.Builder(activity).apply {
            setTitle(R.string.app_name)
            setMessage("You have to add a payment method")
            setPositiveButton("OK") { dialog, _ ->
                replaceFragment(HLManagerEditPaymentOptionsFragment ())
            }.show()
        }
    }

    // MARK: - Functions
    fun initControls() {

        activity?.let {
            if (HLGlobalData.me?.type == HLUserType.MANAGER) {

                (it as HLHorseManagerMainActivity).setSupportActionBar(toolBar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            } else {
                (it as HLServiceProviderMainActivity).setSupportActionBar(toolBar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            context?.let {
                toolBar?.setTitleTextColor(ContextCompat.getColor(it, R.color.colorWhite))

                if (toolBar?.navigationIcon != null) {
                    DrawableCompat.setTint(toolBar?.navigationIcon!!, ContextCompat.getColor(it, R.color.colorWhite))
                }
            }
            if (invoice.status == HLInvoiceStatusType.DRAFT) {
                toolBar?.title = "Invoice Draft"
            } else {
                toolBar?.title = "Invoice Detail"
            }
        }


        toolBar?.setNavigationOnClickListener {
            popFragment()
        }


        contentsLayout?.removeAllViews()

        // Add user info view
        invoiceUserInfoView?:let {
            invoiceUserInfoView = HLInvoiceUserInfoView(context, object : HLInvoiceUserInfoViewListener {
                override fun onClickUserAvatar(user: HLBaseUserModel) {
                    // Go to public user profile screen
                    replaceFragment(HLPublicUserProfileFragment(user))
                }
            })
        }
        invoiceUserInfoView?.let {
            it.setData(invoice)
            contentsLayout?.addView(it)
        }
        context?.let {
            contentsLayout?.addView(HLSeparatorView(it))
        }


        // Add service requests view
        invoiceServiceRequestsView?:let {
            invoiceServiceRequestsView = HLInvoiceServiceRequestsView(context)
        }
        invoiceServiceRequestsView?.let {
            it.setData(invoice)
            contentsLayout?.addView(it)
        }
        context?.let {
            contentsLayout?.addView(HLSeparatorView(it))
        }

        // Check whether or not app should show the tip view or not
        when ((HLGlobalData.me?.type == HLUserType.MANAGER && invoice.status != HLInvoiceStatusType.DRAFT) ||
                (invoice.status == HLInvoiceStatusType.PAID || invoice.status == HLInvoiceStatusType.FULL_PAID) &&
                (invoice.isMainServiceProvider() || HLGlobalData.me?.type == HLUserType.MANAGER)) {
            true -> {
                shouldShowTipView = true

                // Add tip view
                invoiceTipView?:let {
                    invoiceTipView = HLInvoiceTipView(context)
                }
                invoiceTipView?.let {
                    it.setData(invoice)
                    contentsLayout?.addView(it)
                }
                context?.let {
                    contentsLayout?.addView(HLSeparatorView(it))
                }
            }
        }

        // Add Invoice Total / Your Apportionment
        invoiceTotalView?:let {
            invoiceTotalView = HLInvoiceServiceView(context)
        }
        invoiceTotalView?.let {
            it.setInvoicePaidAmount(invoice)
            contentsLayout?.addView(it)
        }
        context?.let {
            contentsLayout?.addView(HLSeparatorView(it))
        }

        // Check whether or not app should show the balance paid and outstanding balance
        if (invoice.status == HLInvoiceStatusType.PAID && HLGlobalData.me?.type == HLUserType.PROVIDER) {
            // Add Balance Paid
            invoiceBalancePaidView?:let {
                invoiceBalancePaidView = HLInvoiceServiceView(context)
            }
            invoiceBalancePaidView?.let {
                it.setInvoiceBalancePaid(invoice)
                contentsLayout?.addView(it)
            }
            context?.let {
                contentsLayout?.addView(HLSeparatorView(it))
            }

            // Add Outstanding Balance Views
            invoiceOutstandingBalanceView?:let {
                invoiceOutstandingBalanceView = HLInvoiceServiceView(context)
            }
            invoiceOutstandingBalanceView?.let {
                it.setInvoiceOutstandingBalance(invoice)
                contentsLayout?.addView(it)
            }
            context?.let {
                contentsLayout?.addView(HLSeparatorView(it))
            }
        }

        // Add the bottom action view.
        invoiceActionView?:let {
            invoiceActionView = HLInvoiceActionView(context, this)
        }
        invoiceActionView?.let {
            it.setData(invoice)
            contentsLayout?.addView(it)
        }
    }

    private fun addObserver() {

        getPayersPaymentOptions()

        // If this invoice is not draft, add invoice listener
        if (!invoice.uid.isEmpty()) {
            HLFirebaseService.instance.subscribeInvoiceStatusUpdatedListener(invoice.uid)
            return
        }

        getPaymentInfo()
    }

    private fun getPaymentInfo() {

        if (invoice.uid.isNotEmpty()) {
            // Get payments info.
            HLFirebaseService.instance.getPaymentsFor(invoice.uid,
                object : ResponseCallback<ArrayList<HLPaymentModel>> {
                    override fun onSuccess(data: ArrayList<HLPaymentModel>) {
                        invoice.payments = data
                        activity?.runOnUiThread {
                            initControls()
                        }
                    }

                    override fun onFailure(error: String) {
                        this@HLInvoiceDetailFragment.showErrorMessage(error)
                    }
                })
        }
    }

    private fun getPayersPaymentOptions() {
        // Get payers' card information if you are a payer of them.
        invoice.payers?.let {

            fun reloadData() {
                if (invoiceActionView == null) {
                    initControls()
                } else {
                    invoiceActionView?.setData(invoice)
                }
            }
            val payersCount = it.size
            for ((index, payer) in it.withIndex()) {
                if (invoice.isPayableBehalfOf(payer)) {
                    HLFirebaseService.instance.getUser(payer.userId, object : ResponseCallback<HLUserModel> {
                        override fun onSuccess(data: HLUserModel) {
                            payer.customer = data.horseManager?.customer

                            val defaultCard = payersDefaultCards[payer.userId]
                            defaultCard?.let {
                                payer.customer?.defaultSource = defaultCard
                            }
                            if (index == payersCount - 1) reloadData()
                        }

                        override fun onFailure(error: String) {
                            print(error)
                        }
                    })
                }
            }
        }
    }


    // MARK: - Change Payment Method Handler
    private fun showChangePaymentMethodAlert(payer: HLHorseManagerModel, cards: ArrayList<HLStripeCardModel>) {

        val items = cards.map { it.brand + " " + it.last4 }

        val adapter = ArrayAdapter<String>(activity!!, R.layout.item_select_payment_option_dlg, R.id.cardInfoTextView, items)
        val dlg = androidx.appcompat.app.AlertDialog.Builder(activity!!)
            .setCancelable(true)
            .setAdapter(adapter, null)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create().apply {
                listView.itemsCanFocus = false
                listView.choiceMode = ListView.CHOICE_MODE_SINGLE
                listView.setOnItemClickListener { _, _, position, _ ->
                    this.dismiss()
                    changeDefaultCard(payer, cards[position].id)
                }
            }
        dlg.show()
    }

    private fun showChangePayerPaymentMethodAlert(payer: HLHorseManagerModel, cards: ArrayList<HLStripeCardModel>) {

        val items = cards.map { it.brand + " ***" + it.last4.last() }

        val adapter = ArrayAdapter<String>(activity!!, R.layout.item_select_payment_option_dlg, R.id.cardInfoTextView, items)
        val dlg = androidx.appcompat.app.AlertDialog.Builder(activity!!)
            .setCancelable(true)
            .setAdapter(adapter, null)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create().apply {
                listView.itemsCanFocus = false
                listView.choiceMode = ListView.CHOICE_MODE_SINGLE
                listView.setOnItemClickListener { _, _, position, _ ->
                    this.dismiss()
                    // Change the payer's default source manually
                    payersDefaultCards[payer.userId] = cards[position].id
                    payer.customer?.defaultSource = cards[position].id
                    invoiceActionView?.setData(invoice)
                }
            }
        dlg.show()
    }

    private fun changeDefaultCard(payer: HLHorseManagerModel, cardId: String) {
        val uid = payer.userId
        val customerId = payer.customer?.id

        if (customerId == null) {
            hideProgressDialog()
            Log.d("HLInvoiceDetailFragment", "Payer id and customer id required")
            return
        }

        showProgressDialog()
        HLFirebaseService.instance.changeDefaultCard(uid, customerId, cardId, object: ResponseCallback<HLStripeCustomerModel> {
            override fun onSuccess(data: HLStripeCustomerModel) {
                hideProgressDialog()
                invoice.getHorseManager()?.customer = data
                invoiceActionView?.setData(invoice)
            }

            override fun onFailure(error: String) {
                hideProgressDialog()
                this@HLInvoiceDetailFragment.showErrorMessage(error)
            }
        })
    }

    fun setReactiveX() {
        if (shouldShowTipView) {
            invoiceTipView?.tipAmountEditView?.let {
                disposable.add(
                    RxTextView.textChanges(it)
                        .skipInitialValue()
                        .debounce(300, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { tip ->
                            try {
                                if (HLGlobalData.me?.type == HLUserType.MANAGER && invoice.status == HLInvoiceStatusType.SUBMITTED) {
                                    val tipValue = tip.replace(Regex.fromLiteral("$"), "").trim().toDouble()
                                    invoice.tip = tipValue / 1.0.withStripeFee
                                    invoiceTotalView?.setInvoicePaidAmount(invoice)
                                    invoiceActionView?.setData(invoice)
                                    it.error = null
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                it.error = "Please input valid number"
                            }
                        }
                )
            }
        }
    }

    fun submitPayment(payerId: String, approverId: String?, payerPaymentSourceId: String?) {

        val dialog = AlertDialog.Builder(activity)
        dialog.apply {
            setTitle("Submit Payment?")
            setMessage("Are you sure you want to submit payment for this invoice?")
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                invoice.isPaymentSubmitting = true
                invoice.paymentSubmitted = false
                invoiceActionView?.setData(invoice)

                HLFirebaseService.instance.putInvoice(invoice, false, object : ResponseCallback<HLInvoiceModel> {
                    override fun onSuccess(data: HLInvoiceModel) {
                        // If I am an approver, send payment be half of payer
                        approverId?.let {
                            submitInvoicePayment(payerId, approverId, payerPaymentSourceId,null, null)
                            return
                        }

                        // TODO google payment
                        submitInvoicePayment(payerId, approverId, null, null, null)
                    }

                    override fun onFailure(error: String) {
                        this@HLInvoiceDetailFragment.showErrorMessage(error)
                    }
                })
            }

        }.show()
    }

    fun submitInvoicePayment(payerId: String, approverId: String?, payerPaymentSourceId: String?, googleSource: String?, callback: ResponseCallback<Boolean>?) {

        showProgressDialog()
        HLFirebaseService.instance.submitInvoicePayment(invoice.uid,
            payerId,
            approverId,
            payerPaymentSourceId,
            googleSource,
            object : ResponseCallback<Boolean> {
                override fun onSuccess(data: Boolean) {
                    // Get the payments history
                    HLFirebaseService.instance.getPaymentsFor(invoice.uid, object : ResponseCallback<ArrayList<HLPaymentModel>> {
                        override fun onSuccess(data: ArrayList<HLPaymentModel>) {
                            paymentSucceeded = true

                            callback?.onSuccess(true)
                            this@HLInvoiceDetailFragment.hideProgressDialog()
                            this@HLInvoiceDetailFragment.showSuccessMessage("Paid invoice successfully.")

                            val payments = data
                            val payers = invoice.payers

                            if (payments.isNullOrEmpty() || invoice.payers.isNullOrEmpty()) return

                            if (payments.size == payers?.size) {
                                // All members are paid, just popup
//                                NotificationCenter.default.post(name: HLNotificationName.didPayerSubmitPayment, object: nil, userInfo: ["invoice": self.invoice!])
                                popFragment()
                            } else {
                                payments.let {
                                    invoice.payments = it
                                }
                                invoice.isPaymentSubmitting = false
                                invoice.paymentRequested = true
                                invoiceActionView?.setData(invoice)
                            }
                        }

                        override fun onFailure(error: String) {
                            this@HLInvoiceDetailFragment.hideProgressDialog()
                            this@HLInvoiceDetailFragment.showErrorMessage(error)
                            paymentSucceeded = false
                            callback?.onFailure("Can't get the invoice payment info")
                        }
                    })
                }

                override fun onFailure(error: String) {
                    this@HLInvoiceDetailFragment.hideProgressDialog()
                    this@HLInvoiceDetailFragment.showErrorMessage(error)
                    paymentSucceeded = false
                    invoice.isPaymentSubmitting = false
                    invoice.paymentSubmitted = false
                    invoiceActionView?.setData(invoice)

                    callback?.onFailure(error)
                }
            })

    }

    private fun shareInviteLink (invoice: HLInvoiceModel, hasFinishHandler: Boolean = true) {
        HLGlobalData.me?.let { user ->
            activity?.let { act ->
                /*showProgressDialog()
                HLBranchService.createInviteLink(act, user.uid, HLUserType.PROVIDER, invoice.uid, invoice.getHorse()?.uid,
                    object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()

                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Signup for HorseLinc $data to manager all of your equine needs.")
                                type = "text/plain"
                            }

                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)

                            if (hasFinishHandler) {
                                EventBus.getDefault().post(HLEventPaymentInvoiceCreated(invoice))
                                popFragment()
                            }
                        }

                        override fun onFailure(error: String) {
                            showError(error)
                        }
                    })*/
                showProgressDialog()
                HLFirebaseService.instance.shareInvoice(user.uid, (invoice.getHorse()?.uid ?: ""), invoice.uid, invoice.shareInfo?.phone,
                    invoice.shareInfo?.email, object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()
                            if (hasFinishHandler) {
                                EventBus.getDefault().post(HLEventPaymentInvoiceCreated(invoice))
                                popFragment()
                            } else {
                                this@HLInvoiceDetailFragment.showSuccessMessage("Payment has been requested.")
                            }
                        }

                        override fun onFailure(error: String) {
                            showError(error)
                        }
                    })
            }
        }
    }

    @SuppressLint("ViewConstructor")
    class HLInvoiceUserInfoView(context: Context?, listener: HLInvoiceUserInfoViewListener): LinearLayout(context) {

        var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_user_info, this)
        var avatarImageView: RoundedImageView = containerView.findViewById(R.id.avatarImageView)
        var nameTextView: TextView = containerView.findViewById(R.id.nameTextView)
        var submittedDateTextView: TextView = containerView.findViewById(R.id.submittedDateTextView)
        var fullPaidDateTextView: TextView = containerView.findViewById(R.id.fullPaidDateTextView)
        var approverNoteTextView: TextView = containerView.findViewById(R.id.approverNoteTextView)

        var invoice: HLInvoiceModel? = null

        init {
            avatarImageView.setOnClickListener {

                HLGlobalData.me?.type?.let { type ->
                    when (type) {
                        HLUserType.PROVIDER -> {
                            invoice?.getServiceProvider()?.let {
                                listener.onClickUserAvatar(it)
                            }
                        }
                        else -> {
                            invoice?.getHorseManager()?.let {
                                listener.onClickUserAvatar(it)
                            }
                        }
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun setData(invoice: HLInvoiceModel) {
            this.invoice = invoice

            HLGlobalData.me?.type.let { type ->
                when (type) {
                    HLUserType.PROVIDER -> {
                        val horseManager = invoice.getHorseManager()
                        if (horseManager != null) {
                            if (horseManager.avatarUrl.isNullOrEmpty()) {
                                avatarImageView.setImageResource(R.drawable.ic_profile_placeholder)
                            } else {
                                Picasso.get()
                                    .load(Uri.parse(horseManager.avatarUrl))
                                    .resize(ResourceUtil.dpToPx(30), ResourceUtil.dpToPx(30))
                                    .centerInside()
                                    .into(avatarImageView)
                            }

                            nameTextView.text = horseManager.name

                            submittedDateTextView.visibility = View.GONE
                            invoice.createdAt.let {
                                submittedDateTextView.text = "INVOICE SUBMITTED: " + it.simpleDateString
                                submittedDateTextView.visibility = View.VISIBLE
                            }

                        } else {
                            avatarImageView.setImageResource(R.drawable.ic_profile_placeholder)
                            if (invoice.status == HLInvoiceStatusType.DRAFT) {
                                nameTextView.visibility = View.VISIBLE
//                                nameTextView.text =  "Anonymous"
                                if (invoice.requests?.isEmpty() == false && invoice.requests?.first()?.shareInfo != null) {
                                    val shareInfo = invoice.requests?.first()?.shareInfo
                                    if (!shareInfo?.name.isNullOrEmpty()) {
                                        if (!shareInfo?.email.isNullOrEmpty()) {
                                            nameTextView.text = "${shareInfo?.name}\n${shareInfo?.email}"
                                        } else if (!shareInfo?.phone.isNullOrEmpty()) {
                                            nameTextView.text = "${shareInfo?.name}\n${shareInfo?.phone}"
                                        } else {
                                            nameTextView.visibility = View.GONE
                                        }
                                    } else {
                                        if (!shareInfo?.email.isNullOrEmpty()) {
                                            nameTextView.text = "${shareInfo?.email}"
                                        } else if (!shareInfo?.phone.isNullOrEmpty()) {
                                            nameTextView.text = "${shareInfo?.phone}"
                                        } else {
                                            nameTextView.visibility = View.GONE
                                        }
                                    }
                                } else {
                                    nameTextView.visibility = View.GONE
                                }
                                submittedDateTextView.visibility = View.GONE
                            } else {
                                nameTextView.visibility = View.VISIBLE
                                if (invoice.shareInfo != null) {
                                    if (!invoice.shareInfo?.name.isNullOrEmpty()) {
                                        if (!invoice.shareInfo?.email.isNullOrEmpty()) {
                                            nameTextView.text = "Emailed to ${invoice.shareInfo?.name}\n${invoice.shareInfo?.email}"
                                        } else if (!invoice.shareInfo?.phone.isNullOrEmpty()) {
                                            nameTextView.text = "Sent to ${invoice.shareInfo?.name}\n${invoice.shareInfo?.phone}"
                                        } else {
                                            nameTextView.visibility = View.GONE
                                        }
                                    } else {
                                        if (!invoice.shareInfo?.email.isNullOrEmpty()) {
                                            nameTextView.text = "Emailed to ${invoice.shareInfo?.email}"
                                        } else if (!invoice.shareInfo?.phone.isNullOrEmpty()) {
                                            nameTextView.text = "Sent to ${invoice.shareInfo?.phone}"
                                        } else {
                                            nameTextView.visibility = View.GONE
                                        }
                                    }
                                } else {
                                    nameTextView.visibility = View.GONE
                                }

                                submittedDateTextView.visibility = View.VISIBLE
                                invoice.createdAt.let {
                                    submittedDateTextView.text = "INVOICE SUBMITTED: " + it.simpleDateString
                                    submittedDateTextView.visibility = View.VISIBLE
                                }
                            }
                        }

                        approverNoteTextView.visibility = View.GONE
                    }
                    else -> {
                        invoice.getServiceProvider()?.apply {
                            if (avatarUrl.isNullOrEmpty()) {
                                avatarImageView.setImageResource(R.drawable.ic_profile_placeholder)
                            } else {
                                Picasso.get()
                                    .load(Uri.parse(avatarUrl))
                                    .resize(ResourceUtil.dpToPx(30), ResourceUtil.dpToPx(30))
                                    .centerInside()
                                    .into(avatarImageView)
                            }

                            nameTextView.text = name
                        }

                        approverNoteTextView.visibility = View.GONE
                        HLGlobalData.me?.horseManager?.let {
                            approverNoteTextView.visibility = if (invoice.isPaymentApprover(it)) View.VISIBLE else View.GONE
                        }

                        submittedDateTextView.visibility = View.GONE
                        invoice.createdAt.let {
                            submittedDateTextView.text = "INVOICE SUBMITTED: " + it.simpleDateString
                            submittedDateTextView.visibility = View.VISIBLE
                        }
                    }
                }
            }

            fullPaidDateTextView.visibility = View.GONE
            invoice.fullPaidAt()?.let {
                fullPaidDateTextView.text = "MARKED AS PAID: " + it.simpleDateString
                fullPaidDateTextView.visibility = View.VISIBLE
            }
        }
    }

    class HLInvoiceServiceRequestsView(context: Context?): LinearLayout(context) {
        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_service_requests, this)
        var serviceRequestsLayout: LinearLayout = containerView.findViewById(R.id.serviceRequestsLayout)

        var invoice: HLInvoiceModel? = null

        fun setData(invoice: HLInvoiceModel) {
            this.invoice = invoice

            serviceRequestsLayout.removeAllViews()
            invoice.requests?.let {

                for ((index, request) in it.withIndex()) {
                    val requestServiceView = HLInvoiceServiceRequestView(context)
                    requestServiceView.setData(request, if (index == 0) null else invoice.requests?.get(index - 1))
                    serviceRequestsLayout.addView(requestServiceView)
                }
            }
        }
    }

    class HLInvoiceServiceRequestView(context: Context?): LinearLayout(context) {
        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_service_request, this)
        var horseInfoLayout: LinearLayout = containerView.findViewById(R.id.horseInfoLayout)
        var horseAvatarImageView: RoundedImageView = containerView.findViewById(R.id.horseAvatarImageView)
        var horseBarnNameTextView: TextView = containerView.findViewById(R.id.horseBarnNameTextView)
        var horseShowNameTextView: TextView = containerView.findViewById(R.id.horseShowNameTextView)
        var dateTextView: TextView = containerView.findViewById(R.id.dateTextView)
        var showNameTextView: TextView = containerView.findViewById(R.id.showNameTextView)
        var servicesLayout: LinearLayout = containerView.findViewById(R.id.servicesLayout)
        var instructionLabel: TextView = containerView.findViewById(R.id.instructionLabel)

        var request: HLServiceRequestModel? = null

        init {
            horseAvatarImageView.setOnClickListener {
                request?.horse?.let {
                    EventBus.getDefault().post(HLEventInvoiceHorseProfileClicked(it))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun setData(request: HLServiceRequestModel, previousRequest: HLServiceRequestModel?) {
            this.request = request

            request.horse?.apply{

                if (avatarUrl.isNullOrEmpty()) {
                    horseAvatarImageView.setImageResource(R.drawable.ic_horse_placeholder)
                } else {
                    Picasso.get()
                        .load(Uri.parse(avatarUrl))
                        .resize(ResourceUtil.dpToPx(30), ResourceUtil.dpToPx(30))
                        .centerInside()
                        .into(horseAvatarImageView)
                }

                horseBarnNameTextView.text = barnName
                horseShowNameTextView.text = displayName
            }

            if (previousRequest != null) {
                dateTextView.visibility = if (request.requestDate.calendar.isSameDay(previousRequest.requestDate.calendar))
                    View.GONE
                else
                    View.VISIBLE
                showNameTextView.visibility = dateTextView.visibility
            } else {
                dateTextView.visibility = View.VISIBLE
                showNameTextView.visibility = View.VISIBLE
            }

            dateTextView.text = request.requestDate.simpleDateString
            if (request.show?.name.orEmpty().isNotEmpty()) {
                showNameTextView.text = request.show?.name
                showNameTextView.visibility = View.VISIBLE
            } else {
                showNameTextView.visibility = View.GONE
            }

            if (request.instruction.isNullOrEmpty()) {
                instructionLabel.visibility = View.GONE
            } else {
                instructionLabel.text = "NOTES: " + request.instruction!!
            }

            servicesLayout.removeAllViews()
            request.services.let {
                for (service in it) {
                    val serviceView = HLInvoiceServiceView(context)
                    serviceView.setService(service)
                    servicesLayout.addView(serviceView)
                }
            }
        }
    }

    class HLInvoiceServiceView(context: Context?) : LinearLayout(context) {
        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_service, this)
        var valueTextView: TextView = containerView.findViewById(R.id.valueTextView)
        var priceTextView: TextView = containerView.findViewById(R.id.priceTextView)

        @SuppressLint("SetTextI18n")
        fun setService(service: HLServiceProviderServiceModel) {
            valueTextView.text = service.service + " x " + service.quantity
            val amount = service.rate * service.quantity
            if (HLGlobalData.me?.type == HLUserType.PROVIDER) {
                priceTextView.text = "$ " + String.format("%.2f", amount)
            } else {
                priceTextView.text = "$ " + String.format("%.2f", amount.toDouble().withStripeFee)
            }
        }

        @SuppressLint("SetTextI18n")
        fun setInvoicePaidAmount(invoice: HLInvoiceModel) {

            when {
                HLGlobalData.me?.type == HLUserType.MANAGER -> {
                    valueTextView.text = "Invoice Total"
                    priceTextView.text = "$ " + String.format("%.2f", ((invoice.amount?: 0.0) + (invoice.tip?: 0.0)).withStripeFee)
                }
                invoice.isMainServiceProvider() -> {
                    valueTextView.text = "Invoice Total"
                    priceTextView.text = "$ " + String.format("%.2f", (invoice.amount?: 0.0) + (invoice.tip?: 0.0))
                }
                else -> {
                    valueTextView.text = "Your Apportionment"
                    priceTextView.text = "$ " + String.format("%.2f", (invoice.amount?: 0.0))
                }
            }

            val typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            valueTextView.typeface = typeface
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f)
            valueTextView.setTextColor(Color.BLACK)
            priceTextView.typeface = typeface
            priceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f)
            priceTextView.setTextColor(Color.BLACK)
        }

        @SuppressLint("SetTextI18n")
        fun setInvoiceBalancePaid(invoice: HLInvoiceModel) {
            valueTextView.text = "Balance Paid"
            priceTextView.text = "$ " + String.format("%.2f", invoice.getPaidAmount())

            val typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            valueTextView.typeface = typeface
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f)
            valueTextView.setTextColor(Color.BLACK)
            priceTextView.typeface = typeface
            priceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f)
            priceTextView.setTextColor(Color.BLACK)
        }

        @SuppressLint("SetTextI18n")
        fun setInvoiceOutstandingBalance(invoice: HLInvoiceModel) {
            valueTextView.text = "Outstanding Balance"
            priceTextView.text = "$ " + String.format("%.2f", invoice.getOutstandingBalance())

            val typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            valueTextView.typeface = typeface
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f)
            valueTextView.setTextColor(Color.BLACK)
            priceTextView.typeface = typeface
            priceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.0f)
            priceTextView.setTextColor(Color.BLACK)
        }

    }

    class HLInvoiceTipView(context: Context?): LinearLayout(context) {
        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_tip, this)
        var tipTextView: TextView = containerView.findViewById(R.id.tipTextView)
        var tipAmountEditView: EditText = containerView.findViewById(R.id.tipAmountEditView)

        @SuppressLint("SetTextI18n")
        fun setData(invoice: HLInvoiceModel) {

            // Invoice editable if a user is the horse manager and invoice has not yet paid.
            if (HLGlobalData.me?.type == HLUserType.MANAGER && invoice.status == HLInvoiceStatusType.SUBMITTED) {
                tipTextView.text = "Add Tip"
                tipTextView.setTextColor(ContextCompat.getColor(context, R.color.colorPink))
                invoice.tip?.let {
                    if (it > 0.0) {
                        tipAmountEditView.setText("$ " + String.format("%.02f", it.withStripeFee), TextView.BufferType.EDITABLE)
                    }
                }
                tipAmountEditView.hint = "$0.00"
                tipAmountEditView.isEnabled = true
                tipAmountEditView.setTextColor(ContextCompat.getColor(context, R.color.quantum_pink))
            } else if ((invoice.status == HLInvoiceStatusType.PAID || invoice.status == HLInvoiceStatusType.FULL_PAID)
                    && (invoice.isMainServiceProvider() || HLGlobalData.me?.type == HLUserType.MANAGER)) {
                tipTextView.text = "Tip Added"
                tipTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoiceUnPaid))
                tipAmountEditView.isEnabled = false
                invoice.tip?.let {
                    if (HLGlobalData.me?.type == HLUserType.MANAGER) {
                        tipAmountEditView.setText("$ " + String.format("%.2f", it.withStripeFee), TextView.BufferType.NORMAL)
                    } else {
                        tipAmountEditView.setText("$ " + String.format("%.2f", it), TextView.BufferType.NORMAL)
                    }
                }
                tipAmountEditView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoiceUnPaid))
            }
        }
    }

    @SuppressLint("ViewConstructor")
    class HLInvoiceActionView(context: Context?, val fragment: HLBaseFragment?): LinearLayout(context) {
        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_action, this)
        var providerActionLayout: LinearLayout = containerView.findViewById(R.id.providerActionLayout)
        var editInvoiceLayout: LinearLayout = containerView.findViewById(R.id.editInvoiceLayout)
        var editInvoiceButton: Button = containerView.findViewById(R.id.editInvoiceButton)
        var markAsPaidButton: Button = containerView.findViewById(R.id.markAsPaidButton)
        var reminderLayout: LinearLayout = containerView.findViewById(R.id.reminderLayout)
        var sendReminderButton: Button = containerView.findViewById(R.id.sendReminderButton)
        var reminderNoteTextView: TextView = containerView.findViewById(R.id.reminderNoteTextView)
        var paymentsLayout: LinearLayout = containerView.findViewById(R.id.paymentsLayout)

        var invoice: HLInvoiceModel? = null
        init {
            fragment?.setProgressButton(editInvoiceButton)
            fragment?.setProgressButton(markAsPaidButton)
            fragment?.setProgressButton(sendReminderButton)

            editInvoiceButton.setOnClickListener {
                EventBus.getDefault().post(HLEventInvoiceEditInvoiceButtonClicked())
            }

            markAsPaidButton.setOnClickListener {
                EventBus.getDefault().post(HLEventInvoiceMarkAsPaidButtonClicked())
            }


            sendReminderButton.setOnClickListener {
                EventBus.getDefault().post(HLEventInvoiceSendPaymentReminderButtonClicked())
            }
        }

        @SuppressLint("SetTextI18n")
        fun setData(invoice: HLInvoiceModel) {
            this.invoice = invoice

            if (HLGlobalData.me?.type == HLUserType.PROVIDER && invoice.status != HLInvoiceStatusType.FULL_PAID) {
                paymentsLayout.visibility = View.VISIBLE
                providerActionLayout.visibility = View.VISIBLE

                editInvoiceLayout.visibility = if (invoice.isMainServiceProvider()) View.VISIBLE else View.GONE

                fragment?.hideProgressButton(editInvoiceButton, (invoice.status == HLInvoiceStatusType.DRAFT || invoice.status == HLInvoiceStatusType.SUBMITTED))

                if (invoice.status == HLInvoiceStatusType.DRAFT) {
                    markAsPaidButton.text = "Submit Invoice"
                    sendReminderButton.text = "Request Invoice Submission"

                    if (invoice.isSubmissionRequesting) {
                        fragment?.showProgressButton(sendReminderButton)
                    } else {
                        fragment?.hideProgressButton(sendReminderButton)
                    }

                    fragment?.hideProgressButton(sendReminderButton, !invoice.submissionRequested)
                    reminderLayout.visibility = if (invoice.isMainServiceProvider()) View.GONE else View.VISIBLE

                    if (invoice.isInvoiceCreating) {
                        fragment?.showProgressButton(markAsPaidButton)
                    } else {
                        fragment?.hideProgressButton(markAsPaidButton)
                    }

                    fragment?.hideProgressButton(markAsPaidButton, !invoice.invoiceCreated)
                } else {
                    if (invoice.getHorseManager() != null) {
                        markAsPaidButton.text = "Mark As Paid"
                        reminderLayout.visibility = View.VISIBLE
                    } else{
                        markAsPaidButton.text = "Send Reminder"
                        reminderLayout.visibility = View.GONE
                    }

                    if (invoice.isPaymentRequesting) {
                        fragment?.showProgressButton(sendReminderButton)
                    } else {
                        fragment?.hideProgressButton(sendReminderButton)
                    }

                    if (invoice.paymentRequested) {
                        fragment?.hideProgressButton(sendReminderButton, false, "Payment Reminder Sent")
                    } else {
                        fragment?.hideProgressButton(sendReminderButton, true, "Send Payment Reminder")
                    }
                }
                fragment?.hideProgressButton(markAsPaidButton, !invoice.markAsPaid)

            } else if (HLGlobalData.me?.type == HLUserType.MANAGER) {
                paymentsLayout.visibility = View.VISIBLE
                providerActionLayout.visibility = View.GONE

                paymentsLayout.removeAllViews()

                invoice.payers?.let {
                    for (payer in it) {
                        val invoicePaymentInfoView = HLInvoicePaymentInfoView(context, fragment)
                        invoicePaymentInfoView.setData(payer, invoice)
                        paymentsLayout.addView(invoicePaymentInfoView)
                    }
                }
            } else {
                paymentsLayout.visibility = View.GONE
                providerActionLayout.visibility = View.GONE
            }
        }
    }

    @SuppressLint("ViewConstructor")
    class HLInvoicePaymentInfoView(context: Context?, val fragment: HLBaseFragment?): LinearLayout(context) {

        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_payment_info, this)
        var paymentLayout: LinearLayout = containerView.findViewById(R.id.paymentLayout)
        var payerNameTextView: TextView = containerView.findViewById(R.id.payerNameTextView)
        var paidAtTextView: TextView = containerView.findViewById(R.id.paidAtTextView)
        var amountToPayTextView: TextView = containerView.findViewById(R.id.amountToPayTextView)
        var payerSubmitPaymentLayout: LinearLayout = containerView.findViewById(R.id.payerSubmitPaymentLayout)
        var cardLayout: LinearLayout = containerView.findViewById(R.id.cardLayout)
        var cardIconImageView: RoundedImageView = containerView.findViewById(R.id.cardIconImageView)
        var cardInfoTextView: TextView = containerView.findViewById(R.id.cardInfoTextView)
        var changeTextView: TextView = containerView.findViewById(R.id.changeTextView)
        var payerSubmitPaymentButton: Button = containerView.findViewById(R.id.payerSubmitPaymentButton)
        var fullApproverLayout: LinearLayout = containerView.findViewById(R.id.fullApproverLayout)
        var fullApproverNoteTextView: TextView = containerView.findViewById(R.id.fullApproverNoteTextView)
        var fullApproverSubmitPaymentButton: Button = containerView.findViewById(R.id.fullApproverSubmitPaymentButton)
        var fullApproverCardLayout: LinearLayout = containerView.findViewById(R.id.fullApproverCardLayout)
        var fullApproverCardIconImageView: RoundedImageView = containerView.findViewById(R.id.fullApproverCardIconImageView)
        var fullApproverCardInfoTextView: TextView = containerView.findViewById(R.id.fullApproverCardInfoTextView)
        var fullApproverChangeTextView: TextView = containerView.findViewById(R.id.fullApproverChangeTextView)
        var partialApproverLayout: LinearLayout = containerView.findViewById(R.id.partialApproverLayout)
        var partialApproverInfoTextView: TextView = containerView.findViewById(R.id.partialApproverInfoTextView)
        var partialApproverRequestApprovalButton: Button = containerView.findViewById(R.id.partialApproverRequestApprovalButton)
        var observerInfoLayout: LinearLayout = containerView.findViewById(R.id.observerInfoLayout)
        var observerInfoTextView: TextView = containerView.findViewById(R.id.observerInfoTextView)
        var observerRequestApprovalButton: Button = containerView.findViewById(R.id.observerRequestApprovalButton)

        var payer: HLHorseManagerModel? = null
        var invoice: HLInvoiceModel? = null
        var cards = arrayListOf<HLStripeCardModel>()
        init {
            fragment?.setProgressButton(payerSubmitPaymentButton)
            fragment?.setProgressButton(fullApproverSubmitPaymentButton)
            fragment?.setProgressButton(partialApproverRequestApprovalButton)
            fragment?.setProgressButton(observerRequestApprovalButton)

            changeTextView.setOnClickListener {
                payer?.let {
                    EventBus.getDefault().post(HLEventInvoiceCardChangeButtonClicked(it, cards))
                }
            }

            fullApproverChangeTextView.setOnClickListener {
                payer?.let {
                    EventBus.getDefault().post(HLEventInvoiceFullApproverCardChangeButtonClicked(it, cards))
                }
            }

            payerSubmitPaymentButton.setOnClickListener {
                payer?.let {
                    EventBus.getDefault().post(HLEventInvoicePayerSubmitPaymentButtonClicked(it))
                }
            }

            fullApproverSubmitPaymentButton.setOnClickListener {
                payer?.let {
                    EventBus.getDefault().post(HLEventInvoiceFullApproverSubmitPaymentButtonClicked(it))
                }
            }

            partialApproverRequestApprovalButton.setOnClickListener {
                payer?.let {
                    EventBus.getDefault().post(HLEventInvoicePartialApproverRequestApprovalButtonClicked(it))
                }
            }

            observerRequestApprovalButton.setOnClickListener {
                payer?.let {
                    EventBus.getDefault().post(HLEventInvoiceObserverRequestApprovalButtonClicked(it))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun setData(payer: HLHorseManagerModel, invoice: HLInvoiceModel) {
            this.payer = payer
            this.invoice = invoice

            payerNameTextView.text = payer.name
            amountToPayTextView.text = "$ " + String.format("%.2f", invoice.getPayerTotalAmount(payer))
            if (invoice.hasPaidBy(payer) && invoice.getPaymentOf(payer)?.createdAt != null) {
                paidAtTextView.text = invoice.getPaymentOf(payer)?.createdAt?.simpleDateString
                paidAtTextView.visibility = View.VISIBLE
                payerNameTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoicePaid))
                amountToPayTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoicePaid))
                paidAtTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoicePaid))

                payerSubmitPaymentLayout.visibility = View.GONE
                fullApproverLayout.visibility = View.GONE
                partialApproverLayout.visibility = View.GONE
                observerInfoLayout.visibility = View.GONE
            } else {
                paidAtTextView.text = ""
                paidAtTextView.visibility = View.GONE
                payerNameTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoiceUnPaid))
                amountToPayTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoiceUnPaid))
                paidAtTextView.setTextColor(ContextCompat.getColor(context, R.color.colorInvoiceUnPaid))

                if (HLGlobalData.me?.type == HLUserType.MANAGER) {
                    val horseManager = HLGlobalData.me?.horseManager
                    if (horseManager != null) {
                        if (invoice.isPaymentApprover(horseManager)) {
                            if (invoice.isPayableBehalfOf(payer)) {
                                payerSubmitPaymentLayout.visibility = View.GONE
                                fullApproverLayout.visibility = View.VISIBLE
                                partialApproverLayout.visibility = View.GONE
                                observerInfoLayout.visibility = View.GONE

                                payer.name.let {

                                    if (!invoice.payers.isNullOrEmpty()) {
                                        val text = "You are approving a portion of this payment on behalf of $it"

                                        val builder = SpannableStringBuilder(text)
                                        builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorTextHighlight)),
                                            0,
                                            text.length,
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        builder.setSpan(StyleSpan(Typeface.NORMAL), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        fullApproverNoteTextView.text = builder
                                    } else {
                                        val text = "You are approving this payment on behalf of $it"
                                        val builder = SpannableStringBuilder(text)
                                        builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorTextHighlight)),
                                            0,
                                            text.length,
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        builder.setSpan(StyleSpan(Typeface.NORMAL), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        fullApproverNoteTextView.text = builder
                                    }

                                    if (invoice.isPaymentSubmitting) {
                                        fragment?.showProgressButton(fullApproverSubmitPaymentButton)
                                    } else {
                                        fragment?.hideProgressButton(fullApproverSubmitPaymentButton)
                                    }
                                }

                                // Get the cards info from the current user because payer information will be involved payment information.
                                val customer = payer.customer
                                customer?.cards?.let { it ->
                                    cards.addAll(it)
                                }

                                // Set card information of payer
                                if (!cards.isNullOrEmpty()) {
                                    fullApproverCardLayout.visibility = View.VISIBLE

                                    val card =
                                        cards.firstOrNull { it.id == payer.customer?.defaultSource }
                                    if (card != null) {
                                        fullApproverCardIconImageView.setImageResource(Card.getBrandIcon(card.brand))
                                        fullApproverCardInfoTextView.text = card.brand + " ***" + card.last4.last()
                                    } else {
                                        fullApproverCardIconImageView.setImageResource(0)
                                        fullApproverCardInfoTextView.text = ""
                                    }
                                } else {
                                    fullApproverCardLayout.visibility = View.GONE
                                }

                            } else {
                                payerSubmitPaymentLayout.visibility = View.GONE
                                fullApproverLayout.visibility = View.GONE
                                partialApproverLayout.visibility = View.VISIBLE
                                observerInfoLayout.visibility = View.GONE

                                val approverAmount = invoice.getApprovalAmountFor(payer)
                                val userName = payer.name

                                if (approverAmount != null && !userName.isEmpty()) {
                                    val payerTotalAmount = invoice.getPayerTotalAmount(payer)
                                    var text = "This invoice is "
                                    val builder = SpannableStringBuilder(text)
                                    builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPink)),
                                        0,
                                        text.length,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(StyleSpan(Typeface.NORMAL), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                    var prevPosition = text.length
                                    text = "$ " + String.format("%.2f ", payerTotalAmount - approverAmount)
                                    builder.append(text)
                                    builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPink)),
                                        prevPosition,
                                        prevPosition + text.length,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(StyleSpan(Typeface.BOLD), prevPosition, prevPosition + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                    prevPosition += text.length
                                    text = "more than the maximum amount you are authorized to pay on behalf of $userName"
                                    builder.append(text)
                                    builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPink)),
                                        prevPosition,
                                        prevPosition + text.length,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    builder.setSpan(StyleSpan(Typeface.NORMAL), prevPosition, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    partialApproverInfoTextView.text = builder
                                }

                                if (invoice.isPaymentRequesting) {
                                    fragment?.showProgressButton(partialApproverRequestApprovalButton, ContextCompat.getColor(context, R.color.colorSecondaryButtonProgressBar))
                                } else {
                                    fragment?.hideProgressButton(partialApproverRequestApprovalButton)
                                }

                                fragment?.hideProgressButton(partialApproverRequestApprovalButton, !invoice.paymentRequested)
                            }
                        } else if (!(invoice.isPaymentApprover(horseManager) || invoice.isPayer(payer.userId))) {
                            payerSubmitPaymentLayout.visibility = View.GONE
                            fullApproverLayout.visibility = View.GONE
                            partialApproverLayout.visibility = View.GONE
                            observerInfoLayout.visibility = View.VISIBLE

                            payer.name.let {

                                var text = "You do not have the correct authorization to approve this payment on behalf of "
                                val builder = SpannableStringBuilder(text)
                                builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPink)),
                                    0,
                                    text.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder.setSpan(StyleSpan(Typeface.NORMAL), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                val prevPosition = text.length
                                text = it
                                builder.append(text)
                                builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPink)),
                                    prevPosition,
                                    prevPosition + text.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder.setSpan(StyleSpan(Typeface.BOLD), prevPosition, prevPosition + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                observerInfoTextView.text = builder
                            }

                            if (invoice.isSubmissionRequesting) {
                                fragment?.showProgressButton(observerRequestApprovalButton, ContextCompat.getColor(context, R.color.colorSecondaryButtonProgressBar))
                            } else {
                                fragment?.hideProgressButton(observerRequestApprovalButton)
                            }

                            fragment?.hideProgressButton(observerRequestApprovalButton, !invoice.submissionRequested)
                        } else {

                            if (invoice.isPaymentSubmitting) {
                                fragment?.showProgressButton(payerSubmitPaymentButton)
                            } else {
                                fragment?.hideProgressButton(payerSubmitPaymentButton)
                            }

                            if (invoice.hasPaidBy(payer)) {
                                payerSubmitPaymentLayout.visibility = View.GONE
                            } else {
                                payerSubmitPaymentLayout.visibility = View.VISIBLE
                                payer.customer = horseManager.customer

                                cards.clear()

                                // Get the cards info from the current user because payer information will be involved payment information.
                                val customer = HLGlobalData.me?.horseManager?.customer
                                customer?.cards?.let { it ->
                                    cards.addAll(it)
                                }

                                // TODO : Implement google pay
                                /*
                                if Stripe.deviceSupportsApplePay(), cards.first(where: {$0.id == APPLE_PAYMENT_SOURCE_ID}) == nil {
                                    let applePay: HLStripeCardModel = HLStripeCardModel()
                                    applePay.id = APPLE_PAYMENT_SOURCE_ID
                                    applePay.brand = HLStripeCardBrand.applePay
                                    applePay.last4 = ""
                                    cards.append(applePay)
                                }
                                */

                                // Set card information of payer
                                if (!cards.isNullOrEmpty()) {
                                    cardLayout.visibility = View.VISIBLE

                                    // TODO: Show apply card info
                                    /*
                                    if Stripe.deviceSupportsApplePay(), HLPaymentManager.shared.isApplePayDefault, let applePay = cards.first(where: {$0.id == APPLE_PAYMENT_SOURCE_ID}) {
                                        cardImageView.image = applePay.brand.icon
                                        cardInfoLabel.text = "\(applePay.brand.rawValue) \(applePay.last4!)"
                                    } else {}
                                     */
                                    val card =
                                        cards.firstOrNull { it.id == payer.customer?.defaultSource }
                                    if (card != null) {
                                        cardIconImageView.setImageResource(Card.getBrandIcon(card.brand))
                                        cardInfoTextView.text = card.brand + " " + card.last4
                                    } else {
                                        cardIconImageView.setImageResource(0)
                                        cardInfoTextView.text = ""
                                    }
                                } else {
                                    cardLayout.visibility = View.GONE
                                }
                            }

                            fullApproverLayout.visibility = View.GONE
                            partialApproverLayout.visibility = View.GONE
                            observerInfoLayout.visibility = View.GONE
                        }
                    }
                }
            }

        }
    }
}