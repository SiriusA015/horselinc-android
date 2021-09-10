package com.horselinc.models.data

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Exclude
import com.horselinc.HLGlobalData
import com.horselinc.HLServiceRequestStatus
import java.util.*
import kotlin.collections.ArrayList

data class HLServiceRequestModel (
    var uid: String = "",
    var horseId: String = "",
    var horseBarnName: String = "",
    var horseDisplayName: String = "",
    var showId: String? = null,
    var competitionClass: String? = null,
    var serviceProviderId: String = "",
    var assignerId: String? = null,
    var services: ArrayList<HLServiceProviderServiceModel> = ArrayList(),
    var instruction: String? = null,
    var status: String = HLServiceRequestStatus.PENDING,
    @field: JvmField var isCustomRequest: Boolean? = false,
    @field: JvmField var isDeletedFromInvoice: Boolean = false,
    var dismissedBy: ArrayList<String> = ArrayList(),
    var creatorId: String = "",
    var shareInfo: HLInvoiceShareInfo? = null,
    var requestDate: Long = Calendar.getInstance().timeInMillis,
    var createdAt: Long = Calendar.getInstance().timeInMillis,
    var updatedAt: Long = Calendar.getInstance().timeInMillis
) {
    @get: Exclude var horse: HLHorseModel? = null
    @get: Exclude var show: HLServiceShowModel? = null
    @get: Exclude var serviceProvider: HLServiceProviderModel? = null
    @get: Exclude var assigner: HLServiceProviderModel? = null
    @get: Exclude var payer: HLHorseManagerModel? = null

    @set: Exclude @get: Exclude var diffType: DocumentChange.Type? = null

    @Exclude fun getRequestDateCalendar (): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = requestDate
        return calendar
    }

    fun copy (): HLServiceRequestModel = HLServiceRequestModel().apply {
        this.uid = this@HLServiceRequestModel.uid
        this.horseId = this@HLServiceRequestModel.horseId
        this.horseBarnName = this@HLServiceRequestModel.horseBarnName
        this.horseDisplayName = this@HLServiceRequestModel.horseDisplayName
        this.showId = this@HLServiceRequestModel.showId
        this.competitionClass = this@HLServiceRequestModel.competitionClass
        this.serviceProviderId = this@HLServiceRequestModel.serviceProviderId
        this.assignerId = this@HLServiceRequestModel.assignerId
        this.services = this@HLServiceRequestModel.services
        this.instruction = this@HLServiceRequestModel.instruction
        this.status = this@HLServiceRequestModel.status
        this.isCustomRequest = this@HLServiceRequestModel.isCustomRequest
        this.isDeletedFromInvoice = this@HLServiceRequestModel.isDeletedFromInvoice
        this.dismissedBy = this@HLServiceRequestModel.dismissedBy
        this.creatorId = this@HLServiceRequestModel.creatorId
        this.shareInfo = this@HLServiceRequestModel.shareInfo
        this.requestDate = this@HLServiceRequestModel.requestDate
        this.createdAt = this@HLServiceRequestModel.createdAt
        this.updatedAt = this@HLServiceRequestModel.updatedAt
        this.horse = this@HLServiceRequestModel.horse
        this.show = this@HLServiceRequestModel.show
        this.serviceProvider = this@HLServiceRequestModel.serviceProvider
        this.assigner = this@HLServiceRequestModel.assigner
        this.payer = this@HLServiceRequestModel.payer
    }

    @get:Exclude
    val isMainServiceProvider: Boolean
        get() = (HLGlobalData.me?.uid == serviceProviderId) && (HLGlobalData.me?.uid != assignerId)

    @get:Exclude
    val isReassignedServiceProvider: Boolean
        get() = (HLGlobalData.me?.uid != serviceProviderId) && (HLGlobalData.me?.uid == assignerId)
}