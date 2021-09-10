package com.horselinc

import android.content.Context
import androidx.core.content.ContextCompat

/**
 *  Constants
 */
object PreferenceKey {
    const val FIRST_LAUNCH = "first_launch"
    const val USER = "user"
    const val USER_EMAIL = "user_email"
    const val USER_PASSWORD = "user_password"
    const val KEY_NAME = "horselinc_key"
}

object ActivityRequestCode {
    const val CAMERA = 11
    const val GALLERY = 12
    const val PLACE_AUTO_COMPLETE = 13
    const val ADD_STRIPE_ACCOUNT = 14
    const val SELECT_DATE = 15
    const val SELECT_CONTACT = 16
}

object IntentExtraKey {
    const val USER_SIGN_UP = "user_sign_up"
    const val USER_ROLE_TYPE = "user_role_type"
    const val BASE_USER_SELECT_TYPE = "base_user_select_type"
    const val BASE_USER_EXCLUDE_IDS = "base_user_exclude_ids"
    const val BASE_USER_MY_SERVICE_PROVIDER = "base_user_my_service_provider"
    const val BASE_USER_INVITE_SERVICE_PROVIDER = "base_user_invite_service_provider"
    const val CALENDAR_START_DATE = "calendar_start_date"
    const val CALENDAR_END_DATE = "calendar_end_date"
    const val CALENDAR_SELECTED_DATE = "calendar_selected_date"
    const val CALENDAR_RETURN_DATE = "calendar_return_date"
    const val CREATE_INVOICE = "create_invoice"
}

object HLConstants {
    const val PLACE_API_KEY = "AIzaSyDlIfxdHeoBsoZz0S9Vp-hj6Y_VnR5EDbY"
    const val STRIPE_PUBLISHABLE_KEY = "pk_test_SCjKq9ThYF4VfqNSDwwGtE2X"
    const val STRIPE_PUBLISHABLE_KEY_PROD = "pk_live_sFiDk5GlthNU4OF0rjCRNSSG"

    const val FIREBASE_DEFAULT_PASSWORD = "HorseLinc2019"

    val ADD_STRIPE_ACCOUNT_URL = if (BuildConfig.DEBUG) {
        "https://us-central1-horselinc-dev.cloudfunctions.net/api/stripes/accounts/authorize?userId="
    } else {
        "https://us-central1-horselinc-5b153.cloudfunctions.net/api/stripes/accounts/authorize?userId="
    }

    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.HorseLinc"

    const val LIMIT_HORSE_MANAGERS = 10L
    const val LIMIT_HORSES = 10L
    const val LIMIT_HORSE_USERS = 20L
    const val LIMIT_SERVICE_REQUESTS = 10L
    const val LIMIT_SERVICE_SHOWS = 10L
    const val LIMIT_INVOICES = 10L
    const val LIMIT_NOTIFICATIONS = 20L

    const val DEEP_LINK_POST_TIME = 300L

    val HORSE_GENDERS = listOf("None", HLHorseGenderType.MARE, HLHorseGenderType.GELDING, HLHorseGenderType.STALLION)
}


/**
 *  Enumerations
 */
object HLUserOnlineStatus {
    const val ONLINE = "online"
    const val OFFLINE = "offline"
    const val AWAY = "away"

}

object HLUserType {
    const val MANAGER = "Horse Manager"
    const val PROVIDER = "Service Provider"
}

object HLExternalAccountType {
    const val CARD = "card"
    const val BANK = "bank_account"
}

object HLPlatformType {
    const val IOS = "iOS"
    const val ANDROID = "Android"
    const val WEB = "Web"
}

object HLHorseGenderType {
    const val MARE = "Mare"
    const val GELDING = "Gelding"
    const val STALLION = "Stallion"
}

object HLSortOrder {
    const val ASC = "asc"
    const val DESC = "desc"
}

object HLBaseUserSelectType {
    const val HORSE_TRAINER = 0
    const val HORSE_OWNER = 1
    const val HORSE_LEASED_TO = 2
    const val HORSE_MANAGER = 3
    const val SERVICE_PROVIDER = 4
}

object HLHorseUserSearchType {
    const val OWNER = "owner"
    const val TRAINER = "trainer"
    const val MANAGER = "manager"
}

object HLServiceRequestStatus {
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
    const val DECLINED = "declined"
    const val COMPLETED = "completed"
    const val DELETED = "deleted"
    const val PAID = "paid"
    const val INVOICED = "invoiced"
}

object HLInvoiceStatusType {
    const val SUBMITTED = "submitted"
    const val PAID = "paid"
    const val DRAFT = "draft"
    const val FULL_PAID = "fullPaid"

    fun color(status: String?, context: Context): Int {
        return when (status) {
            DRAFT, SUBMITTED, PAID -> ContextCompat.getColor(context, R.color.colorPink)
            else -> ContextCompat.getColor(context, R.color.colorTeal)
        }

    }
}

object HLDeepLinkType {
    const val INVOICE = "invoice"
    const val INVITE = "invite"
}

enum class HLInvoiceMethodType {
    NONE, EMAIL, SMS
}

enum class HLPaymentScreenType {
    drafts, submitted, paid, outstanding, completed
}