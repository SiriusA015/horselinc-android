package com.horselinc.views.fragments.provider


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.DocumentChange
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLServiceRequestFilterModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.event.HLProviderFilterEvent
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLProviderBaseScheduleAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.common.HLPublicHorseProfileFragment
import com.horselinc.views.fragments.common.HLPublicUserProfileFragment
import com.horselinc.views.fragments.manager.HLManagerScheduleServiceFragment
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.jude.easyrecyclerview.decoration.SpaceDecoration
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import java.util.*


open class HLProviderScheduleFragment : HLBaseFragment() {

    var scheduleRecyclerView: EasyRecyclerView? = null
    lateinit var scheduleHeaderView: RecyclerArrayAdapter.ItemView

    var filter: HLServiceRequestFilterModel? = HLServiceRequestFilterModel()

    var isLoading = false
    var shouldLoadMore = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_schedule, container, false)

            EventBus.getDefault().register(this)

            initControls ()
        }
        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun handleInternetAvailable() {
        super.handleInternetAvailable()
//        scheduleRecyclerView?.swipeToRefresh?.isEnabled = true
    }

    override fun handleInternetUnavailable() {
        super.handleInternetUnavailable()
        scheduleRecyclerView?.setRefreshing(false)
//        scheduleRecyclerView?.swipeToRefresh?.isEnabled = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedFilterEvent (event: HLProviderFilterEvent) {
        try {
            filter = event.data
            getServiceRequests(true)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     *  Initialize Handlers
     */
    open fun initControls () {

        scheduleHeaderView = object: RecyclerArrayAdapter.ItemView {
            @SuppressLint("SetTextI18n")
            override fun onBindView(headerView: View?) {
                val updateText = "Last update: ${Calendar.getInstance().time.formattedString("dd/MM/YYYY h:mm a")}"
                headerView?.findViewById<TextView>(R.id.dateTextView)?.text = updateText
            }

            override fun onCreateView(parent: ViewGroup?): View {
                return LayoutInflater.from(context).inflate(R.layout.item_provider_schedule_header, parent, false)
            }
        }

        scheduleRecyclerView = rootView?.findViewById(R.id.scheduleRecyclerView)
        scheduleRecyclerView?.run {
            setLayoutManager(LinearLayoutManager(activity))
            addItemDecoration(SpaceDecoration(ResourceUtil.dpToPx(8)))

            setRefreshListener {
                if (isNetworkConnected) {
                    getServiceRequests(true)
                } else {
                    setRefreshing(false)
                }
            }
        }
    }

    open fun getServiceRequests (isRefresh: Boolean) {}


    /**
     *  Event Handlers
     */
    fun onClickHorse (horse: HLHorseModel) {
        replaceFragment(HLPublicHorseProfileFragment(horse), R.id.mainContainer)
    }

    fun onClickUser (user: HLBaseUserModel) {
        replaceFragment(HLPublicUserProfileFragment(user), R.id.mainContainer)
    }

    fun assignJob (request: HLServiceRequestModel?) {
        request?.copy()?.let {
            it.status = HLServiceRequestStatus.PENDING
            replaceFragment(HLManagerScheduleServiceFragment(it, true), R.id.mainContainer)
        }
    }

    fun addService (request: HLServiceRequestModel?) {
        request?.copy()?.let {
            replaceFragment(HLProviderEditServiceRequestFragment(it), R.id.mainContainer)
        }
    }


    fun markComplete(request: HLServiceRequestModel?) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.app_name)
                .setMessage(R.string.msg_alert_complete_request)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    updateRequestService(request, HLServiceRequestStatus.COMPLETED)
                }
                .show()
        }
    }

    fun acceptJob(request: HLServiceRequestModel?) {
        updateRequestService(request, HLServiceRequestStatus.ACCEPTED)
    }

    fun updateRequestService (request: HLServiceRequestModel?, status: String) {
        request?.let { req ->
            HLGlobalData.me?.serviceProvider?.account ?: showPayment ()

            val updateData = hashMapOf(
                "status" to status,
                "updatedAt" to Calendar.getInstance().timeInMillis
            )

            HLFirebaseService.instance.updateServiceRequest(req.uid, updateData, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    scheduleRecyclerView?.adapter?.let { adapter ->
                        if (adapter is HLProviderBaseScheduleAdapter) {
                            val index = adapter.allData.indexOfFirst { it.uid == req.uid }
                            if (index >= 0) {
                                adapter.getItem(index).status = status
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }

    fun dismiss(request: HLServiceRequestModel?) {
        HLGlobalData.me?.uid?.let { userId ->
            request?.copy()?.let { req ->
                HLGlobalData.me?.serviceProvider?.account ?: showPayment ()

                req.dismissedBy.add(userId)

                val updateData = hashMapOf<String, Any>(
                    "dismissedBy" to req.dismissedBy,
                    "updatedAt" to Calendar.getInstance().timeInMillis
                )

                HLFirebaseService.instance.updateServiceRequest(req.uid, updateData, object: ResponseCallback<String> {
                    override fun onFailure(error: String) {
                        showError(error)
                    }
                })
            }
        }
    }

    private fun showPayment () {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.app_name)
                .setMessage("First Add Payment Method.\nYou need to complete your payment information in your profile before you may proceed.")
                .setNegativeButton(R.string.ok, null)
                .show()
        }
    }

    /**
     *  Process Requests
     */
    fun processRequests (requests: List<HLServiceRequestModel>) {
        val removedRequests = ArrayList<HLServiceRequestModel>()
        val updatedRequests = ArrayList<HLServiceRequestModel>()

        val adapter = (scheduleRecyclerView?.adapter as? HLProviderBaseScheduleAdapter)

        requests.forEach { request ->

            if (request.diffType == DocumentChange.Type.REMOVED) {
                removedRequests.add(request)
            } else {

                val index = adapter?.allData?.indexOfFirst { it.uid == request.uid } ?: -1
                if (index >= 0 && adapter?.getItem(index)?.status != request.status) {
                    if (request.status == HLServiceRequestStatus.DELETED || request.status == HLServiceRequestStatus.PAID) {
                        adapter?.remove(index)
                    } else {
                        adapter?.getItem(index)?.status = request.status
                    }

                    adapter?.notifyDataSetChanged()
                } else {
                    updatedRequests.add(request)
                }

                if (updatedRequests.isNotEmpty()) {
                    getServiceRequests(updatedRequests.map{ it.uid })
                }

                if (removedRequests.isNotEmpty()) {
                    removeRequests (removedRequests)
                }
            }
        }
    }

    private fun getServiceRequests (requestsIds: List<String>) {
        HLFirebaseService.instance.getServiceRequests(requestsIds, object : ResponseCallback<List<HLServiceRequestModel>> {

            override fun onSuccess(data: List<HLServiceRequestModel>) {

                val adapter = (scheduleRecyclerView?.adapter as? HLProviderBaseScheduleAdapter)

                data.forEach { request ->
                    val index = adapter?.allData?.indexOfFirst { it.uid == request.uid } ?: -1
                    if (index >= 0) {
                        if (request.status == HLServiceRequestStatus.DELETED || request.status == HLServiceRequestStatus.PAID) {
                            adapter?.remove(index)
                        } else {
                            adapter?.update(request, index)
                        }
                    } else {
                        val insertIndex = adapter?.allData?.indexOfFirst { it.requestDate.calendar.isSameDay(request.requestDate.calendar) } ?: -1
                        if (insertIndex >= 0) {
                            adapter?.insert(request, insertIndex)
                        } else {
                            if (adapter?.count == 0) {
                                adapter.add(request)
                            } else {
                                val newInsertIndex = adapter?.allData?.indexOfFirst { it.requestDate < request.requestDate } ?: -1
                                if (newInsertIndex >= 0) {
                                    adapter?.insert(request, newInsertIndex)
                                }
                            }
                        }
                    }
                }

                adapter?.notifyDataSetChanged()
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }

    private fun removeRequests (requests: List<HLServiceRequestModel>) {
        val adapter = (scheduleRecyclerView?.adapter as HLProviderBaseScheduleAdapter)
        requests.forEach { request ->
            val index = adapter.allData.indexOfFirst { it.uid == request.uid }
            if (index >= 0) {
                adapter.remove(index)
            }
        }
        adapter.notifyDataSetChanged()
    }
}
