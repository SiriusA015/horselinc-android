package com.horselinc.models.data

import java.util.*

data class HLPaymentModel (
    var uid: String = "",
    var invoiceId: String = "",
    var serviceProviderId: String = "",
    var payerId: String = "",
    var payerApproverId: String? = null,
    var amount: Double = 0.0,
    var tip: Double = 0.0,
    @field: JvmField var isPaidOutsideApp: Boolean = false,
    var createdAt: Long = Calendar.getInstance().timeInMillis
)