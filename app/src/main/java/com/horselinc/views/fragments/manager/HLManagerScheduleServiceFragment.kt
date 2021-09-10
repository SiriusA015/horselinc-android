package com.horselinc.views.fragments.manager


import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceProviderModel
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.data.HLServiceShowModel
import com.horselinc.models.event.HLSearchEvent
import com.horselinc.models.event.HLSelectBaseUserEvent
import com.horselinc.views.activities.HLSearchServiceShowActivity
import com.horselinc.views.activities.HLSearchUserActivity
import com.horselinc.views.adapters.recyclerview.HLInvoiceSelectServiceAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLSelectServicesListener
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class HLManagerScheduleServiceFragment (private val request: HLServiceRequestModel,
                                        private val isReassign: Boolean = false) : HLBaseFragment() {

    /**
     *  Controls
     */
    private var backButton: ImageView? = null
    private var dateButton: Button? = null
    private var showTextView: TextView? = null
    private var showEditText: EditText? = null
    private var classEditText: EditText? = null
    private var providerImageView: RoundedImageView? = null
    private var providerNameTextView: TextView? = null
    private var serviceRecyclerView: RecyclerView? = null
    private var noteEditText: EditText? = null
    private var nextButton: Button? = null


    /**
     *  Variables
     */
    private var services = ArrayList<HLServiceProviderServiceModel>()
    private var serviceShows = ArrayList<HLServiceShowModel>()
    private var customShow: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_schedule_service, container, false)

            // event bus
            EventBus.getDefault().register(this)

            // initialize controls
            initControls ()

            // set request data
            setRequestData()

            // get service shows
            getServiceShows ()
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ActivityRequestCode.SELECT_DATE) {
                val selectedDate = data?.getLongExtra(IntentExtraKey.CALENDAR_RETURN_DATE, 0L)
                selectedDate?.let {
                    dateButton?.text = it.date.formattedString("MMMM d, YYYY")
                    request.requestDate = it
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    /**
     *  Event Bus Handler
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedSearchEvent (event: HLSearchEvent) {
        try {
            when(event.data) {
//                is HLHorseModel -> showHorseInfo(event.data)
                is HLServiceShowModel -> {
                    request.show = event.data
                    request.showId = event.data.uid
                    customShow = null

                    setShow()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    fun onReceivedSelectUser (event: HLSelectBaseUserEvent) {
        try {
            when (event.selectType) {
                HLBaseUserSelectType.SERVICE_PROVIDER -> {
                    if (isReassign) {
                        request.assigner = event.baseUser as HLServiceProviderModel
                        request.assignerId = event.baseUser.userId
                    } else {
                        request.serviceProvider = event.baseUser as HLServiceProviderModel
                        request.serviceProviderId = event.baseUser.userId
                    }
                    services.clear()
                    request.services.clear()

                    setProvider()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        backButton = rootView?.findViewById(R.id.backImageView)
        dateButton = rootView?.findViewById(R.id.dateButton)
        showTextView = rootView?.findViewById(R.id.showTextView)
        showEditText = rootView?.findViewById(R.id.showEditText)
        classEditText = rootView?.findViewById(R.id.classEditText)
        providerImageView = rootView?.findViewById(R.id.providerImageView)
        providerNameTextView = rootView?.findViewById(R.id.providerNameTextView)
        serviceRecyclerView = rootView?.findViewById(R.id.existServiceRecyclerView)
        noteEditText = rootView?.findViewById(R.id.noteEditText)
        nextButton = rootView?.findViewById(R.id.nextButton)

        // initialize service recycler view
        if (isReassign) {
            request.services.clear()
        }
        serviceRecyclerView?.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        serviceRecyclerView?.adapter = HLInvoiceSelectServiceAdapter(activity, request.services) { position ->
            activity?.let {act ->
                AlertDialog.Builder(act)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.msg_alert_delete_service)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes) { dialog, _ ->

                        request.services.removeAt(position)
                        serviceRecyclerView?.adapter?.notifyDataSetChanged()

                        dialog.dismiss()
                    }
                    .show()
            }
        }


        // event handlers
        backButton?.setOnClickListener { popFragment() }
        dateButton?.setOnClickListener { onClickDate () }
        rootView?.findViewById<ConstraintLayout>(R.id.showContainer)?.setOnClickListener { onClickShows () }
        rootView?.findViewById<CardView>(R.id.providerCardView)?.setOnClickListener { onClickProvider () }
        rootView?.findViewById<CardView>(R.id.serviceCardView)?.setOnClickListener { onClickService () }
        nextButton?.setOnClickListener { onClickNext () }

        showEditText?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) { }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = showEditText?.text?.trim().toString()
                if (text.isNotEmpty()) {
                    request.show = null
                    request.showId = null
                    showTextView?.text = null
                    customShow = text
                }
            }
        })
    }

    private fun getServiceShows () {
        HLFirebaseService.instance.getServiceShows(object: ResponseCallback<List<HLServiceShowModel>> {
            override fun onSuccess(data: List<HLServiceShowModel>) {
                serviceShows.clear()
                serviceShows.addAll(data)
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }

    /**
     *  Event Handlers
     */
    private fun onClickDate () {
        hideKeyboard()
        showCalendar(selectedDate = request.requestDate)
    }

    private fun onClickShows () {
        hideKeyboard()
        startActivity(Intent(activity, HLSearchServiceShowActivity::class.java))
    }

    private fun onClickProvider () {
        hideKeyboard()

        // build exclude user
        val excludeUsers = ArrayList<String>()

        HLGlobalData.me?.let {
            excludeUsers.add(it.uid)
        }

        if (isReassign) {
            request.assignerId?.let {
                excludeUsers.add(it)
            }
        } else {
            excludeUsers.add(request.serviceProviderId)
        }

        val intent = Intent (activity, HLSearchUserActivity::class.java)
        intent.putExtra(IntentExtraKey.BASE_USER_SELECT_TYPE, HLBaseUserSelectType.SERVICE_PROVIDER)
        intent.putStringArrayListExtra(IntentExtraKey.BASE_USER_EXCLUDE_IDS, excludeUsers)
        intent.putExtra(IntentExtraKey.BASE_USER_MY_SERVICE_PROVIDER, true)
        startActivity(intent)
    }

    private fun onClickService () {
        hideKeyboard()

        if (isReassign) {
            if (request.assigner == null) {
                showInfoMessage("Please select a service provider")
                return
            }

            if (services.isEmpty()) {
                getServices (request.assigner!!)
            } else {
                showServices ()
            }
        } else {
            if (request.serviceProvider == null) {
                showInfoMessage("Please select a service provider")
                return
            }

            if (services.isEmpty()) {
                getServices (request.serviceProvider!!)
            } else {
                showServices ()
            }
        }

    }

    private fun onClickNext () {
        // validate service show
        if (request.show == null) {
            if (customShow != null && customShow?.isNotEmpty() == true) {
                if (serviceShows.indexOfFirst { it.name == customShow } >= 0) {
                    showInfoMessage("Please enter other show name")
                    return
                } else {
                    request.show = HLServiceShowModel().apply {
                        name = customShow ?: ""
                    }
                }
            }
        }

        // validate service provider
        if (isReassign) {
            request.assigner ?: showInfoMessage("Please select service provider")
        } else {
            request.serviceProvider ?: showInfoMessage("Please select service provider")
        }

        // validate services
        if (request.services.isEmpty()) {
            showInfoMessage("Please select service")
            return
        }

        request.run {
            competitionClass = classEditText?.text?.trim().toString()
            instruction = noteEditText?.text?.trim().toString()
        }

        replaceFragment(HLManagerConfirmServiceRequestFragment(request))
    }

    private fun getServices (provider: HLServiceProviderModel) {
        HLFirebaseService.instance.getProviderServices(provider.userId, object: ResponseCallback<List<HLServiceProviderServiceModel>> {
            override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                services.addAll(data)
                showServices()
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }

    private fun showServices () {

        val serviceNames = ArrayList<String>()
        serviceNames.add("None")

        val restServices = if (request.services.isEmpty()) {
            services
        } else {
            services.filter { service ->
                val existServices = request.services.filter { requestService ->
                    requestService.uid == service.uid
                }

                existServices.isEmpty()
            }
        }

        showSelectService(restServices, object: HLSelectServicesListener {
            override fun onClickDone(selectedServices: List<HLServiceProviderServiceModel>) {
                request.services.addAll(selectedServices)
                serviceRecyclerView?.adapter?.notifyDataSetChanged()
            }
        })
    }


    /**
     *  Set Data Handlers
     */
    private fun setRequestData () {
        // date button
        dateButton?.text = request.getRequestDateCalendar().time.formattedString("MMMM d, YYYY")

        // show text
        setShow()

        // class name
        classEditText?.setText(request.competitionClass)

        // provider
        setProvider()

        // private note
        noteEditText?.setText(request.instruction)
    }

    private fun setShow () {
        showTextView?.text = request.show?.name
        showEditText?.setText(customShow)
    }

    private fun setProvider () {
        val provider = if (isReassign) request.assigner else request.serviceProvider
        providerImageView?.loadImage(provider?.avatarUrl, R.drawable.ic_profile)
        providerNameTextView?.text = provider?.name
    }
}
