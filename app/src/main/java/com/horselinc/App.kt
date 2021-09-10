package com.horselinc

import android.app.Application
import android.content.IntentFilter
import com.google.android.libraries.places.api.Places
import com.horselinc.utils.NetworkChangeReceiver
import com.horselinc.utils.PreferenceUtil
import com.jakewharton.threetenabp.AndroidThreeTen
import com.stripe.android.PaymentConfiguration
import io.branch.referral.Branch

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
        lateinit var preference: PreferenceUtil
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        preference = PreferenceUtil(applicationContext)

        Places.initialize(applicationContext, HLConstants.PLACE_API_KEY)

        if (BuildConfig.DEBUG) {
            PaymentConfiguration.init(HLConstants.STRIPE_PUBLISHABLE_KEY)
        } else {
            PaymentConfiguration.init(HLConstants.STRIPE_PUBLISHABLE_KEY_PROD)
        }


        AndroidThreeTen.init(this)

        // Branch logging for debugging
        Branch.enableDebugMode()

        // Initialize the Branch object
        Branch.getAutoInstance(this)

        registerReceiver(
            NetworkChangeReceiver(),
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
    }
}