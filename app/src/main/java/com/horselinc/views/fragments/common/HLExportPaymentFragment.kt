package com.horselinc.views.fragments.common


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.event.HLSearchEvent
import com.horselinc.models.event.HLSelectBaseUserEvent
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.activities.HLSearchHorseActivity
import com.horselinc.views.activities.HLSearchUserActivity
import com.horselinc.views.adapters.recyclerview.HLEditBaseUserAdapter
import com.horselinc.views.adapters.recyclerview.HLEditHorseAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.listeners.HLEditBaseUserItemListener
import com.horselinc.views.listeners.HLEditHorseItemListener
import com.horselinc.views.listeners.HLSpinnerDialogListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.jude.easyrecyclerview.decoration.DividerDecoration


class HLExportPaymentFragment(private val userType: String) : HLBaseFragment() {

    private var serviceTextView: TextView? = null
    private var fromDateTextView: TextView? = null
    private var toDateTextView: TextView? = null
    private var userTitleTextView: TextView? = null
    private var userRecyclerView: RecyclerView? = null
    private var addUserTextView: TextView? = null
    private var userSearchImageView: ImageView? = null
    private var horseRecyclerView: RecyclerView? = null
    private var addHorseTextView: TextView? = null
    private var horseSearchImageView: ImageView? = null
    private var exportButton: Button? = null

    private lateinit var userAdapter: HLEditBaseUserAdapter
    private lateinit var horseAdapter: HLEditHorseAdapter

    private val services = arrayListOf("All Payments", "Outstanding Payments", "Completed Payments")

    private var isSelectFromDate = true
    private var startDate: Long? = null
        set(value) {
            field = value
            fromDateTextView?.text = value?.date?.formattedString("MM/dd/yyyy")
        }

    private var endDate: Long? = null
        set(value) {
            field = value
            toDateTextView?.text = value?.date?.formattedString("MM/dd/yyyy")
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_export_payment, container, false)

            EventBus.getDefault().register(this)

            initControls ()
        }
        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ActivityRequestCode.SELECT_DATE) {
                val selectedDate = data?.getLongExtra(IntentExtraKey.CALENDAR_RETURN_DATE, 0L)
                selectedDate?.let {
                    if (isSelectFromDate) {
                        startDate = it
                        endDate = null
                    } else {
                        endDate = it
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedSelectUserEvent (event: HLSelectBaseUserEvent) {
        try {
            if (event.selectType == HLBaseUserSelectType.SERVICE_PROVIDER || event.selectType == HLBaseUserSelectType.HORSE_MANAGER) {
                userAdapter.add(event.baseUser)

                addUserTextView?.text = if (userType == HLUserType.MANAGER) {
                    "Add Another Service Provider"
                } else {
                    "Add Another Horse Manager"
                }
                val color = ResourceUtil.getColor(R.color.colorTeal)
                addUserTextView?.setTextColor(color)
                userSearchImageView?.setImageResource(R.drawable.ic_add_circle_black)
                userSearchImageView?.setColorFilter(color)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedSearchHorseEvent (event: HLSearchEvent) {
        try {
            if (event.data is HLHorseModel) {
                horseAdapter.add(event.data)

                addHorseTextView?.text = "Add Another Horse"
                val color = ResourceUtil.getColor(R.color.colorTeal)
                addHorseTextView?.setTextColor(color)
                horseSearchImageView?.setImageResource(R.drawable.ic_add_circle_black)
                horseSearchImageView?.setColorFilter(color)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }



    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        serviceTextView = rootView?.findViewById(R.id.serviceTextView)
        fromDateTextView = rootView?.findViewById(R.id.fromDateTextView)
        toDateTextView = rootView?.findViewById(R.id.toDateTextView)
        userTitleTextView = rootView?.findViewById(R.id.userTitleTextView)
        userRecyclerView = rootView?.findViewById(R.id.userRecyclerView)
        addUserTextView = rootView?.findViewById(R.id.addUserTextView)
        userSearchImageView = rootView?.findViewById(R.id.userSearchImageView)
        horseRecyclerView = rootView?.findViewById(R.id.horseRecyclerView)
        addHorseTextView = rootView?.findViewById(R.id.addHorseTextView)
        horseSearchImageView = rootView?.findViewById(R.id.horseSearchImageView)
        exportButton = rootView?.findViewById(R.id.exportButton)

        // initialize data
        serviceTextView?.text = services[0]

        userTitleTextView?.text = if (userType == HLUserType.MANAGER) {
            "Service Providers"
        } else {
            "Horse Managers"
        }

        // bind progress buttons
        setProgressButton(exportButton)


        // initialize user recycler view
        userAdapter = HLEditBaseUserAdapter(activity, object: HLEditBaseUserItemListener {
            override fun onClickDelete(position: Int, user: HLBaseUserModel) {
                userAdapter.remove(position)

                if (userAdapter.count == 0) {
                    addUserTextView?.text = getString(R.string.tap_to_search)
                    addUserTextView?.setTextColor(Color.BLACK)
                    userSearchImageView?.setImageResource(R.drawable.ic_search_black)
                    userSearchImageView?.setColorFilter(Color.BLACK)
                }
            }
        })
        userRecyclerView?.run {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)

            // add decoration
            val itemDecoration = DividerDecoration(
                Color.parseColor("#1e000000"),
                ResourceUtil.dpToPx(1),
                ResourceUtil.dpToPx(16),
                0
            ) //color & height & paddingLeft & paddingRight
            itemDecoration.setDrawLastItem(false) //sometimes you don't want draw the divider for the last item,default is true.
            itemDecoration.setDrawHeaderFooter(false) //whether draw divider for header and footer,default is false.
            addItemDecoration(itemDecoration)
        }

        // initialize horse recycler view
        horseAdapter = HLEditHorseAdapter(activity, object: HLEditHorseItemListener {
            @SuppressLint("SetTextI18n")
            override fun onClickDelete(position: Int, horse: HLHorseModel) {
                horseAdapter.remove(horse)

                if (horseAdapter.count == 0) {
                    addHorseTextView?.text = getString(R.string.tap_to_search)
                    addHorseTextView?.setTextColor(Color.BLACK)
                    horseSearchImageView?.setImageResource(R.drawable.ic_search_black)
                    horseSearchImageView?.setColorFilter(Color.BLACK)
                }
            }
        })
        horseRecyclerView?.run {
            adapter = horseAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        serviceTextView?.setOnClickListener { onClickService () }
        fromDateTextView?.setOnClickListener { onClickFromDate () }
        toDateTextView?.setOnClickListener { onClickToDate () }
        rootView?.findViewById<ConstraintLayout>(R.id.addUserContainer)?.setOnClickListener { onClickAddUser () }
        rootView?.findViewById<ConstraintLayout>(R.id.addHorseContainer)?.setOnClickListener { onClickAddHorse () }
        exportButton?.setOnClickListener { onClickExport () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickService () {
        fragmentManager?.let { fm ->
            val selectedPosition = services.indexOf(serviceTextView?.text.toString())
            val fragment = HLSpinnerDialogFragment (
                "Select Service",
                services,
                if (selectedPosition == -1) 0 else selectedPosition,
                object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        serviceTextView?.text = data as String
                    }
                })

            fragment.show(fm, "Select Service Fragment")
        }
    }

    private fun onClickFromDate () {
        isSelectFromDate = true
        showCalendar(selectedDate = startDate)
    }

    private fun onClickToDate () {
        isSelectFromDate = false
        showCalendar(startDate = startDate, selectedDate = endDate)
    }

    private fun onClickAddUser () {
        val selectedType = if (userType == HLUserType.MANAGER) {
            HLBaseUserSelectType.SERVICE_PROVIDER
        } else {
            HLBaseUserSelectType.HORSE_MANAGER
        }

        // build exclude users
        val excludeUsers = ArrayList<String>()

        HLGlobalData.me?.let {
            excludeUsers.add(it.uid)
        }

        if (userAdapter.count > 0) {
            excludeUsers.addAll(userAdapter.allData.map { it.userId })
        }

        val intent = Intent (activity, HLSearchUserActivity::class.java)
        intent.putExtra(IntentExtraKey.BASE_USER_SELECT_TYPE, selectedType)
        intent.putStringArrayListExtra(IntentExtraKey.BASE_USER_EXCLUDE_IDS, excludeUsers)
        startActivity(intent)
    }

    private fun onClickAddHorse () {
        startActivity(Intent(activity, HLSearchHorseActivity::class.java))
    }

    private fun onClickExport () {
        HLGlobalData.me?.uid?.let { userId ->

            val status = when (services.indexOfFirst { serviceTextView?.text == it }) {
                1 -> HLInvoiceStatusType.SUBMITTED
                2 -> HLInvoiceStatusType.PAID
                else -> null
            }

            val providerIds = if (userType == HLUserType.MANAGER) {
                userAdapter.allData.map { it.userId }
            } else {
                null
            }

            val managerIds = if (userType == HLUserType.PROVIDER) {
                userAdapter.allData.map { it.userId }
            } else {
                null
            }

            showProgressButton(exportButton)
            HLFirebaseService.instance.exportInvoices(userId, status, startDate, endDate, providerIds, managerIds,
                horseAdapter.allData.map { it.uid }, object : ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        hideProgressButton(exportButton, stringResId = R.string.export_invoices)
                        showSuccessMessage(data)
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(exportButton, stringResId = R.string.export_invoices)
                        showError(error)
                    }
                })
        }
    }
}
