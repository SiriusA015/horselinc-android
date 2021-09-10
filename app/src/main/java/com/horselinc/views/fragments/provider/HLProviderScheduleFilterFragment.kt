package com.horselinc.views.fragments.provider


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.horselinc.*
import com.horselinc.models.data.HLServiceRequestFilterModel
import com.horselinc.models.data.HLSortModel
import com.horselinc.models.event.HLProviderFilterEvent
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.listeners.HLSpinnerDialogListener
import org.greenrobot.eventbus.EventBus


class HLProviderScheduleFilterFragment(private var filter: HLServiceRequestFilterModel?) : HLBaseFragment() {

    private var sortButton: Button? = null
    private var startDateButton: Button? = null
    private var endDateButton: Button? = null

    private var isStartDate = true

    private val sortByList = arrayListOf("None",
        "Horse Barn Name (ascending)",
        "Horse Barn Name (descending)",
        "Horse Show Name (ascending)",
        "Horse Show Name (descending)")

    private var selectedSortByIndex: Int = 0
        set(value) {
            field = value
            if (value > 0) {
                sortButton?.text = sortByList[value]
            } else {
                sortButton?.text = getString(R.string.select)
            }
        }

    private var startDate: Long? = filter?.startDate
        set (value) {
            field = value
            if (value != null) {
                startDateButton?.text = value.date.formattedString("EEE, MMMM d, YYYY")
            } else {
                startDateButton?.text = getString(R.string.select)
            }
        }


    private var endDate: Long? = filter?.startDate
        set(value) {
            field = value
            if (value != null) {
                endDateButton?.text = value.date.formattedString("EEE, MMMM d, YYYY")
            } else {
                endDateButton?.text = getString(R.string.select)
            }
        }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_schedule_filter, container, false)

            initControls ()
        }

        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ActivityRequestCode.SELECT_DATE) {
                val selectedDate = data?.getLongExtra(IntentExtraKey.CALENDAR_RETURN_DATE, 0L)
                selectedDate?.let {
                    if (isStartDate) {
                        startDate = it
                    } else {
                        endDate = it
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }



    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        sortButton = rootView?.findViewById(R.id.sortButton)
        startDateButton = rootView?.findViewById(R.id.startDateButton)
        endDateButton = rootView?.findViewById(R.id.endDateButton)

        startDate = filter?.startDate
        endDate = filter?.endDate

        filter?.sort?.let { sort ->
            sort.order?.let { order ->
                selectedSortByIndex = when (sort.name) {
                    "horseBarnName" ->  if (order == HLSortOrder.ASC) 1 else 2
                    "horseDisplayName" -> if (order == HLSortOrder.ASC) 3 else 4
                    else -> 0
                }
            }
        }


        // event
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        sortButton?.setOnClickListener { onClickSort () }
        startDateButton?.setOnClickListener { onClickStartDate () }
        endDateButton?.setOnClickListener { onClickEndDate () }
        rootView?.findViewById<Button>(R.id.applyButton)?.setOnClickListener { onClickApply () }
        rootView?.findViewById<Button>(R.id.clearButton)?.setOnClickListener { onClickClear () }
    }


    /**
     *  Event Handlers
     */
    private fun onClickSort () {
        fragmentManager?.let { fm ->
            val fragment = HLSpinnerDialogFragment (
                "Sort by",
                sortByList,
                if (selectedSortByIndex == -1) 0 else selectedSortByIndex,
                object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        selectedSortByIndex = position
                    }
                })
            fragment.show(fm, "Select Sort by Fragment")
        }
    }

    private fun onClickStartDate () {
        isStartDate = true
        showCalendar(selectedDate = startDate)
    }

    private fun onClickEndDate () {
        isStartDate = false
        showCalendar(startDate = startDate, selectedDate = endDate)
    }

    private fun onClickApply() {
        setFilterData()
        popFragment()
    }

    private fun onClickClear () {
        selectedSortByIndex = 0
        startDate = null
        endDate = null
        setFilterData()
    }

    private fun setFilterData () {
        filter = HLServiceRequestFilterModel ()

        when (selectedSortByIndex) {
            1 -> {
                filter?.sort = HLSortModel()
                filter?.sort?.name = "horseBarnName"
                filter?.sort?.order = HLSortOrder.ASC
            }
            2 -> {
                filter?.sort = HLSortModel()
                filter?.sort?.name = "horseBarnName"
                filter?.sort?.order = HLSortOrder.DESC
            }
            3 -> {
                filter?.sort = HLSortModel()
                filter?.sort?.name = "horseDisplayName"
                filter?.sort?.order = HLSortOrder.ASC
            }
            4 -> {
                filter?.sort = HLSortModel()
                filter?.sort?.name = "horseDisplayName"
                filter?.sort?.order = HLSortOrder.DESC
            }
            else -> filter?.sort = null
        }
        filter?.startDate = startDate
        filter?.endDate = endDate

        EventBus.getDefault().post(HLProviderFilterEvent (filter))
    }
}
