package com.horselinc

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.*


object HLGlobalData {

    var me: HLUserModel? = null
        set(value) {
            field = value
            App.preference.put(PreferenceKey.USER, Gson().toJson(value))
        }

    var paymentAccount: HLPaymentAccountModel? = null

    var token: String? = null

    lateinit var settings: HLSettingsModel

    var isCreateInvoice = false     // will be used to block backspace in create invoice screen

    var deepLinkInvoice: HLDeepLinkInvoiceModel? = null
    var deepLinkInvite: HLDeepLinkInviteModel? = null

    init {
        me = when {
            App.preference.contains(PreferenceKey.USER) -> {
                val value = App.preference.get(PreferenceKey.USER, "")
                Gson().fromJson(value, HLUserModel::class.java)
            }
            else -> null
        }

        // get firebase token
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                token = task.result?.token
                token?.let {
                    HLFirebaseService.instance.registerToken(it, object: ResponseCallback<String> {})
                }
            })
    }
}