package com.horselinc.views.fragments.manager


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.data.HLServiceShowModel
import com.horselinc.views.adapters.recyclerview.HLProviderServiceAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.jude.easyrecyclerview.EasyRecyclerView
import kotlinx.android.synthetic.main.fragment_manager_confirm_service_request.*
import java.util.*


class HLManagerConfirmServiceRequestFragment(private val request: HLServiceRequestModel) : HLBaseFragment() {

    private var dateTextView: TextView? = null
    private var showTextView: TextView? = null
    private var classTextView: TextView? = null
    private var providerTextView: TextView? = null
    private var assignerTitleTextView: TextView? = null
    private var assignerTextView: TextView? = null
    private var servicesRecyclerView: EasyRecyclerView? = null
    private var instructionTitleTextView: TextView? = null
    private var instructionTextView: TextView? = null

    private lateinit var servicesAdapter: HLProviderServiceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_confirm_service_request, container, false)

            initControls ()

            setRequestData ()
        }
        return rootView
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        dateTextView = rootView?.findViewById(R.id.dateTextView)
        showTextView = rootView?.findViewById(R.id.showTextView)
        classTextView = rootView?.findViewById(R.id.classTextView)
        providerTextView = rootView?.findViewById(R.id.providerTextView)
        assignerTitleTextView = rootView?.findViewById(R.id.assignerTitleTextView)
        assignerTextView = rootView?.findViewById(R.id.assignerTextView)
        servicesRecyclerView = rootView?.findViewById(R.id.servicesRecyclerView)
        instructionTitleTextView = rootView?.findViewById(R.id.instructionTitleTextView)
        instructionTextView = rootView?.findViewById(R.id.instructionTextView)

        // initialize recycler view
        servicesAdapter = HLProviderServiceAdapter(activity)
        servicesRecyclerView?.run {
            adapter = servicesAdapter
            setLayoutManager(LinearLayoutManager(activity))
        }

        // bind progress buttons
        setProgressButton(confirmButton)

        // event handler
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<Button>(R.id.confirmButton)?.setOnClickListener { onClickConfirm() }
    }

    private fun setRequestData () {

        // date
        dateTextView?.text = request.requestDate.date.formattedString("EEEE, MMMM d, YYYY")

        // show
        showTextView?.text = request.show?.name

        // class
        classTextView?.text = request.competitionClass ?: "N/A"

        // provider
        providerTextView?.text = request.serviceProvider?.name

        // assigner
        if (request.assignerId == null) {
            assignerTitleTextView?.visibility = View.GONE
            assignerTextView?.visibility = View.GONE
        } else {
            assignerTitleTextView?.visibility = View.VISIBLE
            assignerTextView?.visibility = View.VISIBLE

            assignerTextView?.text = request.assigner?.name
        }

        // services
        servicesAdapter.clear()
        servicesAdapter.addAll(request.services)

        // instruction
        if ((request.instruction ?: "").isEmpty()) {
            instructionTitleTextView?.visibility = View.GONE
            instructionTextView?.visibility = View.GONE
        } else {
            instructionTitleTextView?.visibility = View.VISIBLE
            instructionTextView?.visibility = View.VISIBLE

            instructionTextView?.text = request.instruction
        }
    }

    /**
     *  Event Handler
     */
    private fun onClickConfirm () {
        hideKeyboard()
        showProgressButton(confirmButton)

        if (request.show != null && (request.show?.uid ?: "").isEmpty()) {
            HLFirebaseService.instance.putShow(request.show!!, object: ResponseCallback<HLServiceShowModel> {
                override fun onSuccess(data: HLServiceShowModel) {

                    request.showId = data.uid
                    saveServiceRequest ()
                }

                override fun onFailure(error: String) {
                    hideProgressButton(confirmButton, stringResId = R.string.confirm)
                    showError(error)
                }
            })
        } else {
            saveServiceRequest()
        }
    }

    private fun saveServiceRequest () {
        HLGlobalData.me?.uid?.let { userId ->
            if (request.uid.isNotEmpty()) { // update

                request.run {
                    creatorId = userId
                    updatedAt = Calendar.getInstance().timeInMillis
                }

                HLFirebaseService.instance.putServiceRequest(request, object: ResponseCallback<HLServiceRequestModel> {
                    override fun onSuccess(data: HLServiceRequestModel) {
                        hideProgressButton(confirmButton, stringResId = R.string.confirm)
                        popFragment(HLManagerHorseDetailFragment::class.java)
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(confirmButton, stringResId = R.string.confirm)
                        showError(error)
                    }
                })
            } else { // create
                request.run {
                    creatorId = userId
                    status = HLServiceRequestStatus.PENDING
                    createdAt = Calendar.getInstance().timeInMillis
                    updatedAt = Calendar.getInstance().timeInMillis
                }

                HLFirebaseService.instance.addServiceRequest(request, object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        hideProgressButton(confirmButton, stringResId = R.string.confirm)
                        popFragment(HLManagerHorseDetailFragment::class.java)
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(confirmButton, stringResId = R.string.confirm)
                        showError(error)
                    }
                })
            }
        } ?: hideProgressButton(confirmButton, stringResId = R.string.confirm)
    }
}
