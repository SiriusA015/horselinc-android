package com.horselinc.models.event

import com.google.firebase.firestore.DocumentChange
import com.horselinc.HLPaymentScreenType
import com.horselinc.models.data.*

data class HLSelectBaseUserEvent (
    val selectType: Int,
    val baseUser: HLBaseUserModel
)

data class HLRefreshHorsesEvent (val filter: HLHorseFilterModel? = null)

data class HLNewNotificationEvent(val data: Any? = null)
class HLUpdateNotificationCountEvent

data class HLSearchEvent(val data: Any? = null)

data class HLEventPaymentReloadInvoices(val screenType: HLPaymentScreenType)
data class HLEventPaymentGroupByChanged(val isHorseGroup: Boolean)
data class HLEventPaymentInvoicesUpdated(val invoiceIds: List<String>, val statuses: List<DocumentChange.Type>)
data class HLEventServiceRequestsCompleted(val requestIds: List<String>, val statuses: List<DocumentChange.Type>)
data class HLEventPaymentInvoiceDeleted(val invoiceId: String)
data class HLEventPaymentInvoiceRefresh(val invoice: HLInvoiceModel)
data class HLEventPaymentDraftDeleted(val invoice: HLInvoiceModel)


data class HLUpdateHorsePrivateNoteEvent(val data: String)
data class HLEventPaymentInvoiceCreated(val invoice: HLInvoiceModel)
data class HLEventPaymentInvoiceMarkAsPaid(val invoice: HLInvoiceModel)
data class HLEventPaymentInvoiceUpdated(val invoiceId: String)

data class HLEventInvoiceEditInvoiceButtonClicked(val data: Any? = null)
data class HLEventInvoiceMarkAsPaidButtonClicked(val data: Any? = null)
data class HLEventInvoiceSendPaymentReminderButtonClicked(val data: Any? = null)
data class HLEventInvoiceCardChangeButtonClicked(val payer: HLHorseManagerModel, val cards: ArrayList<HLStripeCardModel>)
data class HLEventInvoiceFullApproverCardChangeButtonClicked(val payer: HLHorseManagerModel, val cards: ArrayList<HLStripeCardModel>)
data class HLEventInvoicePayerSubmitPaymentButtonClicked(val payer: HLHorseManagerModel)
data class HLEventInvoiceFullApproverSubmitPaymentButtonClicked(val payer: HLHorseManagerModel)
data class HLEventInvoicePartialApproverRequestApprovalButtonClicked(val payer: HLHorseManagerModel)
data class HLEventInvoiceObserverRequestApprovalButtonClicked(val payer: HLHorseManagerModel)
data class HLEventInvoiceHorseProfileClicked(val horse: HLHorseModel)

data class HLProviderFilterEvent (val data: HLServiceRequestFilterModel?)

class HLUpdateUserEvent

data class HLNetworkChangeEvent (val networkState: Int)

class HLSubmitInstantPayoutEvent
