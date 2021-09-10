package com.horselinc.views.fragments.provider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLInvoiceModel
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.event.HLEventPaymentInvoiceDeleted
import com.horselinc.models.event.HLEventPaymentInvoiceRefresh
import com.horselinc.views.activities.HLHorseManagerMainActivity
import com.horselinc.views.activities.HLServiceProviderMainActivity
import com.horselinc.views.adapters.recyclerview.HLEditInvoiceSelectServiceAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.common.HLPublicHorseProfileFragment
import com.horselinc.views.listeners.HLEditInvoiceSelectServiceAdapterListener
import com.horselinc.views.listeners.HLEditInvoiceServiceRequestViewListener
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList


/** Created by jcooperation0137 on 2019-08-31.
 */
class HLProviderEditInvoiceFragment(var invoice: HLInvoiceModel): HLBaseFragment() {

    // MARK: - Properties
    var toolBar: Toolbar? = null
    private var requestsLayout: LinearLayout? = null
    private var actionLayout: LinearLayout? = null
    private var saveButtonLayout: LinearLayout? = null
    var saveButton: Button? = null
    private var deleteButtonLayout: LinearLayout? = null
    var deleteButton: Button? = null

    private var requestViews = arrayListOf<HLEditInvoiceServiceRequestView>()
    var providerServices = ArrayList<HLServiceProviderServiceModel>()
    var selectedSectionIndex = -1

    // MARK: - Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_edit_invoice, container, false)

            toolBar = rootView?.findViewById(R.id.toolBar)
            requestsLayout = rootView?.findViewById(R.id.requestsLayout)
            actionLayout = rootView?.findViewById(R.id.actionLayout)
            saveButtonLayout = rootView?.findViewById(R.id.saveButtonLayout)
            saveButton = rootView?.findViewById(R.id.saveButton)
            deleteButtonLayout = rootView?.findViewById(R.id.deleteButtonLayout)
            deleteButton = rootView?.findViewById(R.id.deleteButton)

            getServices()
            initControls()
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data?.let {
            val requestDate = it.getLongExtra(IntentExtraKey.CALENDAR_RETURN_DATE, 0)
            if (requestDate == 0L) return

            if (selectedSectionIndex > -1) {
                invoice.requests?.get(selectedSectionIndex)?.requestDate = requestDate
                invoice.requests?.get(selectedSectionIndex)?.let { serviceRequest ->
                    requestViews[selectedSectionIndex].setData(selectedSectionIndex, serviceRequest)
                }
                selectedSectionIndex = -1
            }
        }
    }

    // MARK: - Functions
    fun initControls() {
        activity?.let {
            if (HLGlobalData.me?.type == HLUserType.MANAGER) {
                (it as HLHorseManagerMainActivity).setSupportActionBar(toolBar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            } else {
                (it as HLServiceProviderMainActivity).setSupportActionBar(toolBar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }

        context?.let {
            toolBar?.setTitleTextColor(ContextCompat.getColor(it, R.color.colorWhite))

            if (toolBar?.navigationIcon != null) {
                DrawableCompat.setTint(toolBar?.navigationIcon!!, ContextCompat.getColor(it, R.color.colorWhite))
            }
        }
        toolBar?.title = "Edit Invoice"

        toolBar?.setNavigationOnClickListener {
            popFragment()
        }

        // Add service requests
        invoice.requests?.let {
            for ((index, request ) in it.withIndex()) {
                val serviceRequestView = HLEditInvoiceServiceRequestView(context,
                    object : HLEditInvoiceServiceRequestViewListener {
                        override fun onClickHorseAvatar(position: Int, horse: HLHorseModel) {
                            // Show public horse profile
                            replaceFragment(HLPublicHorseProfileFragment(horse))
                        }

                        override fun onClickCalenderView(position: Int) {
                            hideKeyboard()
                            // Show calendar activity
                            selectedSectionIndex = position
                            showCalendar(Date().time, null, invoice.requests?.get(position)?.requestDate)
                        }
                    })
                serviceRequestView.setData(index, request)
                requestViews.add(serviceRequestView)
                requestsLayout?.addView(serviceRequestView)
            }
        }

        this.setProgressButton(saveButton)
        this.setProgressButton(deleteButton)

        saveButton?.setOnClickListener {

            invoice.requests?.let {
                for (request in it) {
                    if (
                        request.services.any { service ->
                            service.service.isEmpty() || service.rate == 0.0f
                        }
                    ) {
                        showErrorMessage("Please enter valid service!")
                        return@setOnClickListener
                    }
                }

                showProgressButton(saveButton)
                HLFirebaseService.instance.putInvoice(invoice, invoice.uid.isEmpty(), object : ResponseCallback<HLInvoiceModel> {
                    override fun onSuccess(data: HLInvoiceModel) {
                        hideProgressButton(saveButton)
                        EventBus.getDefault().post(HLEventPaymentInvoiceRefresh(data))
                        popFragment()
                    }

                    override fun onFailure(error: String) {
                        showError(error)
                        hideProgressButton(saveButton)
                    }
                })
            }
        }

        deleteButton?.setOnClickListener {
            val dialog = android.app.AlertDialog.Builder(activity)
            dialog.apply {
                setTitle(getString(R.string.app_name))
                setMessage("Are you sure you want to delete this invoice? All requests on this invoice will also be deleted.")
                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    showProgressButton(deleteButton, ContextCompat.getColor(context, R.color.colorSecondaryButtonProgressBar))

                    // If this is a draft, delete all requests.
                    if (invoice.uid.isEmpty() && !invoice.requests.isNullOrEmpty()) {
                        HLFirebaseService.instance.deleteDraft(invoice.requests!!, object : ResponseCallback<Boolean> {
                            override fun onSuccess(data: Boolean) {
                                EventBus.getDefault().post(HLEventPaymentInvoiceRefresh(invoice))
                                popToMain()
                            }

                            override fun onFailure(error: String) {
                                showErrorMessage(error)
                            }
                        })
                    } else {
                        HLFirebaseService.instance.deleteInvoice(invoice.uid, object : ResponseCallback<Boolean> {
                            override fun onSuccess(data: Boolean) {
                                EventBus.getDefault().post(HLEventPaymentInvoiceDeleted(invoice.uid))
                                popToMain()
                            }

                            override fun onFailure(error: String) {
                                showErrorMessage(error)
                            }
                        })
                    }
                }.show()

            }
        }
    }

    private fun getServices() {
        showProgressDialog()
        HLGlobalData.me?.serviceProvider?.userId?.let {
            HLFirebaseService.instance.getProviderServices(it, object :
                ResponseCallback<List<HLServiceProviderServiceModel>> {
                override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                    hideProgressDialog()
                    providerServices.addAll(data)
                }

                override fun onFailure(error: String) {
                    hideProgressDialog()
                }
            })
        }
    }

    // MARK: - Actions


    // MARK: - Custom classes
    inner class HLEditInvoiceServiceRequestView(context: Context?, val listener: HLEditInvoiceServiceRequestViewListener): LinearLayout(context) {

        private var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_edit_invoice_service_request, this)
        private var avatarImageView: ImageView = containerView.findViewById(R.id.avatarImageView)
        private var horseBarnNameTextView: TextView = containerView.findViewById(R.id.horseBarnNameTextView)
        private var calendarLayout: LinearLayout = containerView.findViewById(R.id.calendarLayout)
        private var dateTextView: TextView = containerView.findViewById(R.id.dateTextView)
        private var showTextView: TextView = containerView.findViewById(R.id.showTextView)
        private var recyclerView: RecyclerView = containerView.findViewById(R.id.recyclerView)
        private var adapter: HLEditInvoiceSelectServiceAdapter? = null

        var serviceRequest: HLServiceRequestModel? = null
        var sectionIndex: Int? = null

        init {
            avatarImageView.setOnClickListener {
                if (sectionIndex == null || serviceRequest == null) return@setOnClickListener
                listener.onClickHorseAvatar(sectionIndex!!, serviceRequest?.horse!!)
            }
        }

        fun setData(sectionIndex: Int, serviceRequest: HLServiceRequestModel) {
            this.sectionIndex = sectionIndex
            this.serviceRequest = serviceRequest.copy()

            val size = context?.resources?.getDimensionPixelSize(R.dimen.size_avatar_small)
            size?.let {
                // Set horse profile avatar
                this.serviceRequest?.horse?.avatarUrl ?.let {
                    Picasso.get().load(it)
                        .resize(size, size)
                        .centerInside()
                        .placeholder(R.drawable.ic_horse_placeholder)
                        .into(avatarImageView)
                } ?: avatarImageView.setImageResource(R.drawable.ic_horse_placeholder)
            }

            // horse barn name
            horseBarnNameTextView.text = this.serviceRequest?.horse?.barnName

            // set date
            dateTextView.text = this.serviceRequest?.requestDate?.simpleDateString

            // set show name
            showTextView.text = this.serviceRequest?.show?.name

            // set current services
            calendarLayout.setOnClickListener {
                listener.onClickCalenderView(sectionIndex)
            }

            adapter?.let {
                recyclerView.adapter?.notifyDataSetChanged()
            }

            adapter?:let {
                if (serviceRequest.services.isNullOrEmpty()) {
                    serviceRequest.services = arrayListOf()
                }
                adapter = HLEditInvoiceSelectServiceAdapter(context,
                    sectionIndex,
                    serviceRequest.services,
                    object :
                    HLEditInvoiceSelectServiceAdapterListener {
                    override fun onClickDeleteServiceButton(sectionIndex: Int, position: Int) {
                        serviceRequest.services.removeAt(position)
                        recyclerView.adapter?.notifyDataSetChanged()
                    }

                    override fun onClickSelectServiceButton(sectionIndex: Int, position: Int) {
                        if (providerServices.isEmpty()) {
                            showInfoMessage("No selectable services")
                            return
                        }

                        val psList = providerServices.filter { provider ->
                            null == serviceRequest.services.firstOrNull { it.uid == provider.uid }
                        }
                        val items = psList.map {
                            it.selected = false
                            it.quantity = 1
                            "${it.service} - ${"$%.02f".format(it.rate)}"
                        }

                        if (items.isEmpty()) {
                            showInfoMessage("You've selected all services")
                            return
                        }

                        val adapter = ArrayAdapter<String>(activity!!, R.layout.item_select_service_dlg, items)
                        val dlg = AlertDialog.Builder(activity!!)
                            .setCancelable(true)
                            .setAdapter(adapter, null)
                            .setPositiveButton("Done") { _, _ ->
                                serviceRequest.services.addAll(psList.filter { it.selected })
                                recyclerView.adapter?.notifyDataSetChanged()
                            }
                            .setNegativeButton("Close") { _, _ ->
                            }
                            .create().apply {
                                listView.itemsCanFocus = false
                                listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
                                listView.setOnItemClickListener { _, view, position, _ ->
                                    psList[position].selected = (view as CheckedTextView).isChecked
                                }
                            }

                        dlg.show()
                    }

                    override fun onClickAddCustomServiceButton(sectionIndex: Int, position: Int) {
                        HLGlobalData.me?.serviceProvider?.userId?.let {
                            val service = HLServiceProviderServiceModel()
                            service.userId = it
                            serviceRequest.services.add(service)
                            recyclerView.adapter?.notifyDataSetChanged()
                        }
                    }
                })

                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }
        }
    }

}