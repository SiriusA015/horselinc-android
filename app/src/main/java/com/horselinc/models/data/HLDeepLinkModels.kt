package com.horselinc.models.data

import com.google.gson.annotations.SerializedName
import com.horselinc.HLDeepLinkType
import com.horselinc.HLPlatformType
import com.horselinc.HLUserType

data class HLDeepLinkInvoiceModel(
    @SerializedName("link_type") var linkType: String = HLDeepLinkType.INVOICE,
    @SerializedName("invoice_id") var invoiceId: String = ""
)

data class HLDeepLinkInviteModel(
    @SerializedName("link_type") var linkType: String = HLDeepLinkType.INVITE,
    @SerializedName("sender_id") var senderId: String = "",
    @SerializedName("sender_type") var senderType: String = HLUserType.MANAGER,
    @SerializedName("invoice_id") var invoiceId: String? = null,
    @SerializedName("horse_id") var horseId: String? = null,
    var platform: String = HLPlatformType.ANDROID
)