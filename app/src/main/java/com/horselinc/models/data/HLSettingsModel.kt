package com.horselinc.models.data

import com.google.gson.annotations.SerializedName


/**
 *  Setting Model
 */
data class HLSettingsModel (
    var urls: HLUrlsModel = HLUrlsModel(),
    var phones: HLContactModel = HLContactModel(),
    var emails: HLContactModel = HLContactModel(),
    @SerializedName ("application-fee") var applicationFee: Double = 5.0
)


/**
 *  Url Model
 */
data class HLUrlsModel (
    var terms: String = "",
    var privacy: String = ""
)


/**
 *  Contact Model
 */
data class HLContactModel (
    var contact: String = ""
)