package com.horselinc.views.activities

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.models.data.HLDeepLinkInviteModel
import com.horselinc.models.event.HLNetworkChangeEvent
import com.horselinc.showErrorMessage
import com.horselinc.showInfoMessage
import com.horselinc.utils.NetworkChangeListener
import com.horselinc.utils.NetworkUtil
import com.horselinc.views.adapters.viewpager.HLMainViewPagerAdapter
import kotlinx.android.synthetic.main.activity_horse_manager_main.*
import org.greenrobot.eventbus.EventBus

open class HLMainActivity : AppCompatActivity(), NetworkChangeListener {

    lateinit var mainViewPagerAdapter: HLMainViewPagerAdapter

    override fun onResume() {
        super.onResume()
        NetworkUtil.addNetworkChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        NetworkUtil.removeNetworkChangeListener()
    }

    override fun onDestroy() {
        HLFirebaseService.instance.removeListeners()
        NetworkUtil.removeNetworkChangeListener()
        super.onDestroy()
    }

    override fun onBackPressed() {
        val curFrag = mainViewPagerAdapter.getRegisteredFragment(viewPager.currentItem)
        if (curFrag.childFragmentManager.backStackEntryCount > 0) {
            if (HLGlobalData.isCreateInvoice) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Are you sure you want to go back? All your changes will be lost.")
                    .setCancelable(true)
                    .setPositiveButton("OK") { _, _ -> curFrag.childFragmentManager.popBackStack() }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                curFrag.childFragmentManager.popBackStack()
            }
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Deep Link Handler
     */
    open fun handleDeepLink () {

        // handle invite
        handleDeepLinkInvite ()

        // handle invoice
        handleDeepLinkInvoice ()
    }

    open fun handleDeepLinkInvoice () {}

    private fun handleDeepLinkInvite () {
        // invite handler
        HLGlobalData.deepLinkInvite?.let { deepLinkModel ->
            // handle invite
            handleInvite (deepLinkModel)
        }
        HLGlobalData.deepLinkInvite = null
    }

    open fun handleInvite (deepLinkModel: HLDeepLinkInviteModel) {}

    override fun onChangedNetworkStatus(networkState: Int) {
        if (networkState == NetworkUtil.NETWORK_DISCONNECTED) {
            showErrorMessage(getString(R.string.msg_err_network))
        } else {
            showInfoMessage(getString(R.string.msg_info_network))
        }
        EventBus.getDefault().post(HLNetworkChangeEvent(networkState))
    }
}
