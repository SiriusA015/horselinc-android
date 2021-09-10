package com.horselinc.views.fragments.manager


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.horselinc.HLHorseUserSearchType
import com.horselinc.HLSortOrder
import com.horselinc.R
import com.horselinc.models.data.HLHorseFilterModel
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.event.HLRefreshHorsesEvent
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.listeners.HLSpinnerDialogListener
import org.greenrobot.eventbus.EventBus

class HLManagerHorsesFilterFragment (private var filter: HLHorseFilterModel?) : HLBaseFragment() {

    /**
     *  Controls
     */
    private var backButton: ImageView? = null
    private var selectTextView: TextView? = null
    private var ownerTextView: TextView? = null
    private var trainerTextView: TextView? = null
    private var scheduledSwitch: Switch? = null
    private var applyButton: Button? = null
    private var clearButton: Button? = null

    /**
     *  Sort Data
     */
    private val sortByList = listOf("None", "Barn Name (ascending)", "Barn Name (descending)",
        "Creation Date (ascending)", "Creation Date (descending)")
    private var selectedOwner: HLHorseManagerModel? = null
    private var selectedTrainer: HLHorseManagerModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // root view
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_horses_filter, container, false)

            // initialize controls
            initControls ()

            // set filter data
            setFilterData ()
        }

        return rootView
    }

    /**
     *  Event Handlers
     */
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

    private fun onClickOwner () {
        fragmentManager?.let { fm ->
            val fragment = HLManagerHorseUserSpinnerDialogFragment (
                HLHorseUserSearchType.OWNER,
                selectedUser = selectedOwner,
                listener = object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        selectedOwner = data as HLHorseManagerModel
                        ownerTextView?.text = if (position == 0) "" else selectedOwner?.name

                    }
                })
            fragment.show(fm, "Select Owner Fragment")
        }
    }

    private fun onClickTrainer () {
        fragmentManager?.let { fm ->
            val fragment = HLManagerHorseUserSpinnerDialogFragment (
                HLHorseUserSearchType.TRAINER,
                selectedUser = selectedTrainer,
                listener = object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        selectedTrainer = data as   
                        trainerTextView?.text = if (position == 0) "" else selectedTrainer?.name
                    }
                })
            fragment.show(fm, "Select Trainer Fragment")
        }
    }

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

            // owner
            owner = selectedOwner

            // trainer
            trainer = selectedTrainer
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
        selectedOwner = null
        selectedTrainer = null

        selectTextView?.text = ""
        ownerTextView?.text = ""
        trainerTextView?.text = ""
        scheduledSwitch?.isChecked = false

        filter = null

        EventBus.getDefault().post(HLRefreshHorsesEvent(filter))
    }


    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        backButton = rootView?.findViewById(R.id.backImageView)
        selectTextView = rootView?.findViewById(R.id.selectTextView)
        ownerTextView = rootView?.findViewById(R.id.ownerTextView)
        trainerTextView = rootView?.findViewById(R.id.trainerTextView)
        scheduledSwitch = rootView?.findViewById(R.id.scheduledSwitch)
        applyButton = rootView?.findViewById(R.id.applyButton)
        clearButton = rootView?.findViewById(R.id.clearButton)


        // event handlers
        backButton?.setOnClickListener { popFragment() }
        selectTextView?.setOnClickListener { onClickSelect () }
        ownerTextView?.setOnClickListener { onClickOwner () }
        trainerTextView?.setOnClickListener { onClickTrainer () }
        applyButton?.setOnClickListener { onClickApply () }
        clearButton?.setOnClickListener { onClickClear () }
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
            ownerTextView?.text = filterModel.owner?.name
            selectedOwner = filterModel.owner

            // trainers
            trainerTextView?.text = filterModel.trainer?.name
            selectedTrainer = filterModel.trainer
        }
    }
}
