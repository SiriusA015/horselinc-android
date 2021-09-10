package com.horselinc.views.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLDeepLinkInviteModel
import com.horselinc.models.data.HLDeepLinkInvoiceModel
import com.horselinc.models.data.HLSettingsModel
import com.horselinc.models.data.HLUserModel
import io.branch.referral.Branch

class HLSplashActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()

        // Branch init
        Branch.getInstance().initSession({ referringParams, error ->
            if (error == null) {
                if (referringParams.has("link_type")) {
                    when (referringParams["link_type"]) {
                        HLDeepLinkType.INVOICE -> {
                            HLGlobalData.deepLinkInvoice = Gson().fromJson(referringParams.toString(), HLDeepLinkInvoiceModel::class.java)
                        }
                        HLDeepLinkType.INVITE -> {
                            HLGlobalData.deepLinkInvite = Gson().fromJson(referringParams.toString(), HLDeepLinkInviteModel::class.java)
                        }
                    }
                }
            } else {
                Log.e("BRANCH SDK", error.message)
            }
        }, this.intent.data, this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide status bar
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // set content view
        setContentView(R.layout.activity_splash)

        // get settings
        getSettings ()
    }

    private fun getSettings () {
        HLFirebaseService.instance.getSettings(object: ResponseCallback<HLSettingsModel> {
            override fun onSuccess(data: HLSettingsModel) {
                HLGlobalData.settings = data

                // get user info
                getUserInfo ()
            }

            override fun onFailure(error: String) {
                showErrorMessage(error)

                // get user info
                getUserInfo()
            }
        })
    }

    private fun getUserInfo () {
        val userId = HLGlobalData.me?.uid ?: ""
        if (userId.isEmpty()) {
            finishSplash ()
        } else {
            HLFirebaseService.instance.getUser(userId, object: ResponseCallback<HLUserModel> {
                override fun onSuccess(data: HLUserModel) {
                    HLGlobalData.me = data
                    finishSplash ()
                }

                override fun onFailure(error: String) {
                    showErrorMessage(error)
                    finishSplash ()
                }
            })
        }
    }

    private fun finishSplash () {

        val isFirstLaunch = App.preference.get(PreferenceKey.FIRST_LAUNCH, true)
        val userId = if (!HLFirebaseService.instance.isAuthorized) "" else HLGlobalData.me?.uid ?: ""
        
        val cls = when {
            isFirstLaunch -> HLIntroActivity::class.java                                                        // intro activity
            userId.isEmpty() -> HLAuthActivity::class.java                                                      // auth activity
            HLGlobalData.me?.type == null -> HLUserRoleActivity::class.java                                     // select user role activity
            HLGlobalData.me?.type == HLUserType.MANAGER -> HLHorseManagerMainActivity::class.java               // manager activity
            else -> HLServiceProviderMainActivity::class.java                                                   // provider activity
        }

        startActivity(Intent(this, cls))
        finish()
    }
}
