package com.horselinc.views.fragments.provider


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.HLHorseUserSearchType
import com.horselinc.HLSortOrder
import com.horselinc.R
import com.horselinc.models.data.HLHorseFilterModel
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.event.HLRefreshHorsesEvent
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.fragments.manager.HLManagerHorseUserSpinnerDialogFragment
import com.horselinc.views.listeners.HLSpinnerDialogListener
import org.greenrobot.eventbus.EventBus

class HLProviderHorseFilterFragment(private var filter: HLHorseFilterModel?) : HLBaseFragment() {

    private var selectTextView: TextView? = null
    private var managerTextView: TextView? = null


    private val sortByList = listOf("None", "Barn Name (ascending)", "Barn Name (descending)",
        "Creation Date (ascending)", "Creation Date (descending)")
    private var selectedManager: HLHorseManagerModel? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_horse_filter, container, false)

            // initialize handlers
            initControls ()

            // set filter data
            setFilterData ()
        }

        return rootView
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        selectTextView = rootView?.findViewById(R.id.selectTextView)
        managerTextView = rootView?.findViewById(R.id.managerTextView)


        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<Button>(R.id.applyButton)?.setOnClickListener { onClickApply () }
        rootView?.findViewById<Button>(R.id.clearButton)?.setOnClickListener { onClickClear () }

        selectTextView?.setOnClickListener { onClickSelect () }
        managerTextView?.setOnClickListener { onClickManager () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickApply () {
        // set filter model
        filter?:let {
            filter = HLHorseFilterModel()
        }

        filter?.run {
            // sort by
            when (sortByList.indexOf(selectTextView?.text.toString())) {
                1 -> {
                    sortFieldName = "barnName"
                    sortOrder = HLSortOrder.ASC
                }
                2 -> {
                    sortFieldName = "barnName"
                    sortOrder = HLSortOrder.DESC
                }
                3 -> {
                    sortFieldName = "createdAt"
                    sortOrder = HLSortOrder.ASC
                }
                4 -> {
                    sortFieldName = "createdAt"
                    sortOrder = HLSortOrder.DESC
                }
                else -> {
                    sortFieldName = null
                }
            }

            // manager
            manager = selectedManager
        }

        if (filter?.isNullData == true) {
            popFragment ()
            return
        }

        EventBus.getDefault().post(HLRefreshHorsesEvent(filter))
        popFragment()
    }

    private fun onClickClear () {
        // reset filter options
        selectedManager = null

        selectTextView?.text = ""
        managerTextView?.text = ""

        filter = null

        EventBus.getDefault().post(HLRefreshHorsesEvent(filter))
    }

    private fun onClickSelect () {
        fragmentManager?.let { fm ->
            val selectedPosition = sortByList.indexOf(selectTextView?.text.toString())
            val fragment = HLSpinnerDialogFragment (
                "Sort by",
                sortByList,
                if (selectedPosition == -1) 0 else selectedPosition,
                object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        selectTextView?.text = if (position == 0) "" else (data as String)
                    }
                })
            fragment.show(fm, "Select Sort by Fragment")
        }
    }

    private fun onClickManager () {
        fragmentManager?.let { fm ->
            val fragment = HLManagerHorseUserSpinnerDialogFragment (
                HLHorseUserSearchType.MANAGER,
                selectedUser = selectedManager,
                listener = object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        selectedManager = data as HLHorseManagerModel
                        managerTextView?.text = if (position == 0) "" else selectedManager?.name
                    }
                })
            fragment.show(fm, "Select Manager Fragment")
        }
    }

    private fun setFilterData () {
        filter?.let {  filterModel ->
            // sort by
            selectTextView?.text = when (filterModel.sortFieldName) {
                "barnName" -> {
                    val index = if (filterModel.sortOrder == HLSortOrder.ASC) 1 else 2
                    sortByList[index]
                }
                "createdAt" -> {
                    val index = if (filterModel.sortOrder == HLSortOrder.ASC) 3 else 4
                    sortByList[index]
                }
                else -> ""
            }

            // owners
            managerTextView?.text = filterModel.manager?.name
            selectedManager = filterModel.manager
        }
    }
}
