package com.horselinc.models.data

import com.google.firebase.firestore.Exclude

data class HLServiceProviderServiceModel (
    var uid: String = "",
    var userId: String = "",
    var service: String = "",
    var rate: Float = 0.0f,
    var quantity: Int = 1
) {

    @set: Exclude @get: Exclude var selected: Boolean = false
}