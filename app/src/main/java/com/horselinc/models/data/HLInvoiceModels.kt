package com.horselinc.models.data

import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.horselinc.HLGlobalData
import com.horselinc.HLInvoiceStatusType
import com.horselinc.HLUserType
import com.horselinc.withStripeFee
import java.util.*
import kotlin.collections.ArrayList

data class HLInvoiceModel (
    var uid: String = "",
    var requestIds: ArrayList<String> = ArrayList(),
    var tip: Double? = null,
    var createdAt: Long = Calendar.getInstance().timeInMillis,
    var status: String = "",
    var name: String = "",
    var paidAt: Long = Calendar.getInstance().timeInMillis,
    var listenerUsers: ArrayList<HLListenerUserModel>? = null,
    var shareInfo: HLInvoiceShareInfo? = null,
    var updatedAt: Long = Calendar.getInstance().timeInMillis
) {
    @get: Exclude var payments: ArrayList<HLPaymentModel>? = null
    @get: Exclude var requests: ArrayList<HLServiceRequestModel>? = null
    @get: Exclude var amount: Double? = null
    @get: Exclude var payers: ArrayList<HLHorseManagerModel>? = null
    @get: Exclude var paymentApprovers: ArrayList<HLHorseManagerPaymentApproverModel>? = null

    // To be used on mobile app, not on server.
    @get: Exclude var _serviceProvider: HLServiceProviderModel? = null
    @get: Exclude var _horseManager: HLHorseManagerModel? = null
    @get: Exclude var _horse: HLHorseModel? = null

    @get: Exclude var markAsPaid: Boolean = false
    @get: Exclude var isMarkingAsPaid: Boolean = false

    // To be used for an assigner to send an notification to a service provider
    @get: Exclude var submissionRequested: Boolean = false
    @get: Exclude var isSubmissionRequesting: Boolean = false

    // To be used for an service provider to send payment requests to payers
    @get: Exclude var paymentRequested: Boolean = false
    @get: Exclude var isPaymentRequesting: Boolean = false

    // To be used for a main service provider to send an notification to payers when creates an invoice
    @get: Exclude var invoiceCreated: Boolean = false
    @get: Exclude var isInvoiceCreating: Boolean = false

    @get: Exclude var paymentSubmitted: Boolean = false
    @get: Exclude var isPaymentSubmitting: Boolean = false

    fun copy (): HLInvoiceModel {
        val gson = Gson()
        val json = gson.toJson(this@HLInvoiceModel)
        return gson.fromJson<HLInvoiceModel>(json, HLInvoiceModel::class.java)
    }

    @Exclude fun getServiceProvider(): HLServiceProviderModel? {
        if (null != _serviceProvider) {
            return _serviceProvider
        }

        _serviceProvider = requests?.firstOrNull()?.serviceProvider
        return _serviceProvider
    }

    @Exclude fun getHorseManager(): HLHorseManagerModel? {
        if (null != _horseManager) {
            return _horseManager
        }

        _horseManager = requests?.firstOrNull()?.horse?.trainer
        return _horseManager
    }

    @Exclude fun getHorse(): HLHorseModel? {
        if (null != _horse) {
            return _horse
        }

        _horse = requests?.firstOrNull()?.horse
        return _horse
    }

    @Exclude fun isMainServiceProvider(): Boolean {
        if (HLGlobalData.me?.type != HLUserType.PROVIDER) {
            return false
        }

        if (getServiceProvider()?.userId != null
            && HLGlobalData.me?.serviceProvider?.userId != null
            && getServiceProvider()?.userId == HLGlobalData.me?.serviceProvider?.userId) {
                return true
        }
        return false
    }

    @Exclude fun isPaymentApprover(user: HLBaseUserModel): Boolean = when {
//        payers?.firstOrNull { it.userId == user.userId } != null -> false // If the user is a payer for this invoice, he can't become an approver.
        paymentApprovers?.firstOrNull { it.userId == user.userId} != null -> true
        else -> false
    }

    @Exclude fun fullPaidAt(): Long?  = if (status != HLInvoiceStatusType.FULL_PAID) {
        null
    } else {
        paidAt
    }


    @Exclude fun getPayerTotalAmount(payer: HLHorseManagerModel): Double {
        val total = payer.percentage.toDouble() / 100 * (amount ?: 0.0)
        val fee = payer.percentage.toDouble() / 100 * (tip ?: 0.0)
        return (total + fee).withStripeFee
    }

    @Exclude fun hasPaidBy(payer: HLHorseManagerModel): Boolean = getPaymentOf(payer) != null

    @Exclude fun getPaymentOf(payer: HLHorseManagerModel): HLPaymentModel? {
        if (payments == null) {
            return null
        }

        payments?.let { payments ->
            for (payment in payments) {
                if (payment.payerId == payer.userId) {
                    return payment
                }
            }
        }

        return null
    }

    @Exclude fun isPayableBehalfOf(payer: HLHorseManagerModel): Boolean {
        val managerId = HLGlobalData.me?.horseManager?.userId
        if (paymentApprovers == null && managerId == null) {
            return false
        }

        val approver = paymentApprovers?.firstOrNull{ it.userId == HLGlobalData.me?.horseManager?.userId && it.creatorId == payer.userId } ?: return false
        val amount = approver.amount ?: return true
        return getPayerTotalAmount(payer) <= amount
    }

    @Exclude fun getApprovalAmountFor(payer: HLHorseManagerModel): Double? {

        val managerId = HLGlobalData.me?.horseManager?.userId
        if (paymentApprovers == null && managerId == null) {
            return null
        }

        val approver = paymentApprovers?.firstOrNull() { it.userId == HLGlobalData.me?.horseManager?.userId && it.creatorId == payer.userId }
            ?: return null

        return approver.amount?.toDouble()
    }

    @Exclude fun isPayer(payerId: String): Boolean = when {
        HLGlobalData.me?.type != HLUserType.MANAGER -> false
        HLGlobalData.me?.horseManager?.userId != null -> HLGlobalData.me?.horseManager?.userId == payerId
        else -> false
    }

    @Exclude fun getPaidAmount(): Double {
        val payments = payments ?: return 0.0
        return payments.toArray().map { (it as HLPaymentModel).amount + it.tip }.reduce { acc, d -> acc + d }
    }

    @Exclude fun getOutstandingBalance(): Double = (amount ?: 0.0) + (tip ?: 0.0) - getPaidAmount()
}

data class HLInvoiceShareInfo (
    var name: String? = null,
    var phone: String? = null,
    var email: String? = null
)