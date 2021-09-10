package com.horselinc.models.data

import com.google.gson.annotations.SerializedName
import com.horselinc.toCurrencyString
import java.text.NumberFormat
import java.util.*


/**
 *  Service Provider Model
 */
open class HLServiceProviderModel (
    var account: HLStripeAccountModel? = null,
    var rates: ArrayList<HLServiceProviderServiceModel>? = null
): HLBaseUserModel()

/**
 *  Stripe Account Model
 */
data class HLStripeAccountModel (
    var id: String = "",
    var businessType: String? = null,
    var email: String? = null,
    var country: String = "",
    var externalAccounts: ArrayList<HLStripeExternalAccountModel>? = null
)


/**
 *  Stripe External Account Model
 */
data class HLStripeExternalAccountModel (
    var `object`: String = "",
    var id: String = "",
    var country: String = "",
    var currency: String = "",
    var last4: String = "",
    var defaultForCurrency: Boolean = false,
    var bankName: String = "",
    var routingNumber: String = "",
    var brand: String = "",
    var expMonth: Int = 0,
    var expYear: Int = 0
)

/**
 *  Instant Payout
 */
data class HLPaymentAccountModel (
    @field: JvmField @SerializedName("instant") var isInstant: Boolean = false,
    var balance: HLPaymentBalanceModel = HLPaymentBalanceModel ()
) {
    fun getCurrencyString (): String {
        return (balance.amount / 100).toCurrencyString(balance.currency)
    }
}

data class HLPaymentBalanceModel (
    var amount: Float = 0.0f,
    var currency: String = "usd"
)