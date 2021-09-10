package com.horselinc.models.data

import com.google.firebase.firestore.Exclude
import java.util.ArrayList

/**
 *  Horse Manager Model
 */
open class HLHorseManagerModel (
    var barnName: String = "",
    var percentage: Float = 100.0f,
    var customer: HLStripeCustomerModel? = null
): HLBaseUserModel() {
    @set: Exclude @get: Exclude var userType: String = ""
    @get: Exclude var email: String = ""
}


/**
 *  Stripe Customer Model
 */
data class HLStripeCustomerModel (
    var id: String = "",
    var defaultSource: String? = null,
    var description: String? = null,
    var email: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var cards: ArrayList<HLStripeCardModel>? = null
)


/**
 *  Stripe Card Model
 */
data class HLStripeCardModel (
    var id: String = "",
    var brand: String = "",
    var last4: String = "",
    var expMonth: Int = 0,
    var expYear: Int = 0
)