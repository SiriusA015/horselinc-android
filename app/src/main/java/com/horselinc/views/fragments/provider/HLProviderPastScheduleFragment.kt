package com.horselinc.views.fragments.provider

import com.horselinc.HLConstants
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.views.adapters.recyclerview.HLProviderBaseScheduleAdapter
import com.horselinc.views.listeners.HLProviderServiceRequestItemListener
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import java.util.*


class HLProviderPastScheduleFragment : HLProviderScheduleFragment() {

    private var pastAdapter: HLProviderBaseScheduleAdapter? = null

    override fun initControls () {
        super.initControls()

        // initialize adapter
        pastAdapter = HLProviderBaseScheduleAdapter(activity, false, object :
            HLProviderServiceRequestItemListener {

            override fun onClickHorse(request: HLServiceRequestModel?, position: Int) {
                request?.horse?.let {
                    onClickHorse(it)
                }
            }

            override fun onClickTrainer(request: HLServiceRequestModel?, position: Int) {
                request?.horse?.trainer?.let {
                    onClickUser(it)
                }
            }

            override fun onClickMarkComplete(request: HLServiceRequestModel?, position: Int) {
                markComplete (request)
            }

            override fun onClickAssignJob(request: HLServiceRequestModel?, position: Int) {
                assignJob(request)
            }

            override fun onClickAcceptJob(request: HLServiceRequestModel?, position: Int) {
                acceptJob(request)
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
            adapter = pastAdapter
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

        val lastId = if (isRefresh) null else pastAdapter?.allData?.last()?.uid

        // set start date
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        calendar.add(Calendar.DATE, -1)

        val cloneFilter = filter?.copy()
        if (cloneFilter?.endDate != null) {
            cloneFilter.endDate = if ((cloneFilter.endDate ?: 0) > calendar.timeInMillis) {
                calendar.timeInMillis
            } else {
                cloneFilter.endDate
            }
        } else {
            cloneFilter?.endDate = calendar.timeInMillis
        }

        HLFirebaseService.instance.searchServiceRequestsForProvider (null, HLGlobalData.me?.uid, null, lastId, filter = cloneFilter,
            callback = object: ResponseCallback<List<HLServiceRequestModel>> {

                override fun onSuccess(data: List<HLServiceRequestModel>) {

                    hideProgressDialog()

                    isLoading = false
                    shouldLoadMore = data.size.toLong() == HLConstants.LIMIT_SERVICE_REQUESTS

                    if (isRefresh) {
                        pastAdapter?.clear()
                    }

                    pastAdapter?.addAll(data)

                    if (pastAdapter?.count == 0) {
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
}
