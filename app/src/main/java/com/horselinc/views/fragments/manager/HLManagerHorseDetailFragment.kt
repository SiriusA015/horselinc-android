package com.horselinc.views.fragments.manager


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentChange
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLServiceProviderModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLHorseUserAdapter
import com.horselinc.views.adapters.recyclerview.HLManagerServiceRequestAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.common.HLPublicUserProfileFragment
import com.horselinc.views.listeners.HLManagerServiceRequestItemListener
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class HLManagerHorseDetailFragment(private var cloneHorse: HLHorseModel) : HLBaseFragment() {

    /**
     *  Controls
     */
    private var backButton: ImageView? = null
    private var editButton: ImageView? = null
    private var profileImageView: RoundedImageView? = null
    private var barnNameTextView: TextView? = null
    private var displayNameTextView: TextView? = null
    private var userRecyclerView: EasyRecyclerView? = null
    private var requestsRecyclerView: EasyRecyclerView? = null
    private var requestButton: Button? = null

    /**
     *  Adapters
     */
    private lateinit var userAdapter: HLHorseUserAdapter
    private lateinit var requestAdapter: HLManagerServiceRequestAdapter

    /**
     * Variables
     */
    private var shouldLoadMore = false
    private var isLoading = false

    private var warningText: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // root view
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_horse_detail, container, false)

            // initialize controls
            initControls ()

            // get service requests
            showProgressDialog()
            getServiceRequests (isRefresh = true)

            // add service request listener
            addServiceRequestListener ()

            // event bus
            EventBus.getDefault().register(this)
        }

        // set horse data
        setHorseData ()

        return rootView
    }

    override fun onDestroy() {
        HLFirebaseService.instance.removeServiceRequestListeners()
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun handleInternetAvailable() {
        super.handleInternetAvailable()
//        requestsRecyclerView?.swipeToRefresh?.isEnabled = true
    }

    override fun handleInternetUnavailable() {
        super.handleInternetUnavailable()
        requestsRecyclerView?.setRefreshing(false)
//        requestsRecyclerView?.swipeToRefresh?.isEnabled = false
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        backButton = rootView?.findViewById(R.id.backImageView)
        editButton = rootView?.findViewById(R.id.editProfileImageView)
        profileImageView = rootView?.findViewById(R.id.horseProfileImageView)
        barnNameTextView = rootView?.findViewById(R.id.barnNameTextView)
        displayNameTextView = rootView?.findViewById(R.id.displayNameTextView)
        userRecyclerView = rootView?.findViewById(R.id.userRecyclerView)
        requestsRecyclerView = rootView?.findViewById(R.id.requestsRecyclerView)
        requestButton = rootView?.findViewById(R.id.requestButton)
        

        // initialize user recycler view
        userAdapter = HLHorseUserAdapter(activity).apply {
            setOnItemClickListener {
                replaceFragment(HLPublicUserProfileFragment(userAdapter.getItem(it)))
            }
        }
        userRecyclerView?.run {
            adapter = userAdapter
            isHorizontalScrollBarEnabled = false
            setLayoutManager(LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false))
        }

        // initialize request recycler view
        requestAdapter = HLManagerServiceRequestAdapter(activity, object: HLManagerServiceRequestItemListener {
            override fun onClickOption(request: HLServiceRequestModel?, position: Int) {
                request?.let { req ->
                    // set option items
                    val optionItems = if (request.status == HLServiceRequestStatus.DECLINED || request.status == HLServiceRequestStatus.PENDING) {
                        arrayOf("Edit", "Delete")
                    } else {
                        arrayOf("Delete")
                    }

                    // show alert
                    activity?.let { act ->
                        AlertDialog.Builder(act)
                            .setTitle("Service Request Options")
                            .setItems(optionItems) { _, which ->
                                if (optionItems.size == 1 || which == 1) {
                                    confirmDeleteRequest (req)
                                } else {
                                    editServiceRequest (req)
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }

            override fun onClickProvider(provider: HLServiceProviderModel?) {
                replaceFragment(HLPublicUserProfileFragment(provider))
            }

            override fun onClickChooseProvider(request: HLServiceRequestModel?, position: Int) {
                request?.let {
                    val cloneRequest = it.copy().apply {
                        status = HLServiceRequestStatus.PENDING
                    }
                    replaceFragment(HLManagerScheduleServiceFragment(cloneRequest, isReassign = true))
                }
            }
        }).apply {
            setMore(R.layout.load_more, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (shouldLoadMore && isNetworkConnected) {
                        getServiceRequests()
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() {}
            })

            // header view
            addHeader(object: RecyclerArrayAdapter.ItemView {
                @SuppressLint("SetTextI18n")
                override fun onBindView(headerView: View?) {
                    if ((warningText ?: "").isEmpty()) {
                        headerView?.findViewById<TextView>(R.id.warningTextView)?.visibility = View.GONE
                    } else {
                        headerView?.findViewById<TextView>(R.id.warningTextView)?.visibility = View.VISIBLE
                        headerView?.findViewById<TextView>(R.id.warningTextView)?.text = warningText
                    }

                    headerView?.findViewById<TextView>(R.id.lastRefreshTextView)?.text = "Last Refresh ${Calendar.getInstance().time.formattedString("h:mm a")}"
                }

                override fun onCreateView(parent: ViewGroup?): View {
                    return LayoutInflater.from(context).inflate(R.layout.item_manager_service_request_header, parent, false)
                }
            })
        }
        requestsRecyclerView?.run {
            setLayoutManager(LinearLayoutManager(activity))
            recyclerView?.setPadding(0, 0, 0, ResourceUtil.dpToPx(92))
            recyclerView?.clipToPadding = false
            setRefreshListener {
                if (isNetworkConnected) {
                    getServiceRequests(isRefresh = true)
                } else {
                    setRefreshing(false)
                }
            }

            adapter = requestAdapter
        }

        // event handlers
        backButton?.setOnClickListener { popFragment() }
        editButton?.setOnClickListener {
            replaceFragment(HLManagerHorseProfileFragment.new(cloneHorse))
        }
        requestButton?.setOnClickListener {
            onClickRequest ()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setHorseData () {
        // profile image
        profileImageView?.loadImage(cloneHorse.avatarUrl, R.drawable.ic_horse_placeholder)

        // barn name
        barnNameTextView?.text = cloneHorse.barnName

        // display name
        displayNameTextView?.text = "\"${cloneHorse.displayName}\""

        // edit button
        editButton?.visibility = if (cloneHorse.trainerId == HLGlobalData.me?.uid || cloneHorse.ownerIds?.contains(HLGlobalData.me?.uid) == true) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        // set horse users
        userAdapter.clear()
        cloneHorse.trainer?.let { trainer ->
            userAdapter.add(trainer)
        }

        cloneHorse.leaser?.let { leaser ->

            // set warning text
            warningText = if (leaser.userId != HLGlobalData.me?.uid) {
                "This horse is leased to another owner. ${leaser.name} is currently responsible for payments."
            } else {
                ""
            }

            userAdapter.add(leaser)
        }

        cloneHorse.owners?.let { owners ->
            userAdapter.addAll(owners)
        }
    }
    /**
     *  Event Handlers
     */
    private fun onClickRequest () {
        HLGlobalData.me?.horseManager?.customer?.defaultSource?.let {
            val cloneRequest = HLServiceRequestModel(horseId = cloneHorse.uid,
                horseBarnName = cloneHorse.barnName,
                horseDisplayName = cloneHorse.displayName)
            replaceFragment(HLManagerScheduleServiceFragment(cloneRequest))
        } ?: showPayment ()
    }

    private fun showPayment () {
        activity?.let { act ->
            AlertDialog.Builder(act)
                .setTitle(R.string.app_name)
                .setMessage("First Add Payment Method.\nYou need to complete your payment information in your profile before you may proceed.")
                .setNegativeButton(R.string.ok, null)
                .show()
        }
    }


    /**
     *  Service Requests Handler
     */
    private fun getServiceRequests (isRefresh: Boolean = false) {
        if (isLoading) {
            return
        }

        isLoading = true
        val lastRequestId = if (isRefresh) null else requestAdapter.allData.last().uid
        HLFirebaseService.instance.searchServiceRequestsForManager(cloneHorse.uid,
            arrayListOf(HLServiceRequestStatus.PENDING,
                HLServiceRequestStatus.ACCEPTED,
                HLServiceRequestStatus.DECLINED,
                HLServiceRequestStatus.COMPLETED),
            lastRequestId, object : ResponseCallback<List<HLServiceRequestModel>> {
                override fun onSuccess(data: List<HLServiceRequestModel>) {
                    hideProgressDialog()

                    if (isRefresh) {
                        requestAdapter.clear()
                    }
                    requestAdapter.addAll(data)

                    shouldLoadMore = data.size.toLong() == HLConstants.LIMIT_SERVICE_REQUESTS
                    isLoading = false

                    if (requestAdapter.count == 0) {
                        requestsRecyclerView?.showEmpty()
                    }
                }

                override fun onFailure(error: String) {
                    shouldLoadMore = false
                    isLoading = false
                    showError(error)
                }
            })
    }

    private fun confirmDeleteRequest (request: HLServiceRequestModel) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.app_name))
                .setMessage(R.string.msg_alert_delete_request)
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("OK") { dialog, _ ->
                    deleteServiceRequest (request)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun editServiceRequest (request: HLServiceRequestModel) {
        replaceFragment(HLManagerScheduleServiceFragment(request.copy()))
    }

    private fun deleteServiceRequest (request: HLServiceRequestModel) {
        HLFirebaseService.instance.deleteServiceRequest(request.uid, object : ResponseCallback<String> {
            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }


    private fun addServiceRequestListener () {
        HLGlobalData.me?.uid ?.let { userId ->
            HLFirebaseService.instance.addServiceRequestListenerForManager(userId, object: ResponseCallback<List<HLServiceRequestModel>> {
                override fun onSuccess(data: List<HLServiceRequestModel>) {
                    if (data.isNotEmpty()) {
                        val removedRequests = ArrayList<HLServiceRequestModel>()
                        val updatedRequests = ArrayList<HLServiceRequestModel>()

                        data.forEach { request ->
                            if (request.diffType == DocumentChange.Type.REMOVED) {
                                removedRequests.add(request)
                            } else {
                                val index = requestAdapter.allData.indexOfFirst { it.uid == request.uid }
                                if (index >= 0 && requestAdapter.getItem(index).status != request.status) {
                                    if (request.status == HLServiceRequestStatus.DELETED) {
                                        requestAdapter.remove(index)
                                    } else {
                                        requestAdapter.getItem(index).status = request.status
                                    }

                                    requestAdapter.notifyDataSetChanged()

                                } else {
                                    updatedRequests.add(request)
                                }
                            }
                        }

                        if (updatedRequests.isNotEmpty()) {
                            getServiceRequests(updatedRequests.map{ it.uid })
                        }

                        if (removedRequests.isNotEmpty()) {
                            removeServiceRequests (removedRequests.map { it.uid })
                        }
                    }
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }

    private fun getServiceRequests (requestIds: List<String>) {
        HLFirebaseService.instance.getServiceRequests(requestIds, object: ResponseCallback<List<HLServiceRequestModel>> {
            override fun onSuccess(data: List<HLServiceRequestModel>) {
                if (data.isNotEmpty()) {
                    data.forEach { request ->
                        val updatedIndex = requestAdapter.allData.indexOfFirst { it.uid == request.uid }
                        if (updatedIndex >= 0) {
                            if (request.status == HLServiceRequestStatus.DELETED) {
                                requestAdapter.remove(updatedIndex)
                            } else {
                                requestAdapter.update(request, updatedIndex)
                            }
                        } else {
                            val sameInsertIndex = requestAdapter.allData.indexOfFirst { it.getRequestDateCalendar().isSameDay(request.getRequestDateCalendar()) }
                            if (sameInsertIndex >= 0) {
                                requestAdapter.insert(request, sameInsertIndex)
                            } else {
                                if (requestAdapter.count == 0) {
                                    requestAdapter.add(request)
                                } else {
                                    val newInsertIndex = requestAdapter.allData.indexOfFirst { it.getRequestDateCalendar().time.before(request.getRequestDateCalendar().time) }
                                    if (newInsertIndex >= 0) {
                                        requestAdapter.insert(request, newInsertIndex)
                                    }
                                }
                            }
                        }
                    }

                    requestAdapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }

    private fun removeServiceRequests (requestIds: List<String>) {
        requestIds.forEach { requestId ->
            val index = requestAdapter.allData.indexOfFirst { it.uid == requestId }
            if (index >= 0) {
                requestAdapter.remove(index)
            }
        }

        requestAdapter.notifyDataSetChanged()
    }
}
