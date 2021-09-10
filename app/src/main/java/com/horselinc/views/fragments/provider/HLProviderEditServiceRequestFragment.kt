package com.horselinc.views.fragments.provider


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLInvoiceSelectServiceAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLSelectServicesListener
import com.jude.easyrecyclerview.decoration.DividerDecoration
import com.makeramen.roundedimageview.RoundedImageView
import java.util.*
import kotlin.collections.ArrayList

class HLProviderEditServiceRequestFragment(private var request: HLServiceRequestModel) : HLBaseFragment() {

    private var horseProfileImageView: RoundedImageView? = null
    private var barnNameTextView: TextView? = null
    private var displayNameTextView: TextView? = null
    private var existServiceRecyclerView: RecyclerView? = null
    private var newServiceRecyclerView: RecyclerView? = null
    private var noteTitleTextView: TextView? = null
    private var noteTextView: TextView? = null
    private var saveButton: Button? = null

    private lateinit var existAdapter: HLInvoiceSelectServiceAdapter
    private lateinit var newAdapter: HLInvoiceSelectServiceAdapter

    private var selectedProviderServices: ArrayList<HLServiceProviderServiceModel> = ArrayList()
    private var newServices: ArrayList<HLServiceProviderServiceModel> = ArrayList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_edit_service_request, container, false)

            initControls ()

            setData ()
        }

        return rootView
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        horseProfileImageView = rootView?.findViewById(R.id.horseProfileImageView)
        barnNameTextView = rootView?.findViewById(R.id.barnNameTextView)
        displayNameTextView = rootView?.findViewById(R.id.displayNameTextView)
        existServiceRecyclerView = rootView?.findViewById(R.id.existServiceRecyclerView)
        newServiceRecyclerView = rootView?.findViewById(R.id.newServiceRecyclerView)
        noteTitleTextView = rootView?.findViewById(R.id.noteTitleTextView)
        noteTextView = rootView?.findViewById(R.id.noteTextView)
        saveButton = rootView?.findViewById(R.id.saveButton)

        // bind progress button
        setProgressButton(saveButton)

        // initialize recycler views
        existAdapter = HLInvoiceSelectServiceAdapter(activity, request.services, false) { position ->
            activity?.let {act ->
                AlertDialog.Builder(act)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.msg_alert_delete_service)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes) { dialog, _ ->

                        request.services.removeAt(position)
                        existServiceRecyclerView?.adapter?.notifyDataSetChanged()

                        dialog.dismiss()
                    }
                    .show()
            }
        }
        existServiceRecyclerView?.run {
            adapter = existAdapter
            layoutManager = LinearLayoutManager(activity)

            val itemDecoration = DividerDecoration(
                Color.parseColor("#1e000000"),
                ResourceUtil.dpToPx(1),
                16,
                0
            )
            itemDecoration.setDrawLastItem(false)
            itemDecoration.setDrawHeaderFooter(false)
            addItemDecoration(itemDecoration)
        }

        newAdapter = HLInvoiceSelectServiceAdapter(activity, newServices) { position ->
            activity?.let {act ->
                AlertDialog.Builder(act)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.msg_alert_delete_service)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes) { dialog, _ ->

                        newServices.removeAt(position)
                        newServiceRecyclerView?.adapter?.notifyDataSetChanged()

                        dialog.dismiss()
                    }
                    .show()
            }
        }
        newServiceRecyclerView?.run {
            adapter = newAdapter
            layoutManager = LinearLayoutManager(activity)

            val itemDecoration = DividerDecoration(
                Color.parseColor("#1e000000"),
                ResourceUtil.dpToPx(1),
                16,
                0
            )
            itemDecoration.setDrawLastItem(false)
            itemDecoration.setDrawHeaderFooter(false)
            addItemDecoration(itemDecoration)
        }


        // event handler
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<LinearLayout>(R.id.addServiceContainer)?.setOnClickListener { onClickAdd () }
        saveButton?.setOnClickListener { onClickSave () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickAdd () {
        HLGlobalData.me?.uid?.let { userId ->
            HLFirebaseService.instance.getProviderServices(userId, object: ResponseCallback<List<HLServiceProviderServiceModel>> {
                override fun onSuccess(data: List<HLServiceProviderServiceModel>) {

                    selectedProviderServices.clear()
                    selectedProviderServices.addAll(data)

                    showServices ()
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }

    private fun onClickSave () {
        hideKeyboard()

        if (newServices.isNotEmpty()) {

            showProgressButton(saveButton)
            request.services.addAll(newServices)
            request.updatedAt = Calendar.getInstance().timeInMillis
            HLFirebaseService.instance.updateServiceRequest(request, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    hideProgressButton(saveButton, stringResId = R.string.save)
                    popFragment()
                }

                override fun onFailure(error: String) {
                    hideProgressButton(saveButton, stringResId = R.string.save)
                    showError(error)
                }
            })
        } else {
            popFragment()
        }
    }

    private fun showServices () {
        val restServices = ArrayList<HLServiceProviderServiceModel>()

        val existServices = ArrayList<HLServiceProviderServiceModel>()
        existServices.addAll(request.services)
        existServices.addAll(newServices)

        if (existServices.isNotEmpty()) {
            restServices.addAll(selectedProviderServices.filter { service ->
                existServices.none { service.uid == it.uid }
            })
        } else {
            restServices.addAll(selectedProviderServices)
        }

        showSelectService(restServices, object: HLSelectServicesListener {
            override fun onClickDone(selectedServices: List<HLServiceProviderServiceModel>) {

                newServices.addAll(selectedServices)
                newServiceRecyclerView?.adapter?.notifyDataSetChanged()
            }
        })
    }

    /**
     *  Set Data Handlers
     */
    @SuppressLint("SetTextI18n")
    private fun setData () {

        horseProfileImageView?.loadImage(request.horse?.avatarUrl, R.drawable.ic_horse_placeholder)
        barnNameTextView?.text = request.horseBarnName
        displayNameTextView?.text = "\"${request.horseDisplayName}\""


    }


}
