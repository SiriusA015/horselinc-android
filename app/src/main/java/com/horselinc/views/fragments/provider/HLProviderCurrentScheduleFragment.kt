package com.horselinc.views.fragments.provider

import android.app.AlertDialog
import com.horselinc.HLConstants
import com.horselinc.HLGlobalData
import com.horselinc.HLServiceRequestStatus
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.views.adapters.recyclerview.HLProviderBaseScheduleAdapter
import com.horselinc.views.listeners.HLProviderServiceRequestItemListener
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import java.util.*


class HLProviderCurrentScheduleFragment : HLProviderScheduleFragment() {

    private var currentAdapter: HLProviderBaseScheduleAdapter? = null

    private val requestStatuses = arrayListOf(
        HLServiceRequestStatus.PENDING,
        HLServiceRequestStatus.ACCEPTED,
        HLServiceRequestStatus.DECLINED,
        HLServiceRequestStatus.COMPLETED/*,
        HLServiceRequestStatus.DELETED,
        HLServiceRequestStatus.PAID*/
    )

    override fun initControls () {
        super.initControls()

        // initialize adapter
        currentAdapter = HLProviderBaseScheduleAdapter(activity, true, object:
            HLProviderServiceRequestItemListener {
            override fun onClickHorse(request: HLServiceRequestModel?, position: Int) {
                request?.horse?.let {
                    onClickHorse (it)
                }
            }

            override fun onClickOption(request: HLServiceRequestModel?, position: Int) {
                onClickOption(request)
            }

            override fun onClickTrainer(request: HLServiceRequestModel?, position: Int) {
                request?.horse?.trainer?.let {
                    onClickUser(it)
                }
            }

            override fun onClickMarkComplete(request: HLServiceRequestModel?, position: Int) {
                markComplete(request)
            }

            override fun onClickAssignJob(request: HLServiceRequestModel?, position: Int) {
                assignJob(request)
            }

            override fun onClickAcceptJob(request: HLServiceRequestModel?, position: Int) {
                acceptJob(request)
            }

            override fun onClickDismiss(request: HLServiceRequestModel?, position: Int) {
                dismiss(request)
            }
        }).apply {

            addHeader(scheduleHeaderView)

            setMore(R.layout.load_more, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (shouldLoadMore) {
                        getServiceRequests (false)
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() { }
            })
        }

        scheduleRecyclerView?.run {
            adapter = currentAdapter
        }

        // get service requests
        scheduleRecyclerView?.setRefreshing(true)
        getServiceRequests(true)
    }

    /**
     *  Request Handlers
     */
    override fun getServiceRequests (isRefresh: Boolean) {
        if (isRefresh) {
            isLoading = false
            shouldLoadMore = false
        }

        if (isLoading) return
        isLoading = true

        val lastId = if (isRefresh) null else currentAdapter?.allData?.last()?.uid

        // set start date
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)


        val cloneFilter = filter?.copy()
        if (cloneFilter?.startDate != null) {
            cloneFilter.startDate = if ((cloneFilter.startDate ?: 0) < calendar.timeInMillis) {
                calendar.timeInMillis
            } else {
                cloneFilter.startDate
            }
        } else {
            cloneFilter?.startDate = calendar.timeInMillis
        }

        HLFirebaseService.instance.searchServiceRequestsForProvider (null, HLGlobalData.me?.uid, requestStatuses, lastId, filter = cloneFilter,
            callback = object: ResponseCallback<List<HLServiceRequestModel>> {

                override fun onSuccess(data: List<HLServiceRequestModel>) {

                    hideProgressDialog()

                    isLoading = false
                    shouldLoadMore = data.size.toLong() == HLConstants.LIMIT_SERVICE_REQUESTS

                    if (isRefresh) {
                        currentAdapter?.clear()
                    }

                    currentAdapter?.addAll(data)

                    if (currentAdapter?.count == 0) {
                        scheduleRecyclerView?.showEmpty()
                    }
                }

                override fun onFailure(error: String) {
                    isLoading = false
                    shouldLoadMore = false
                    showError(error)
                }
            })
    }

    private fun onClickOption (request: HLServiceRequestModel?) {
        val options = ArrayList<String>()

        if (request?.status == HLServiceRequestStatus.PENDING
            && ((request.isMainServiceProvider && request.assignerId == null)
                    || request.isReassignedServiceProvider)) {

            options.add(getString(R.string.request_option_decline_job))
        }

        if (request?.status == HLServiceRequestStatus.PENDING && request.isMainServiceProvider && request.assignerId != null) {
            options.add(getString(R.string.request_option_assign_to_someone_else))
        }

        if (request?.status == HLServiceRequestStatus.ACCEPTED) {
            options.add(getString(R.string.request_option_add_services))
        }

        val arrayOptions = arrayOfNulls<String>(options.size)
        activity?.let { act ->
            AlertDialog.Builder(act)
                .setTitle("Job Options")
                .setItems(options.toArray(arrayOptions)) { _, which ->
                    when (options[which]) {
                        getString(R.string.request_option_decline_job) -> {
                            updateRequestService (request, HLServiceRequestStatus.DECLINED)
                        }
                        getString(R.string.request_option_assign_to_someone_else) -> {
                            assignJob(request)
                        }
                        getString(R.string.request_option_add_services) -> {
                            addService (request)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }


}
