package com.horselinc.views.fragments.manager


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerProviderModel
import com.horselinc.models.data.HLServiceProviderModel
import com.horselinc.models.event.HLSelectBaseUserEvent
import com.horselinc.views.activities.HLSearchUserActivity
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.listeners.HLSpinnerDialogListener
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HLManagerAddServiceProviderFragment(var provider: HLServiceProviderModel? = null) : HLBaseFragment() {

    private var providerImageView: RoundedImageView? = null
    private var providerNameTextView: TextView? = null
    private var selectServiceTextView: TextView? = null
    private var otherServiceCardView: CardView? = null
    private var otherServiceEditText: EditText? = null
    private var saveButton: Button? = null

    private val serviceTypes = arrayListOf("None", "Braider", "Clipping", "Farrier", "Therapy", "Vet", "Other")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_add_service_provider, container, false)

            initControls ()

            EventBus.getDefault().register(this)
        }

        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    /**
     *  Event Bus Handlers
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedSelectUserEvent (event: HLSelectBaseUserEvent) {
        try {
            if (event.selectType == HLBaseUserSelectType.SERVICE_PROVIDER) {
                provider = event.baseUser as HLServiceProviderModel
                providerImageView?.loadImage(event.baseUser.avatarUrl, R.drawable.ic_profile)
                providerNameTextView?.text = event.baseUser.name
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
        providerImageView = rootView?.findViewById(R.id.providerImageView)
        providerNameTextView = rootView?.findViewById(R.id.providerNameTextView)
        selectServiceTextView = rootView?.findViewById(R.id.selectServiceTextView)
        otherServiceCardView = rootView?.findViewById(R.id.otherServiceCardView)
        otherServiceEditText = rootView?.findViewById(R.id.otherServiceEditText)
        saveButton = rootView?.findViewById(R.id.saveButton)

        otherServiceCardView?.visibility = View.INVISIBLE

        provider?.let {
            providerImageView?.loadImage(it.avatarUrl, R.drawable.ic_profile)
            providerNameTextView?.text = it.name
        }

        // bind progress buttons
        setProgressButton(saveButton)

        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<CardView>(R.id.providerCardView)?.setOnClickListener { onClickProvider () }
        rootView?.findViewById<CardView>(R.id.serviceCardView)?.setOnClickListener { onClickService () }
        saveButton?.setOnClickListener { onClickSave () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickProvider () {
        hideKeyboard()

        // build exclude users
        val excludeUsers = ArrayList<String>()

        /*HLGlobalData.me?.let {
            excludeUsers.add(it.uid)
        }*/

        provider?.let {
            excludeUsers.add(it.userId)
        }

        val intent = Intent (activity, HLSearchUserActivity::class.java).apply {
            putExtra(IntentExtraKey.BASE_USER_INVITE_SERVICE_PROVIDER, true)
            putExtra(IntentExtraKey.BASE_USER_SELECT_TYPE, HLBaseUserSelectType.SERVICE_PROVIDER)
            putStringArrayListExtra(IntentExtraKey.BASE_USER_EXCLUDE_IDS, excludeUsers)
        }
        startActivity(intent)
    }

    private fun onClickService () {
        fragmentManager?.let { fm ->
            val selectedPosition = serviceTypes.indexOf(selectServiceTextView?.text.toString())
            val fragment = HLSpinnerDialogFragment (
                "Select Service",
                serviceTypes,
                if (selectedPosition == -1) 0 else selectedPosition,
                object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        selectServiceTextView?.text = data as String

                        otherServiceCardView?.visibility = if (position == serviceTypes.size - 1) {
                            View.VISIBLE
                        } else {
                            View.INVISIBLE
                        }
                    }
                })
            fragment.show(fm, "Select Service Type Fragment")
        }
    }

    private fun onClickSave () {
        hideKeyboard()

        HLGlobalData.me?.uid?.let { userId ->

            provider ?: showErrorMessage("Please select service provider")

            val index = serviceTypes.indexOf(selectServiceTextView?.text.toString())
            if (index == -1) {
                showErrorMessage("Please select service type")
                return
            }

            if (index == serviceTypes.size - 1 && otherServiceEditText?.text?.trim().toString().isEmpty()) {
                showErrorMessage("Please enter custom label")
                return
            }

            provider?.let {  serviceProvider ->
                val managerProvider = HLHorseManagerProviderModel().copy(serviceProvider).apply {
                    creatorId = userId
                    serviceType = if (index == serviceTypes.size - 1) {
                        otherServiceEditText?.text?.trim().toString()
                    } else {
                        serviceTypes[index]
                    }
                }

                showProgressButton(saveButton)
                HLFirebaseService.instance.addHorseManagerProvider(managerProvider, object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        hideProgressButton(saveButton, stringResId = R.string.save)
                        popFragment()
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(saveButton, stringResId = R.string.save)
                        showError(error)
                    }
                })
            }
        }
    }
}
