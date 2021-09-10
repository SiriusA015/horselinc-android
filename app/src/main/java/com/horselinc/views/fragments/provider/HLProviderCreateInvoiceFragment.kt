package com.horselinc.views.fragments.provider

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.*
import com.horselinc.models.event.HLSearchEvent
import com.horselinc.views.activities.HLSearchHorseActivity
import com.horselinc.views.activities.HLSearchServiceShowActivity
import com.horselinc.views.adapters.recyclerview.HLInvoiceOwnerAdapter
import com.horselinc.views.adapters.recyclerview.HLInvoiceSelectServiceAdapter
import com.horselinc.views.fragments.HLEventBusFragment
import com.horselinc.views.listeners.HLSelectInvoiceContactItemListener
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList


/**
 *  Created by TengFei Li on 27, August, 2019
 */

class HLProviderCreateInvoiceFragment(private val horse: HLHorseModel?) : HLEventBusFragment() {

    private var tvSearchHorse: TextView? = null
    private var lytHorseInfo: View? = null
    private var ivHorseAvatar: ImageView? = null
    private var tvHorseName: TextView? = null
    private var tvHorseInfo: TextView? = null
    private var lytTrainer: View? = null
    private var ivTrainerAvatar: ImageView? = null
    private var tvTrainerName: TextView? = null
    private var lytOwners: View? = null
    private var lstOwners: RecyclerView? = null
    private var lytLeased: View? = null
    private var ivLeasedAvatar: ImageView? = null
    private var tvLeasedName: TextView? = null
    private var tvDate: TextView? = null
    private var tvSelectService: TextView? = null
    private var lstServices: RecyclerView? = null
    private var tvSearchByName: TextView? = null
    private var etShow: EditText? = null
    private var etCompetition: EditText? = null
    private var etNote: EditText? = null

    private var invoiceMethodContainer: LinearLayout? = null
    private var selectInvoiceMethodTextView: TextView? = null
    private var invoiceMethodLayout: ConstraintLayout? = null
    private var contactUserImageView: RoundedImageView? = null
    private var invoiceMethodTextView: TextView? = null


    private var curHorse: HLHorseModel? = null
    private var owners = ArrayList<HLHorseOwnerModel>()
    private var providerServices = ArrayList<HLServiceProviderServiceModel>()
    private var services = ArrayList<HLServiceProviderServiceModel>()
    private var show: HLServiceShowModel? = null
    private var isShowSet = false
    private lateinit var curDate: Date
    private var popFragment: Class<*>? = null

    /**
     *  Variables
     */
    private var invoiceMethodType: HLInvoiceMethodType = HLInvoiceMethodType.NONE
        set(value) {
            field = value
            when (value) {
                HLInvoiceMethodType.EMAIL -> selectInvoiceMethodTextView?.text = invoiceMethods[0]
                HLInvoiceMethodType.SMS -> selectInvoiceMethodTextView?.text = invoiceMethods[1]
                else -> selectInvoiceMethodTextView?.text = null
            }
        }

    private var selectedContact: HLPhoneContactModel? = null

    private val invoiceMethods = arrayOf("Email", "Text Message")
    private val phoneUtil = PhoneNumberUtil.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_provider_create_invoice, container, false)

            initControls()
            getServices()
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
        HLGlobalData.isCreateInvoice = true

        if (isShowSet) {
            etShow?.setText("")
            isShowSet = false
        }
    }

    override fun onStop() {
        super.onStop()
        HLGlobalData.isCreateInvoice = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ActivityRequestCode.SELECT_DATE -> {
                    val selectedDate = data?.getLongExtra(IntentExtraKey.CALENDAR_RETURN_DATE, 0L)
                    selectedDate?.let {
                        curDate = it.date
                        tvDate?.text = it.date.formattedString("MM/dd/yyyy")
                    }
                }
                else -> {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }


    /**
     *  Event Bus Receiver
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedSearchEvent (event: HLSearchEvent) {
        try {
            when(event.data) {
                is HLHorseModel -> showHorseInfo(event.data)
                is HLServiceShowModel -> showServiceShowInfo(event.data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun initControls() {
        // Determine the root screen to pop
        popFragment = if (horse != null) HLProviderHorseDetailFragment::class.java else null

        // controls
        tvSearchHorse = rootView?.findViewById(R.id.tvSearchHorse)
        lytHorseInfo = rootView?.findViewById(R.id.lytHorseInfo)
        ivHorseAvatar = rootView?.findViewById(R.id.ivHorseAvatar)
        tvHorseName = rootView?.findViewById(R.id.tvHorseName)
        tvHorseInfo = rootView?.findViewById(R.id.tvHorseInfo)
        lytTrainer = rootView?.findViewById(R.id.lytTrainer)
        ivTrainerAvatar = rootView?.findViewById(R.id.ivTrainerAvatar)
        tvTrainerName = rootView?.findViewById(R.id.tvTrainerName)
        lytOwners = rootView?.findViewById(R.id.lytOwners)
        lytLeased = rootView?.findViewById(R.id.lytLeased)
        ivLeasedAvatar = rootView?.findViewById(R.id.ivLeasedAvatar)
        tvLeasedName = rootView?.findViewById(R.id.tvLeasedName)
        tvDate = rootView?.findViewById(R.id.tvDate)
        tvSelectService = rootView?.findViewById(R.id.tvSelectService)
        lstServices = rootView?.findViewById(R.id.lstServices)
        tvSearchByName = rootView?.findViewById(R.id.tvSearchByName)
        etShow = rootView?.findViewById(R.id.etShow)
        etCompetition = rootView?.findViewById(R.id.etCompetition)
        etNote = rootView?.findViewById(R.id.etNote)

        invoiceMethodContainer = rootView?.findViewById(R.id.invoiceMethodContainer)
        selectInvoiceMethodTextView = rootView?.findViewById(R.id.selectInvoiceMethodTextView)
        invoiceMethodLayout = rootView?.findViewById(R.id.invoiceMethodLayout)
        contactUserImageView = rootView?.findViewById(R.id.contactUserImageView)
        invoiceMethodTextView = rootView?.findViewById(R.id.invoiceMethodTextView)

        invoiceMethodContainer?.visibility = View.GONE
        invoiceMethodLayout?.visibility = View.GONE

        lstOwners?.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        lstOwners?.adapter = HLInvoiceOwnerAdapter(activity, owners)
        lstServices?.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        lstServices?.adapter = HLInvoiceSelectServiceAdapter(activity, services) {
            services.removeAt(it)
            lstServices?.adapter?.notifyDataSetChanged()
        }

        curDate = Date()
        tvDate?.text = curDate.formattedString("MM/dd/yyyy")

        setShowEdit()

        horse?.let {
            showHorseInfo(horse)
        }

        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btBack)?.setOnClickListener {
            popFragment()
        }
        tvSearchHorse?.setOnClickListener { onClickSearchHorses () }
        rootView?.findViewById<LinearLayout>(R.id.lytHorse)?.setOnClickListener { onClickSearchHorses() }
        tvSelectService?.setOnClickListener { onSelectService() }
        rootView?.findViewById<TextView>(R.id.tvAddCustomService)?.setOnClickListener { onAddCustomService() }
        tvSearchByName?.setOnClickListener { onClickSearchShow () }
        tvDate?.setOnClickListener { showCalendar(selectedDate = curDate.time) }
        rootView?.findViewById<ImageView>(R.id.ivDate)?.setOnClickListener { showCalendar(selectedDate = curDate.time) }
        rootView?.findViewById<Button>(R.id.btNext)?.setOnClickListener { onNext() }
        selectInvoiceMethodTextView?.setOnClickListener { onClickSelectInvoiceMethod () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickSearchHorses () {
        val intent = Intent(activity, HLSearchHorseActivity::class.java).apply {
            putExtra(IntentExtraKey.CREATE_INVOICE, true)
        }
        startActivity(intent)
    }

    private fun onSelectService() {
        if (providerServices.isEmpty()) {
            showInfoMessage("No selectable services")
            return
        }

        val psList = providerServices.filter { provider ->
            null == services.firstOrNull { it.uid == provider.uid }
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
                services.addAll(psList.filter { it.selected })
                lstServices?.adapter?.notifyDataSetChanged()
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

    private fun onClickSearchShow () {
        hideKeyboard()
        startActivity(Intent(activity, HLSearchServiceShowActivity::class.java))
    }

    private fun onAddCustomService() {
        HLGlobalData.me?.serviceProvider?.userId?.let {
            val service = HLServiceProviderServiceModel()
            service.userId = it
            services.add(service)
            lstServices?.adapter?.notifyDataSetChanged()
        }
    }

    private fun onNext() {
        hideKeyboard()

        var email: String? = null
        var phone: String? = null
        if (curHorse?.creatorId.isNullOrEmpty()) {
            when {
                selectedContact == null -> {
                    showInfoMessage("Please set invoice method.")
                    return
                }
                invoiceMethodType == HLInvoiceMethodType.EMAIL -> {
                    selectedContact?.emails?.let {
                        email = if (it.isEmpty()) null else it.first()
                    }
                }
                invoiceMethodType == HLInvoiceMethodType.SMS -> {
                    try {
                        val phoneProto = phoneUtil.parse(selectedContact?.phoneNumbers?.first(), Locale.getDefault().country)
                        phone = phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164)
                    } catch (e: NumberParseException) {
                        showInfoMessage("Invalid phone number.")
                        return
                    }
                }
            }
        }

        val request = HLServiceRequestModel()
        request.horse = curHorse
        request.horse?.apply {
            request.horseId = uid
            request.horseBarnName = barnName
            request.horseDisplayName = displayName
        }

        request.requestDate = curDate.time
        request.services = services
        request.services.forEach { it.userId = HLGlobalData.me?.serviceProvider?.userId ?: "" }
        request.show = if ((etShow?.text ?: "").isNotBlank()) {
            HLServiceShowModel().apply {
                name = etShow?.text?.trim().toString()
                createdAt = Date().time
            }
        } else {
            show
        }

        if ((etCompetition?.text ?: "").isNotBlank()) request.competitionClass = etCompetition?.text.toString()
        if ((etNote?.text ?: "").isNotBlank()) request.instruction = etNote?.text.toString()

        if (!checkDataIntegrity(request)) {
            return
        }

        if (null == HLGlobalData.me?.serviceProvider?.userId) return

        request.serviceProviderId = HLGlobalData.me?.serviceProvider?.userId ?: ""
        request.serviceProvider = HLGlobalData.me?.serviceProvider

        request.status = HLServiceRequestStatus.COMPLETED
        request.isCustomRequest = true
        request.showId = request.show?.uid

        // When create an invoice manually, doesnt' need to set assigner info
        request.assignerId = null
        request.assigner = null
        request.createdAt = Date().time
        request.updatedAt = Date().time
        request.creatorId = HLGlobalData.me!!.uid

        replaceFragment(HLProviderConfirmInvoiceFragment(request, popFragment, selectedContact?.name, email, phone))
    }

    private fun onClickSelectInvoiceMethod () {
        hideKeyboard()

        activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Select Invoice Method")
                .setItems(invoiceMethods) { _, which ->

                    val selectedInvoice = if (which == 0) HLInvoiceMethodType.EMAIL else HLInvoiceMethodType.SMS

                    replaceFragment(HLProviderInvoiceMethodFragment(selectedInvoice,
                        object : HLSelectInvoiceContactItemListener {
                            override fun onClickInvoiceContact(invoiceMethod: HLInvoiceMethodType, contact: HLPhoneContactModel) {
                                invoiceMethodType = invoiceMethod
                                onSelectContact (contact)
                            }
                        }))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onSelectContact (contact: HLPhoneContactModel) {
        selectedContact = contact

        invoiceMethodLayout?.visibility = View.VISIBLE

        contactUserImageView?.loadImage(contact.photoUri, R.drawable.ic_profile_placeholder)

        invoiceMethodTextView?.run {
            val contactInfo = if (invoiceMethodType == HLInvoiceMethodType.EMAIL) contact.emails.first() else contact.phoneNumbers.first()
            text = if (contact.name.isEmpty()) {
                contactInfo
            } else {
                "${contact.name}\n$contactInfo"
            }
        }
    }


    /**
     *  Get Data Handler
     */
    private fun getServices() {
        HLGlobalData.me?.serviceProvider?.userId?.let {
            HLFirebaseService.instance.getProviderServices(it, object : ResponseCallback<List<HLServiceProviderServiceModel>> {
                override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                    providerServices.addAll(data)
                }
            })
        }
    }


    /**
     *  Set Data Handlers
     */
    private fun setShowEdit() {
        etShow?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (s.toString().isNotEmpty()) {
                    if (!isShowSet) {
                        show = null
                        tvSearchByName?.text = ""
                    }
                }
            }
        })
    }

    private fun showHorseInfo(horse: HLHorseModel) {
        curHorse = horse

        // invoice method
        if (horse.creatorId.isEmpty()) {
            invoiceMethodContainer?.visibility = View.VISIBLE
        } else {
            invoiceMethodContainer?.visibility = View.GONE
        }

        tvSearchHorse?.visibility = View.GONE
        lytHorseInfo?.visibility = View.VISIBLE

        ivHorseAvatar?.loadImage(horse.avatarUrl, R.drawable.ic_profile)
        tvHorseName?.text = horse.displayName
        tvHorseInfo?.text = horse.barnName

        lytTrainer?.visibility = horse.trainer?.let {
            ivTrainerAvatar?.loadImage(it.avatarUrl, R.drawable.ic_profile)
            tvTrainerName?.text = it.barnName
            View.VISIBLE
        } ?: View.GONE

        if (horse.owners.isNullOrEmpty()) {
            lytOwners?.visibility = View.GONE
        } else {
            lytOwners?.visibility = View.VISIBLE
            owners.clear()
            owners.addAll(horse.owners!!)
        }

        lytLeased?.visibility = horse.leaser?.let {
            ivLeasedAvatar?.loadImage(it.avatarUrl, R.drawable.ic_profile)
            tvLeasedName?.text = it.name
            View.VISIBLE
        } ?: View.GONE
    }

    private fun showServiceShowInfo(show: HLServiceShowModel) {
        isShowSet = true

        this.show = show
        tvSearchByName?.text = show.name
    }

    private fun checkDataIntegrity(request: HLServiceRequestModel) : Boolean {

        if (request.horseId.isEmpty()) {
            showErrorMessage("Please select a horse")
            return false
        }

        if (request.services.isEmpty()) {
            showErrorMessage("Please add at least one service.")
            return false
        }

        for (service in request.services) {
            if (service.service.isEmpty() || service.quantity == 0 || service.rate == 0.0f) {
                showErrorMessage("All services must have a valid name, rate of at least $1.00, and whole number quantity greater than 0 - numeric characters only.")
                return false
            }
        }

        return true
    }
}