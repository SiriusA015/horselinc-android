package com.horselinc.views.activities

import android.os.Bundle
import android.os.Handler
import androidx.viewpager.widget.ViewPager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.event.HLNewNotificationEvent
import com.horselinc.models.event.HLUpdateNotificationCountEvent
import com.horselinc.views.adapters.viewpager.HLMainViewPagerAdapter
import com.horselinc.views.fragments.common.HLPaymentsFragment
import kotlinx.android.synthetic.main.activity_horse_manager_main.*
import org.greenrobot.eventbus.EventBus

class HLServiceProviderMainActivity : HLMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_provider_main)

        // initialize controls
        mainViewPagerAdapter = HLMainViewPagerAdapter(supportFragmentManager, HLUserType.PROVIDER)
        viewPager.adapter = mainViewPagerAdapter
        viewPager.offscreenPageLimit = 4
        viewPager.setSwipeEnabled(false)

        // event handler
        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }

            override fun onPageSelected(position: Int) {
                bottomNavigationView.selectedItemId = when (position) {
                    0 -> R.id.menuHorses
                    1 -> R.id.menuSchedule
                    2 -> R.id.menuPayment
                    else -> R.id.menuProfile
                }
            }
        })

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menuHorses -> viewPager.currentItem = 0
                R.id.menuSchedule -> viewPager.currentItem = 1
                R.id.menuPayment -> viewPager.currentItem = 2
                R.id.menuProfile -> viewPager.currentItem = 3
            }
            return@setOnNavigationItemSelectedListener true
        }

        subscribeNewNotification()

        // handle deep link
        handleDeepLink()
    }

    override fun handleDeepLinkInvoice() {
        HLGlobalData.deepLinkInvoice?.let { deepLinkModel ->
            viewPager.currentItem = 2

            Handler().postDelayed({
                val paymentsFragment = mainViewPagerAdapter.registeredFragments[viewPager.currentItem] as? HLPaymentsFragment
                paymentsFragment?.getInvoiceDetail(deepLinkModel.invoiceId)
            }, HLConstants.DEEP_LINK_POST_TIME)
        }
        HLGlobalData.deepLinkInvoice = null
    }

    private fun subscribeNewNotification() {
        HLGlobalData.me?.serviceProvider?.let { provider ->
            HLFirebaseService.instance.subscribeNewNotification(provider.userId, object :
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

}
