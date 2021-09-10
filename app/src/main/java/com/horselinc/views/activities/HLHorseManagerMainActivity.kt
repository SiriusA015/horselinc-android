package com.horselinc.views.activities

import android.os.Bundle
import android.os.Handler
import androidx.viewpager.widget.ViewPager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLDeepLinkInviteModel
import com.horselinc.models.event.HLNewNotificationEvent
import com.horselinc.models.event.HLUpdateNotificationCountEvent
import com.horselinc.views.adapters.viewpager.HLMainViewPagerAdapter
import com.horselinc.views.fragments.common.HLPaymentsFragment
import com.horselinc.views.fragments.manager.HLManagerHorsesFragment
import com.horselinc.views.fragments.manager.HLManagerProfileFragment
import kotlinx.android.synthetic.main.activity_horse_manager_main.*
import org.greenrobot.eventbus.EventBus
import java.util.*

class HLHorseManagerMainActivity : HLMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horse_manager_main)

        // initialize controls
        mainViewPagerAdapter = HLMainViewPagerAdapter(supportFragmentManager, HLUserType.MANAGER)
        viewPager?.run {
            adapter = mainViewPagerAdapter
            offscreenPageLimit = 3
            setSwipeEnabled(false)

            addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) { }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }

                override fun onPageSelected(position: Int) {
                    bottomNavigationView.selectedItemId = when (position) {
                        0 -> R.id.menuHorses
                        1 -> R.id.menuPayment
                        else -> R.id.menuProfile
                    }
                }
            })
        }

        // event handler
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menuHorses -> viewPager.currentItem = 0
                R.id.menuPayment -> viewPager.currentItem = 1
                R.id.menuProfile -> viewPager.currentItem = 2
            }
            return@setOnNavigationItemSelectedListener true
        }

        subscribeNewNotification()

        // handle deep link
        handleDeepLink()
    }

    override fun handleDeepLinkInvoice() {
        HLGlobalData.deepLinkInvoice?.let { deepLinkModel ->
            handleInvoiceDetail(deepLinkModel.invoiceId)
        }
        HLGlobalData.deepLinkInvoice = null
    }

    override fun handleInvite(deepLinkModel: HLDeepLinkInviteModel) {
        if (deepLinkModel.senderType == HLUserType.PROVIDER) { // invited from service provider
            deepLinkModel.invoiceId?.let { invoiceId ->
                updateInvoice(invoiceId)
            } ?: handleManagerProfile (deepLinkModel) // profile to add service provider
        }
    }

    private fun subscribeNewNotification() {
        HLGlobalData.me?.horseManager?.let {manager ->
            HLFirebaseService.instance.subscribeNewNotification(manager.userId, object :
                ResponseCallback<QuerySnapshot> {
                override fun onSuccess(data: QuerySnapshot) {
                    EventBus.getDefault().post(HLNewNotificationEvent())

                    if (data.documentChanges.map { it.type == DocumentChange.Type.MODIFIED }.isNotEmpty()) {
                        EventBus.getDefault().post(HLUpdateNotificationCountEvent())
                    }
                }

                override fun onFailure(error: String) {
                    showErrorMessage(error)
                }
            })
        }
    }

    /**
     *  DeepLink Handlers
     */
    private fun handleInvoiceDetail (invoiceId: String) {
        viewPager.currentItem = 1
        Handler().postDelayed({
            val paymentsFragment = mainViewPagerAdapter.registeredFragments[viewPager.currentItem] as? HLPaymentsFragment
            paymentsFragment?.getInvoiceDetail(invoiceId)
        }, HLConstants.DEEP_LINK_POST_TIME)
    }

    private fun handleManagerProfile (deepLinkModel: HLDeepLinkInviteModel) {
        viewPager.currentItem = 2
        Handler().postDelayed({

            // hide progress
            val horsesFragment = mainViewPagerAdapter.registeredFragments[0] as? HLManagerHorsesFragment
            horsesFragment?.hideProgressDialog()

            // handle service provider
            val profileFragment = mainViewPagerAdapter.registeredFragments[viewPager.currentItem] as? HLManagerProfileFragment
            profileFragment?.handleInvite (deepLinkModel)

        }, HLConstants.DEEP_LINK_POST_TIME)
    }

    private fun updateInvoice (invoiceId: String) {
        val updateData = hashMapOf<String, Any>(
            "updatedAt" to Date().time
        )

        HLFirebaseService.instance.updateInvoice(invoiceId, updateData, object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                handleInvoiceDetail(invoiceId)
            }

            override fun onFailure(error: String) {
                showErrorMessage(error)
            }
        })
    }
}
