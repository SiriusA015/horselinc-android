package com.horselinc.views.fragments.provider


import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceRequestFilterModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.event.HLProviderFilterEvent
import com.horselinc.views.adapters.viewpager.HLProviderScheduleViewPagerAdapter
import com.horselinc.views.fragments.HLBaseFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList


class HLProviderSchedulesFragment : HLBaseFragment() {

    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager? = null

    private var filter: HLServiceRequestFilterModel? = HLServiceRequestFilterModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_schedules, container, false)

            EventBus.getDefault().register(this)

            initControls ()

            addServiceRequestListener ()
        }
        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    fun onReceivedFilterEvent (event: HLProviderFilterEvent) {
        try {
            filter = event.data
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        tabLayout = rootView?.findViewById(R.id.tabLayout)
        viewPager = rootView?.findViewById(R.id.viewPager)

        // initialize view pager & tabLayout
        viewPager?.run {
            adapter = HLProviderScheduleViewPagerAdapter(childFragmentManager, activity)
            offscreenPageLimit = 2

            addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {}
            })
        }

        tabLayout?.run {
            setupWithViewPager(viewPager)
        }


        // event handlers
        rootView?.findViewById<ImageView>(R.id.filterImageView)?.setOnClickListener { onClickFilter () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickFilter () {
        replaceFragment(HLProviderScheduleFilterFragment(filter?.copy()), R.id.mainContainer)
    }

    /**
     *  Service Request Handlers
     */
    private fun addServiceRequestListener () {
        HLGlobalData.me?.uid?.let { userId ->
            HLFirebaseService.instance.addServiceRequestListenerForProvider(userId, object: ResponseCallback<List<HLServiceRequestModel>> {

                override fun onSuccess(data: List<HLServiceRequestModel>) {

                    val currentRequests = ArrayList<HLServiceRequestModel>()
                    val pastRequests = ArrayList<HLServiceRequestModel>()

                    data.forEach { request ->
                        if (DateUtils.isToday(request.requestDate) || request.requestDate >= Calendar.getInstance().timeInMillis) {
                            currentRequests.add(request)
                        } else {
                            pastRequests.add(request)
                        }
                    }

                    val adapter = (viewPager?.adapter as HLProviderScheduleViewPagerAdapter)
                    if (currentRequests.isNotEmpty()) {
                        (adapter.registeredFragments[0] as HLProviderScheduleFragment).processRequests (currentRequests)
                    }
                    if (pastRequests.isNotEmpty()) {
                        (adapter.registeredFragments[1] as HLProviderScheduleFragment).processRequests (pastRequests)
                    }

                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }
}
