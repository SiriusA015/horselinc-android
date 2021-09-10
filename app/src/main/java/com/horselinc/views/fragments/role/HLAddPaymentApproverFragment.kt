package com.horselinc.views.fragments.role


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLHorseManagerPaymentApproverModel
import com.horselinc.models.data.HLUserModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.activities.HLUserRoleActivity
import com.horselinc.views.adapters.recyclerview.HLBaseUserAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLPaymentApproverListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import me.abhinay.input.CurrencyEditText
import me.abhinay.input.CurrencySymbols
import java.util.concurrent.TimeUnit


class HLAddPaymentApproverFragment(private val approverUser: HLUserModel? = HLGlobalData.me,
                                   private val approverIndex: Int? = null,
                                   private val approvers: List<HLHorseManagerPaymentApproverModel>?,
                                   private val paymentApproverListener: HLPaymentApproverListener) : HLBaseFragment() {

    private var backButton: ImageView? = null
    private var nameEditText: EditText? = null
    private var nameTextView: TextView? = null
    private var unlimitedSwitch: Switch? = null
    private var amountEditText: CurrencyEditText? = null
    private var addButton: Button? = null
    private var searchRecyclerView: EasyRecyclerView? = null

    private lateinit var userAdapter: HLBaseUserAdapter
    private var selectedManager: HLHorseManagerModel? = null

    private var searchEnabled = true
    private var isUserLoadMore = true

    private val disposable = CompositeDisposable ()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            val resId = if (activity is HLUserRoleActivity) {
                R.layout.fragment_auth_add_payment_approver
            } else {
                R.layout.fragment_edit_add_payment_approver
            }
            rootView = inflater.inflate(resId, container, false)

            // initialize controls
            initControls ()

            // set reactive x
            setReactiveX ()
        }
        return rootView
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    /**
     *  Event Handlers
     */
    private fun onClickBack () {
        if (activity is HLUserRoleActivity) {
            popFragment()
        } else {
            popFragment()
        }
    }

    private fun onClickAdd () {
        if (approverIndex == null) {
            val amount = when {
                unlimitedSwitch?.isChecked == true -> null
                amountEditText?.text?.trim()?.isEmpty() == true -> 0.0f
                else ->amountEditText?.cleanDoubleValue?.toFloat()
            }

            when {
                selectedManager == null -> showInfoMessage(ResourceUtil.getString(R.string.msg_err_please_select_approver))
                unlimitedSwitch?.isChecked == false && amount == 0.0f ->
                    showInfoMessage(ResourceUtil.getString(R.string.msg_err_please_enter_amount))
                else -> {
                    val approver = HLHorseManagerPaymentApproverModel().apply {
                        approverUser?.let { user ->
                            creatorId = user.uid
                        }

                        selectedManager?.let { manager ->
                            userId = manager.userId
                            name = manager.name
                            location = manager.location
                            avatarUrl = manager.avatarUrl
                            phone = manager.phone
                        }
                        this.amount = amount
                    }

                    showProgressDialog()
                    HLFirebaseService.instance.addApprover(approver, object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()
                            approver.uid = data
                            paymentApproverListener.onAdd(approver)
                            onClickBack()
                        }

                        override fun onFailure(error: String) {
                            showError(message = error)
                        }
                    })
                }
            }
        } else {
            val amount = when {
                unlimitedSwitch?.isChecked == true -> null
                amountEditText?.text?.trim()?.isEmpty() == true -> 0.0f
                else ->amountEditText?.cleanDoubleValue?.toFloat()
            }

            when {
                unlimitedSwitch?.isChecked == false && amount == 0.0f ->
                    showInfoMessage(ResourceUtil.getString(R.string.msg_err_please_enter_amount))
                else -> {
                    approvers?.get(approverIndex)?.let { selectedApprover ->

                        selectedApprover.amount = amount

                        showProgressDialog()
                        HLFirebaseService.instance.updateApprover(selectedApprover, object: ResponseCallback<String> {
                            override fun onSuccess(data: String) {
                                hideProgressDialog()
                                paymentApproverListener.onUpdate(selectedApprover, approverIndex)
                                onClickBack()
                            }

                            override fun onFailure(error: String) {
                                showError(message = error)
                            }
                        })
                    }
                }
            }
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        backButton = rootView?.findViewById(R.id.backImageView)
        nameEditText = rootView?.findViewById(R.id.nameEditText)
        nameTextView = rootView?.findViewById(R.id.nameTextView)
        unlimitedSwitch = rootView?.findViewById(R.id.unlimitedSwitch)
        amountEditText = rootView?.findViewById(R.id.amountEditText)
        amountEditText?.setCurrency(CurrencySymbols.USA)
        searchRecyclerView = rootView?.findViewById(R.id.searchRecyclerView)
        addButton = rootView?.findViewById(R.id.addButton)

        userAdapter = HLBaseUserAdapter(activity).apply {
            setOnItemClickListener {
                selectedManager = getItem(it) as HLHorseManagerModel
                clear()

                searchEnabled = false
                nameEditText?.setText(selectedManager?.name)

                hideKeyboard()
                nameEditText?.clearFocus()
            }

            setMore(R.layout.load_more_user, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (isUserLoadMore) {
                        searchHorseManagers(nameEditText?.text?.trim().toString())
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() { }
            })
        }
        searchRecyclerView?.adapter = userAdapter
        searchRecyclerView?.recyclerView?.setHasFixedSize(false)
        searchRecyclerView?.setLayoutManager(LinearLayoutManager(activity))


        // event handler
        backButton?.setOnClickListener { onClickBack() }
        addButton?.setOnClickListener { onClickAdd () }
        nameEditText?.setOnClickListener { searchEnabled = true }
        unlimitedSwitch?.setOnCheckedChangeListener { _, isChecked ->
            amountEditText?.isEnabled = !isChecked
        }

        // add or edit
        if (approverIndex == null) {
            nameEditText?.visibility = View.VISIBLE
            nameTextView?.visibility = View.INVISIBLE
            addButton?.setText(R.string.add)
        } else {
            nameEditText?.visibility = View.INVISIBLE
            nameTextView?.visibility = View.VISIBLE
            addButton?.setText(R.string.save)

            nameTextView?.text = approvers?.get(approverIndex)?.name
            if (approvers?.get(approverIndex)?.amount == null) {
                unlimitedSwitch?.isChecked = true
            } else {
                unlimitedSwitch?.isChecked = false
                amountEditText?.setText(String.format("%.02f", approvers[approverIndex].amount))
            }
        }
    }

    private fun setReactiveX () {
        nameEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { name ->
                        val searchKey = name.trim().toString()
                        if (searchKey.length >= 3 && searchEnabled) {
                            searchHorseManagers (searchKey)
                        }
                    }
            )
        }
    }

    /**
     *  Others
     */
    private fun searchHorseManagers (searchName: String) {

        val lastUserId = if (userAdapter.allData?.isEmpty() == true) "" else {
            userAdapter.allData?.last()?.userId ?: ""
        }

        HLFirebaseService.instance.searchHorseManagers(searchName, lastUserId, null,
            object : ResponseCallback<List<HLHorseManagerModel>> {
                override fun onSuccess(data: List<HLHorseManagerModel>) {
                    val filteredData = data.filter { manager ->
                        manager.userId != approverUser?.uid && approvers?.none { approver -> approver.userId == manager.userId } == true
                    }
                    userAdapter.addAll(filteredData)

                    isUserLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSE_MANAGERS
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
    }
}
