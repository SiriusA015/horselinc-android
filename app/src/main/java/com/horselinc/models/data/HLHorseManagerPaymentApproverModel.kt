package com.horselinc.models.data

/**
 *  Horse Manager Payment Approver Model
 */
data class HLHorseManagerPaymentApproverModel (
    var uid: String = "",
    var creatorId: String = "",
    var amount: Float? = null   // if null, it means unlimited
): HLHorseManagerModel()