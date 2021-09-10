package com.horselinc.views.fragments.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLInvoiceModel
import com.horselinc.models.event.HLEventPaymentGroupByChanged
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.activities.HLHorseManagerMainActivity
import com.horselinc.views.activities.HLServiceProviderMainActivity
import com.horselinc.views.adapters.viewpager.HLPaymentViewPagerAdapter
import com.horselinc.views.customs.HLInvoiceGroupTypePopUpView
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLInvoiceGroupTypePopUpViewListener
import org.greenrobot.eventbus.EventBus


/** Created by jcooperation0137 on 2019-08-26.
 */


class HLPaymentsFragment: HLBaseFragment() {

    // MARK: - Properties
    private var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null
    var toolBar: Toolbar? = null
    var exportImageView: ImageView? = null
    var moreImageView: ImageView? = null
    var isHorseGrouped = false

    lateinit var pagerAdapter: HLPaymentViewPagerAdapter
    var groupTypePopUpWindow: PopupWindow? = null
    var groupTypePopUpView: HLInvoiceGroupTypePopUpView? = null

    // MARK: - Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_payments, container, false)

            toolBar = rootView?.findViewById(R.id.toolBar)
            viewPager = rootView?.findViewById(R.id.viewPager)
            tabLayout = rootView?.findViewById(R.id.tabLayout)
            moreImageView = rootView?.findViewById(R.id.moreImageView)
            exportImageView = rootView?.findViewById(R.id.exportImageView)

            initControls()
        }

        return rootView
    }

    // MARK: - Actions


    // MARK: - Functions
    fun initControls() {

        if (HLGlobalData.me?.type == HLUserType.MANAGER) {
            (activity as HLHorseManagerMainActivity).setSupportActionBar(toolBar)
        } else {
            (activity as HLServiceProviderMainActivity).setSupportActionBar(toolBar)
        }
        context?.let {
            toolBar?.setTitleTextColor(ContextCompat.getColor(it, R.color.colorWhite))
            toolBar?.setTitle(R.string.payments)
        }


        HLGlobalData.me?.type?.let { type ->
            pagerAdapter = HLPaymentViewPagerAdapter(childFragmentManager, context, type)
            viewPager?.adapter = pagerAdapter
            if (type == HLUserType.MANAGER) {
                viewPager?.offscreenPageLimit = 2
            } else {
                viewPager?.offscreenPageLimit = 3
            }

            tabLayout?.setupWithViewPager(viewPager)

            tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(p0: TabLayout.Tab?) {

                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {

                }

                override fun onTabSelected(p0: TabLayout.Tab?) {
                    tabLayout?.selectedTabPosition?.let { position ->
                        if (type == HLUserType.PROVIDER) {
                            moreImageView?.visibility = View.VISIBLE
                            when (position) {
                                0 -> moreImageView?.isEnabled = true
                                1 -> moreImageView?.isEnabled = false
                                else -> moreImageView?.isEnabled = false
                            }
                        }
                    }
                }
            })

        }

        // Group By PopUp View Setting
        groupTypePopUpView = HLInvoiceGroupTypePopUpView(context)
        groupTypePopUpView?.setOnClickListener(object : HLInvoiceGroupTypePopUpViewListener {
            override fun onClickHorseButton() {
                if (isHorseGrouped) return

                isHorseGrouped = true

                // Enable / Disable group type buttons.
                groupTypePopUpView?.updateButtonState(isHorseGrouped)

                // Get data from server.
                EventBus.getDefault().post(EventBus.getDefault().post(HLEventPaymentGroupByChanged(isHorseGrouped)))

                groupTypePopUpWindow?.dismiss()
            }

            override fun onClickUserButton() {
                if (!isHorseGrouped) return
                isHorseGrouped = false

                // Enable / Disable group type buttons.
                groupTypePopUpView?.updateButtonState(isHorseGrouped)

                // Get data from server.
                EventBus.getDefault().post(EventBus.getDefault().post(HLEventPaymentGroupByChanged(isHorseGrouped)))

                groupTypePopUpWindow?.dismiss()
            }
        })
        groupTypePopUpWindow = PopupWindow(groupTypePopUpView, ResourceUtil.dpToPx(144), ResourceUtil.dpToPx(102))
        groupTypePopUpWindow?.apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Action buttons setting
        moreImageView?.setOnClickListener {
            if (groupTypePopUpWindow?.isShowing == true) {
                groupTypePopUpWindow?.dismiss()
                return@setOnClickListener
            }

            groupTypePopUpWindow?.showAsDropDown(it)
        }

        if (HLGlobalData.me?.type == HLUserType.MANAGER) {
            moreImageView?.visibility = View.GONE
        } else {
            moreImageView?.visibility = View.VISIBLE
        }

        moreImageView?.drawable?.let {
            DrawableCompat.setTint(it, Color.WHITE)
        }


        exportImageView?.setOnClickListener {
            // Show the export screen
            HLGlobalData.me?.type?.let {
                replaceFragment(HLExportPaymentFragment(it), R.id.mainContainer)
            }
        }
    }

    /**
     *  Transfer To Invoice Detail
     */
    fun getInvoiceDetail (invoiceId: String) {
        HLFirebaseService.instance.getInvoice(invoiceId, object: ResponseCallback<HLInvoiceModel> {
            override fun onSuccess(data: HLInvoiceModel) {


                transferToInvoiceDetail(data)

            }

            override fun onFailure(error: String) {
                showErrorMessage(error)
            }
        })
    }

    private fun transferToInvoiceDetail (invoice: HLInvoiceModel) {
        HLGlobalData.me?.type?.let { userType ->
            val currentTabIndex = when (invoice.status) {
                HLInvoiceStatusType.PAID, HLInvoiceStatusType.SUBMITTED -> {
                    if (userType == HLUserType.MANAGER) 0 else 1
                }
                HLInvoiceStatusType.FULL_PAID -> {
                    if (userType == HLUserType.MANAGER) 1 else 2
                }
                else -> null
            }

            currentTabIndex?.let { tabIndex ->
                viewPager?.currentItem = tabIndex
                val fragment = pagerAdapter.registeredFragments[tabIndex] as? HLPaymentFragment
                fragment?.replaceFragment(HLInvoiceDetailFragment(invoice), R.id.mainContainer)
            }
        }
    }

}
