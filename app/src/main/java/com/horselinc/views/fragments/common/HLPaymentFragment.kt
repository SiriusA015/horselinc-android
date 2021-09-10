package com.horselinc.views.fragments.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.views.adapters.recyclerview.HLInvoiceGroupCardAdapter
import com.google.firebase.firestore.DocumentChange
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLInvoiceModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.event.*
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.provider.HLProviderCreateInvoiceFragment
import com.horselinc.views.listeners.HLInvoiceGroupItemListener
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/** Created by jcooperation0137 on 2019-08-26.
 */
class HLPaymentFragment(val screenType: HLPaymentScreenType): HLBaseFragment() {

    // MARK: - Properties
    var recyclerView: EasyRecyclerView? = null
    var createInvoiceButton: Button? = null

    var invoiceAdapter: HLInvoiceGroupCardAdapter? = null

    var lastId: String? = null
    var isLoading: Boolean = false
    var shouldLoadMore: Boolean = true
    var invoices = ArrayList<HLInvoiceModel>()
    var isHorseGrouped = false
    var isRefresh = true

    // MARK: - Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        rootView?:let {

            EventBus.getDefault().register(this)

            rootView = inflater.inflate(R.layout.fragment_payment, container, false)
            recyclerView = rootView?.findViewById(R.id.recyclerView)
            createInvoiceButton = rootView?.findViewById(R.id.createInvoiceButton)

            initControls()
        }

        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun handleInternetAvailable() {
        super.handleInternetAvailable()
//        recyclerView?.swipeToRefresh?.isEnabled = true
    }

    override fun handleInternetUnavailable() {
        super.handleInternetUnavailable()
        recyclerView?.setRefreshing(false)
//        recyclerView?.swipeToRefresh?.isEnabled = false
    }

    // MARK: - Functions
    fun initControls() {

        when (screenType) {
            HLPaymentScreenType.drafts -> createInvoiceButton?.visibility = View.VISIBLE
            else -> createInvoiceButton?.visibility = View.GONE
        }

        createInvoiceButton?.setOnClickListener {
            activity?.apply {
                replaceFragment(HLProviderCreateInvoiceFragment(null), R.id.mainContainer)
            }
        }

        invoiceAdapter = HLInvoiceGroupCardAdapter(activity, object: HLInvoiceGroupItemListener {
            override fun onClickInvoiceCard(invoice: HLInvoiceModel) {
                replaceFragment(HLInvoiceDetailFragment(invoice.copy()), R.id.mainContainer)
            }

        }).apply {
            setMore(R.layout.load_more, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (shouldLoadMore && isNetworkConnected) {
                        isRefresh = false
                        getInvoices()
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() { }
            })
        }

        recyclerView?.run {
            adapter = invoiceAdapter
            val manager = LinearLayoutManager(activity)
            manager.orientation = LinearLayoutManager.VERTICAL
            setLayoutManager(manager)
            setRefreshListener {
                if (isNetworkConnected) {
                    isRefresh = true
                    lastId = null
                    shouldLoadMore = true
                    getInvoices()
                } else {
                    setRefreshing(false)
                }
            }
        }

        getInvoices()
    }

    // MARK: - Get data from server.
    fun getInvoices() {
        when (screenType) {
            HLPaymentScreenType.drafts -> getDrafts()
            HLPaymentScreenType.submitted, HLPaymentScreenType.outstanding -> getSubmittedInvoices(false)
            HLPaymentScreenType.paid, HLPaymentScreenType.completed -> getSubmittedInvoices(true)
        }
    }

    private fun getDrafts() {
        if (isLoading) {
            return
        }

        if (!shouldLoadMore) {
            recyclerView?.swipeToRefresh?.isRefreshing = false
            invoiceAdapter?.stopMore()
            return
        }
        isLoading = true

        if (isRefresh) {
            recyclerView?.swipeToRefresh?.isRefreshing = true
        }

        if (HLGlobalData.me?.serviceProvider?.userId == null) return
        val providerId = HLGlobalData.me!!.serviceProvider!!.userId

        HLFirebaseService.instance.searchServiceRequestsForProvider(null,
            providerId,
            arrayListOf(HLServiceRequestStatus.COMPLETED),
            lastId,
            null,
            null,
            object : ResponseCallback<List<HLServiceRequestModel>> {
                override fun onSuccess(data: List<HLServiceRequestModel>) {

                    recyclerView?.swipeToRefresh?.isRefreshing = false
                    invoiceAdapter?.stopMore()

                    if (isRefresh) {
                        invoices.clear()
                    }

                    shouldLoadMore = data.size == HLConstants.LIMIT_SERVICE_REQUESTS.toInt()
                    if (shouldLoadMore) {
                        lastId = data.last().uid
                    }

                    if (data.isEmpty()) {
                        // Reload data
                        if (shouldLoadMore) {
                            shouldLoadMore = false
                        }
                        if (isRefresh) {
                            invoiceAdapter?.clear()
                        }
                        isLoading = false

                        // Add service requests completed observer.
                        HLFirebaseService.instance.subscribeServiceRequestsCompletedListener()
                    } else {
                        val updatedIndices = arrayListOf<Int>()
                        val newInvoices = arrayListOf<HLInvoiceModel>()

                        for (request in data) {
                            if (isHorseGrouped) {
                                // Check an invoice is existed whose horse id is same with a service request.
                                val index = invoices.indexOfFirst { invoice ->
                                    invoice.requests?.firstOrNull()?.horseId == request.horseId
                                }

                                if (index > -1) {
                                    // If existed, add the service request into the requests array of the found invoice.
                                    invoices[index].requests?.add(request)
                                    invoices[index].requestIds.add(request.uid)

                                    // If the invoice is contained in new invoices, will not set as updated one, then will update new invoices
                                    val newIndex = newInvoices.indexOfFirst { invoice ->
                                        invoice.requests?.firstOrNull()?.horseId == request.horseId
                                    }
                                    if (newIndex == -1) {
                                        updatedIndices.add(index)
                                    } else {
                                        newInvoices[index].requests?.add(request)
                                        newInvoices[index].requestIds.add(request.uid)
                                    }
                                } else {
                                    // If not existed, create a new invoice with the request
                                    val newInvoice = HLInvoiceModel()
                                    newInvoice.requestIds = arrayListOf()
                                    newInvoice.requests = arrayListOf()
                                    newInvoice.requests?.add(request)
                                    newInvoice.requestIds.add(request.uid)
                                    newInvoice.status = HLInvoiceStatusType.DRAFT
                                    newInvoice.amount = 0.0
                                    newInvoice.name = request.horse?.barnName.orEmpty()
                                    invoices.add(newInvoice)
                                    newInvoices.add(newInvoice.copy())
                                }
                            } else {
                                // Check an invoice is existed whose provider id is same with a service request.
                                val index = invoices.indexOfFirst { invoice ->
                                    val requestIndex = invoice.requests?.indexOfFirst { localRequest ->
                                        if (localRequest.payer != null && request.payer != null) {
                                            localRequest.payer!!.userId == request.payer!!.userId
                                        } else {
                                            false
                                        }
                                    }

                                    requestIndex != -1
                                }

                                if (index > -1) {
                                    // If existed, add the service request into the requests array of the found invoice.
                                    invoices[index].requests?.add(request)
                                    invoices[index].requestIds.add(request.uid)

                                    // If the invoice is contained in new invoices, will not set as updated one, then will update new invoices
                                    val newIndex = newInvoices.indexOfFirst { invoice ->
                                        invoice.requests?.filter { it.uid == request.uid }.orEmpty().isNotEmpty()
                                    }
                                    if (newIndex == -1) {
                                        updatedIndices.add(index)
                                    } else {
                                        newInvoices[index].requests?.add(request)
                                        newInvoices[index].requestIds.add(request.uid)
                                    }
                                } else {
                                    // If not existed, create a new invoice with the request
                                    val newInvoice = HLInvoiceModel()
                                    newInvoice.requestIds = arrayListOf()
                                    newInvoice.requests = arrayListOf()
                                    newInvoice.requests?.add(request)
                                    newInvoice.requestIds.add(request.uid)
                                    newInvoice.status = HLInvoiceStatusType.DRAFT
                                    newInvoice.amount = 0.0
                                    newInvoice.name = request.serviceProvider?.name.orEmpty()
                                    invoices.add(newInvoice)
                                    newInvoices.add(newInvoice.copy())
                                }

                            }
                        }

                        // calculate again the amount to be paid for drafts
                        for (invoice in invoices) {
                            invoice.amount = 0.0
                            invoice.requests?.map { it.services }?.forEach { services ->
                                val sum = services.map { service -> service.rate * service.quantity }.sum()
                                if (invoice.amount == null) {
                                    invoice.amount = sum.toDouble()
                                } else {
                                    invoice.amount = invoice.amount!! + sum
                                }
                            }
                        }
                        for (invoice in newInvoices) {
                            invoice.amount = 0.0
                            invoice.requests?.map { it.services }?.forEach { services ->
                                val sum = services.map { service -> service.rate * service.quantity }.sum()
                                if (invoice.amount == null) {
                                    invoice.amount = sum.toDouble()
                                } else {
                                    invoice.amount = invoice.amount!! + sum
                                }
                            }
                        }

                        isLoading = false
                        // Reload data
                        if (isRefresh) {
                            invoiceAdapter?.clear()
                            invoiceAdapter?.addAll(invoices)
                        } else {
                            invoiceAdapter?.setNotifyOnChange(false)
                            updatedIndices.forEach { index ->
                                invoiceAdapter?.update(invoices[index], index)
                            }
                            invoiceAdapter?.addAll(newInvoices)
                            invoiceAdapter?.notifyDataSetChanged()
                            invoiceAdapter?.setNotifyOnChange(true)
                        }

                        HLFirebaseService.instance.subscribeServiceRequestsCompletedListener()
                        return
                    }
                }

                override fun onFailure(error: String) {
                    isLoading = false
                    recyclerView?.swipeToRefresh?.isRefreshing = false
                    invoiceAdapter?.stopMore()
                    HLFirebaseService.instance.subscribeServiceRequestsCompletedListener()
                    showError(error)
                }
            })

    }

    private fun getSubmittedInvoices(isFullPaid: Boolean = false) {
        if (isLoading) {
            return
        }

        if (!shouldLoadMore) {
            recyclerView?.swipeToRefresh?.isRefreshing = false
            invoiceAdapter?.stopMore()
            isLoading = false
            return
        }
        isLoading = true

        val userId: String? = when (HLGlobalData.me?.type) {
            HLUserType.PROVIDER -> HLGlobalData.me?.serviceProvider?.userId
            else -> HLGlobalData.me?.horseManager?.userId
        }

        if (userId.isNullOrEmpty()) {
            recyclerView?.swipeToRefresh?.isRefreshing = false
            invoiceAdapter?.stopMore()
            isLoading = false
            return
        }

        if (isRefresh) {
            recyclerView?.swipeToRefresh?.isRefreshing = true
        }

        HLFirebaseService.instance.getInvoices(userId,
            (when (isFullPaid) { true -> arrayListOf(HLInvoiceStatusType.FULL_PAID) else -> arrayListOf(HLInvoiceStatusType.SUBMITTED, HLInvoiceStatusType.PAID)}),
            null,
            lastId,
            object: ResponseCallback<List<HLInvoiceModel>> {
                override fun onSuccess(data: List<HLInvoiceModel>) {
                    recyclerView?.swipeToRefresh?.isRefreshing = false
                    invoiceAdapter?.stopMore()

                    if (isRefresh) {
                        invoices.clear()
                    }

                    shouldLoadMore = data.size == HLConstants.LIMIT_INVOICES.toInt()
                    if (shouldLoadMore) {
                        lastId = data.last().uid
                    }

                    if (data.isEmpty()) {
                        // Reload data
                        if (shouldLoadMore) {
                            shouldLoadMore = false
                        }
                        if (isRefresh) {
                            invoiceAdapter?.clear()
                        }
                        isLoading = false

                        // Add payments status update listener
                        HLFirebaseService.instance.subscribePaymentStatusUpdatedListener()
                    } else {

                        val updatedIndices = arrayListOf<Int>()
                        val newInvoices = arrayListOf<HLInvoiceModel>()

                        // Reeplace duplicated ones
                        for (invoice in data) {
                            val index = invoices.indexOfFirst { it.uid == invoice.uid }
                            if (index > -1) {
                                invoices[index] = invoice
                            } else {
                                invoices.add(invoice)
                            }

                            // If the invoice is contained in new invoices, will not set as updated one, then will update new invoices
                            val newIndex = invoices.indexOfFirst { it.uid == invoice.uid }
                            if (newIndex == -1) {
                                updatedIndices.add(index)
                            } else {
                                newInvoices.add(invoice)
                            }
                        }

                        isLoading = false

                        // Reload data
                        if (isRefresh) {
                            invoiceAdapter?.clear()
                            invoiceAdapter?.addAll(invoices)
                        } else {
                            invoiceAdapter?.setNotifyOnChange(false)
                            updatedIndices.forEach { index ->
                                invoiceAdapter?.update(invoices[index], index)
                            }
                            invoiceAdapter?.addAll(newInvoices)
                            invoiceAdapter?.notifyDataSetChanged()
                            invoiceAdapter?.setNotifyOnChange(true)
                        }

                        // Add payments status update listener
                        HLFirebaseService.instance.subscribePaymentStatusUpdatedListener()
                    }
                }

                override fun onFailure(error: String) {
                    isLoading = false
                    recyclerView?.swipeToRefresh?.isRefreshing = false
                    invoiceAdapter?.stopMore()
                    // Add payments status update listener
                    HLFirebaseService.instance.subscribePaymentStatusUpdatedListener()
                    showError(error)
                }
            })
    }

    // MARK: - EventBus handler
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentReloadInvoices(event: HLEventPaymentReloadInvoices) {
        if (event.screenType != screenType) return

        isRefresh = true
        lastId = null
        shouldLoadMore = true
        getInvoices()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentGroupByChanged(event: HLEventPaymentGroupByChanged) {
        if (screenType != HLPaymentScreenType.drafts) {
            return
        }

        isRefresh = true
        lastId = null
        shouldLoadMore = true
        isHorseGrouped = event.isHorseGroup
        getDrafts()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventServiceRequestsCompleted(event: HLEventServiceRequestsCompleted) {

        if (event.requestIds.isEmpty() || event.statuses.isEmpty() || screenType != HLPaymentScreenType.drafts) { return }

        val updatedRequestIds = arrayListOf<String>()
        val deletedRequestIds = arrayListOf<String>()
        val deletedInvoiceIndices = arrayListOf<Int>()
        val updatedInvoiceIndices = arrayListOf<Int>()

        var index = 0
        for (requestId in event.requestIds) {
            val invoiceIndex = invoices.indexOfFirst { invoice ->

                // NOTE: App doesn't care which type user selects between horse group or payer group because filter keyword is requestId
                // Find the invoice index in draft invoices if the requestIdes of an invoice contains requestId

                val requestIndex = invoice.requestIds.indexOfFirst { it == requestId }
                // Delete duplicated one.
                if (requestIndex > -1) {
                    invoice.requestIds.removeAt(requestIndex)
                    invoice.requests?.removeAt(requestIndex)
                    invoice.amount = 0.0
                }

                requestIndex != -1
            }


            if (event.statuses[index] != DocumentChange.Type.REMOVED) {
                updatedRequestIds.add(requestId)
            } else {
                deletedRequestIds.add(requestId)
            }

            if (invoiceIndex != -1) {

                if (event.statuses[index] == DocumentChange.Type.REMOVED) {
                    updatedInvoiceIndices.add(invoiceIndex)
                }

                // If draft invoice has no request anymore, just remove the invoice from the array of draft invoices.
                if (invoices[invoiceIndex].requestIds.isEmpty()) {
                    invoices.removeAt(invoiceIndex)
                    deletedInvoiceIndices.add(invoiceIndex)
                    updatedInvoiceIndices.remove(invoiceIndex)
                }
            }
            index += 1
        }

        // Get service requests
        if (updatedRequestIds.size > 0) {
            HLFirebaseService.instance.getServiceRequests(
                updatedRequestIds,
                object : ResponseCallback<List<HLServiceRequestModel>> {
                    override fun onSuccess(data: List<HLServiceRequestModel>) {

                        val updatedIndices = arrayListOf<Int>()
                        val newInvoices = arrayListOf<HLInvoiceModel>()

                        for (request in data) {
                            if (isHorseGrouped) {
                                // Check an invoice is existed whose horse id is same with a service request.
                                val index = invoices.indexOfFirst { invoice ->
                                    invoice.requests?.firstOrNull()?.horseId == request.horseId
                                }

                                if (index > -1) {
                                    // If existed, add the service request into the requests array of the found invoice.
                                    invoices[index].requests?.add(request)
                                    invoices[index].requestIds.add(request.uid)

                                    // If the invoice is contained in new invoices, will not set as updated one, then will update new invoices
                                    val newIndex = newInvoices.indexOfFirst { invoice ->
                                        invoice.requests?.firstOrNull()?.horseId == request.horseId
                                    }
                                    if (newIndex == -1) {
                                        updatedIndices.add(index)
                                    } else {
                                        newInvoices[index].requests?.add(request)
                                        newInvoices[index].requestIds.add(request.uid)
                                    }
                                } else {
                                    // If not existed, create a new invoice with the request
                                    val newInvoice = HLInvoiceModel()
                                    newInvoice.requestIds = arrayListOf()
                                    newInvoice.requests = arrayListOf()
                                    newInvoice.requests?.add(request)
                                    newInvoice.requestIds.add(request.uid)
                                    newInvoice.status = HLInvoiceStatusType.DRAFT
                                    newInvoice.amount = 0.0
                                    newInvoice.name = request.horse?.barnName.orEmpty()
                                    invoices.add(newInvoice)
                                    newInvoices.add(newInvoice.copy())
                                }

                                // If the invoice is contained in new invoices, will not set as updated one, then will update new invoices
                                val newIndex = newInvoices.indexOfFirst { invoice ->
                                    invoice.requests?.filter { it.uid == request.uid }.orEmpty().isNotEmpty()
                                }
                                if (newIndex == -1) {
                                    updatedIndices.add(index)
                                } else {
                                    newInvoices[newIndex].requests?.add(request)
                                    newInvoices[newIndex].requestIds.add(request.uid)
                                }
                            } else {
                                // Check an invoice is existed whose provider id is same with a service request.
                                val index = invoices.indexOfFirst { invoice ->
                                    val requestIndex = invoice.requests?.indexOfFirst { localRequest ->
                                        if (localRequest.payer != null && request.payer != null) {
                                            localRequest.payer!!.userId == request.payer!!.userId
                                        } else {
                                            false
                                        }
                                    }

                                    requestIndex != -1
                                }

                                if (index > -1) {
                                    // If existed, add the service request into the requests array of the found invoice.
                                    invoices[index].requests?.add(request)
                                    invoices[index].requestIds.add(request.uid)

                                    // If the invoice is contained in new invoices, will not set as updated one, then will update new invoices
                                    val newIndex = newInvoices.indexOfFirst { invoice ->
                                        invoice.requests?.filter { it.uid == request.uid }.orEmpty().isNotEmpty()
                                    }
                                    if (newIndex == -1) {
                                        updatedIndices.add(index)
                                    } else {
                                        newInvoices[newIndex].requests?.add(request)
                                        newInvoices[newIndex].requestIds.add(request.uid)
                                    }
                                } else {
                                    // If not existed, create a new invoice with the request
                                    val newInvoice = HLInvoiceModel()
                                    newInvoice.requestIds = arrayListOf()
                                    newInvoice.requests = arrayListOf()
                                    newInvoice.requests?.add(request)
                                    newInvoice.requestIds.add(request.uid)
                                    newInvoice.status = HLInvoiceStatusType.DRAFT
                                    newInvoice.amount = 0.0
                                    newInvoice.name = request.serviceProvider?.name.orEmpty()
                                    invoices.add(newInvoice)
                                    newInvoices.add(newInvoice.copy())
                                }

                            }
                        }

                        // calculate again the amount to be paid for drafts
                        for (draft in invoices) {
                            draft.amount = 0.0
                            draft.requests?.map { it.services }?.forEach { services ->
                                val sum =
                                    services.map { service -> service.rate * service.quantity }
                                        .sum()
                                if (draft.amount == null) {
                                    draft.amount = sum.toDouble()
                                } else {
                                    draft.amount = draft.amount!! + sum
                                }

                            }
                        }
                        for (invoice in newInvoices) {
                            invoice.amount = 0.0
                            invoice.requests?.map { it.services }?.forEach { services ->
                                val sum = services.map { service -> service.rate * service.quantity }.sum()
                                if (invoice.amount == null) {
                                    invoice.amount = sum.toDouble()
                                } else {
                                    invoice.amount = invoice.amount!! + sum
                                }
                            }
                        }


                        isLoading = false
                        // Reload data
                        if (isRefresh) {
                            invoiceAdapter?.clear()
                            invoiceAdapter?.addAll(invoices)
                        } else {
                            invoiceAdapter?.setNotifyOnChange(false)
                            updatedIndices.forEach { index ->
                                invoiceAdapter?.update(invoices[index], index)
                            }
                            invoiceAdapter?.addAll(newInvoices)
                            invoiceAdapter?.notifyDataSetChanged()
                            invoiceAdapter?.setNotifyOnChange(true)
                        }
                    }

                    override fun onFailure(error: String) {
                        showError(error)
                    }
                })

        }

        if (deletedRequestIds.isNotEmpty()) {
            // calculate again the amount to be paid for drafts
            for (invoice in invoices) {
                invoice.amount = 0.0
                invoice.requests?.map { it.services }?.forEach { services ->
                    val sum =
                        services.map { service -> service.rate * service.quantity }
                            .sum()
                    if (invoice.amount == null) {
                        invoice.amount = sum.toDouble()
                    } else {
                        invoice.amount = invoice.amount!! + sum
                    }

                }
            }

            isLoading = false
            // Reload data
            invoiceAdapter?.setNotifyOnChange(false)
            updatedInvoiceIndices.forEach { index ->
                invoiceAdapter?.update(invoices[index], index)
            }

            deletedInvoiceIndices.forEach { index ->
                invoiceAdapter?.remove(index)
            }
            invoiceAdapter?.notifyDataSetChanged()
            invoiceAdapter?.setNotifyOnChange(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentInvoicesUpdated(event: HLEventPaymentInvoicesUpdated) {

        // If the screenType is drafts, doesn't operate this event
        if (screenType == HLPaymentScreenType.drafts) return

        if (event.invoiceIds.isEmpty() || event.statuses.isEmpty()) { return }

        // Find deleted invoices
        val deletedIndexes= arrayListOf<Int>()
        val restInvoices = arrayListOf<String>()
        var index = 0
        for (status in event.statuses) {
            if (status == DocumentChange.Type.REMOVED) {
                // Delete invoices from submitted and paid arrays
                val deleteIndex = invoices.indexOfFirst { it.uid == event.invoiceIds[index] }
                if (deleteIndex > -1) {
                    invoices.removeAt(deleteIndex)
                    deletedIndexes.add(deleteIndex)
                }
            } else {
                restInvoices.add(event.invoiceIds[index])
            }
            index += 1
        }

        // Remove deleted invoices from the submitted and paid tabs.
        deletedIndexes.forEach { invoiceAdapter?.remove(it) }


        // Get invoice data
        HLFirebaseService.instance.getInvoices(event.invoiceIds,
            object : ResponseCallback<List<HLInvoiceModel>> {
                override fun onSuccess(data: List<HLInvoiceModel>) {
                    for (invoice in data) {
                        when (invoice.status) {
                            HLInvoiceStatusType.SUBMITTED, HLInvoiceStatusType.PAID -> {
                                // Update on submitted and outstanding screens.
                                if (screenType == HLPaymentScreenType.submitted || screenType == HLPaymentScreenType.outstanding) {
                                    val originalIndex = invoices.indexOfFirst { it.uid == invoice.uid }
                                    if (originalIndex > -1) {
                                        invoices.removeAt(originalIndex)
                                    }
                                    invoices.add(0, invoice)
                                }
                            }
                            HLInvoiceStatusType.FULL_PAID -> {
                                // Delete from submitted or outstanding invoices
                                when (screenType) {
                                    HLPaymentScreenType.submitted, HLPaymentScreenType.outstanding -> {
                                        val originalIndex = invoices.indexOfFirst { it.uid == invoice.uid }
                                        if (originalIndex > -1) {
                                            invoices.removeAt(originalIndex)
                                        }
                                    }

                                    HLPaymentScreenType.paid, HLPaymentScreenType.completed -> {
                                        val originalIndex = invoices.indexOfFirst { it.uid == invoice.uid }
                                        if (originalIndex > -1) {
                                            invoices.removeAt(originalIndex)
                                        }
                                        invoices.add(0, invoice)
                                    }
                                }
                            }
                        }
                    }

                    invoiceAdapter?.clear()
                    invoiceAdapter?.addAll(invoices)
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentInvoiceDeleted(event: HLEventPaymentInvoiceDeleted) {

        if (screenType == HLPaymentScreenType.drafts) return

        val index = invoices.indexOfFirst { it.uid == event.invoiceId }
        if (index > -1) {
            invoices.removeAt(index)
            invoiceAdapter?.remove(index)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedHLEventPaymentDraftDeleted(event: HLEventPaymentDraftDeleted) {
        if (screenType != HLPaymentScreenType.drafts) return

        if (invoices.contains(event.invoice)) {
            invoices.remove(event.invoice)
            invoiceAdapter?.remove(event.invoice)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentInvoiceRefresh(event: HLEventPaymentInvoiceRefresh) {
        when (screenType) {
            HLPaymentScreenType.drafts -> {
                if (isHorseGrouped) {
                    // if the invoice is in invoices, just update it as the invoice.
                    val index = invoices.indexOfFirst { invoice ->
                        invoice.getHorse()?.uid == event.invoice.getHorse()?.uid
                    }

                    if (index > -1) {
                        when (event.invoice.status) {
                            HLInvoiceStatusType.DRAFT -> {
                                invoices[index] = event.invoice
                                invoiceAdapter?.update(event.invoice, index)
                            }

                            HLInvoiceStatusType.PAID -> {
                                invoices.removeAt(index)
                                invoiceAdapter?.remove(index)
                            }
                        }
                    }
                } else {

                    val index = invoices.indexOfFirst { invoice ->
                        invoice.requests?.firstOrNull()?.payer?.userId == event.invoice.requests?.firstOrNull()?.payer?.userId
                    }

                    if (index > -1) {
                        invoices[index] = event.invoice
                        invoiceAdapter?.update(event.invoice, index)
                    }
                }
            }

            else -> {
                val index = invoices.indexOfFirst { it.uid == event.invoice.uid }
                if (index > -1) {
                    invoices[index] = event.invoice
                    invoiceAdapter?.update(event.invoice, index)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedEventPaymentInvoiceCreated(event: HLEventPaymentInvoiceCreated) {
        isRefresh = true
        lastId = null
        shouldLoadMore = true
        getInvoices()
    }

}