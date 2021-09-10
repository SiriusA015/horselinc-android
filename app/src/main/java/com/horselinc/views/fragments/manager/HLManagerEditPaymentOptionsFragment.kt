package com.horselinc.views.fragments.manager


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.horselinc.App
import com.horselinc.HLGlobalData
import com.horselinc.PreferenceKey
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerPaymentApproverModel
import com.horselinc.models.data.HLStripeCustomerModel
import com.horselinc.models.data.HLUserModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLPaymentApproverAdapter
import com.horselinc.views.adapters.recyclerview.HLPaymentCardAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.role.HLAddPaymentApproverFragment
import com.horselinc.views.fragments.role.HLAddPaymentCardDialogFragment
import com.horselinc.views.listeners.HLAddPaymentCardListener
import com.horselinc.views.listeners.HLPaymentApproverItemListener
import com.horselinc.views.listeners.HLPaymentApproverListener
import com.horselinc.views.listeners.HLPaymentMethodItemListener
import com.jude.easyrecyclerview.decoration.DividerDecoration

class HLManagerEditPaymentOptionsFragment (private val paymentUser: HLUserModel? = HLGlobalData.me) : HLBaseFragment() {

    private var backButton: ImageView? = null
    private var doneButton: Button? = null
    private var cardRecyclerView: RecyclerView? = null
    private var approverRecyclerView: RecyclerView? = null

    private lateinit var cardAdapter: HLPaymentCardAdapter
    private lateinit var approverAdapter: HLPaymentApproverAdapter

    private var customer: HLStripeCustomerModel? = paymentUser?.horseManager?.customer?.copy()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_edit_payment_options, container, false)

            initControls ()

            // get approvers
            getApprovers ()
        }

        return rootView
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        backButton = rootView?.findViewById(R.id.backImageView)
        doneButton = rootView?.findViewById(R.id.doneButton)
        cardRecyclerView = rootView?.findViewById(R.id.cardRecyclerView)
        approverRecyclerView = rootView?.findViewById(R.id.approverRecyclerView)

        // card recycler view
        val selectedIndex = customer?.cards?.indexOfFirst { card -> customer?.defaultSource == card.id } ?: 0
        cardAdapter = HLPaymentCardAdapter(activity, selectedIndex, object: HLPaymentMethodItemListener {
            override fun onClickRadio(position: Int) {
                updateDefaultCard(position)
            }

            override fun onClickDelete(position: Int) {
                deleteCard (position)
            }
        }).apply {
            addAll(customer?.cards)
        }
        cardRecyclerView?.run {
            adapter = cardAdapter
            layoutManager = LinearLayoutManager(activity)
            val itemDecoration = DividerDecoration(
                Color.parseColor("#1e000000"),
                ResourceUtil.dpToPx(1),
                ResourceUtil.dpToPx(16),
                0
            )
            itemDecoration.setDrawLastItem(false)
            itemDecoration.setDrawHeaderFooter(false)
            addItemDecoration(itemDecoration)
        }

        // approver recycler view
        approverAdapter = HLPaymentApproverAdapter(activity, object: HLPaymentApproverItemListener {
            override fun onClickEdit(position: Int) {
                val fragment = HLAddPaymentApproverFragment(paymentUser, position, approverAdapter.allData, object : HLPaymentApproverListener {
                    override fun onUpdate(updateApprover: HLHorseManagerPaymentApproverModel, updatePosition: Int) {
                        approverAdapter.update(updateApprover, updatePosition)
                    }
                })
                replaceFragment(fragment)
            }

            override fun onClickDelete(position: Int) {
                approverAdapter.getItem(position)?.let { approver ->
                    showProgressDialog()
                    HLFirebaseService.instance.deleteApprover(approver.uid, object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()
                            approverAdapter.remove(position)
                        }

                        override fun onFailure(error: String) {
                            showError(error)
                        }
                    })
                }
            }
        })

        approverRecyclerView?.run {
            adapter = approverAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        // back and done button
        if (paymentUser?.uid == HLGlobalData.me?.uid) {
            backButton?.visibility = View.VISIBLE
            doneButton?.visibility = View.GONE
        } else {
            backButton?.visibility = View.GONE
            doneButton?.visibility = View.VISIBLE
        }


        // event handlers
        backButton?.setOnClickListener { popFragment() }
        doneButton?.setOnClickListener { popFragment() }
        rootView?.findViewById<CardView>(R.id.addCardView)?.setOnClickListener { onClickAddCard () }
        rootView?.findViewById<CardView>(R.id.addApproverCardView)?.setOnClickListener { onClickAddApprover () }

    }

    /**
     *  Event Handlers
     */
    private fun onClickAddCard () {
        if (customer == null) {
            paymentUser?.let { user ->
                showProgressDialog()
                HLFirebaseService.instance.createCustomer(user.uid, object: ResponseCallback<HLStripeCustomerModel> {
                    override fun onSuccess(data: HLStripeCustomerModel) {
                        hideProgressDialog()

                        setCustomerData(data)

                        // show add card
                        showAddCard ()
                    }

                    override fun onFailure(error: String) {
                        showError(message = error)
                    }
                })
            }
        } else {
            showAddCard ()
        }
    }

    private fun showAddCard () {
        activity?.let {
            val dialogFragment =
                HLAddPaymentCardDialogFragment(paymentUser, object : HLAddPaymentCardListener {
                    override fun onAddToCustomer(newCustomer: HLStripeCustomerModel) {
                        setCustomerData(newCustomer)
                    }
                })
            dialogFragment.show(it.supportFragmentManager, "Add Payment Card Fragment")
        }
    }

    private fun onClickAddApprover () {
        val fragment =
            HLAddPaymentApproverFragment(paymentUser,
                approvers = approverAdapter.allData,
                paymentApproverListener = object : HLPaymentApproverListener {
                    override fun onAdd(newApprover: HLHorseManagerPaymentApproverModel) {
                        approverAdapter.add(newApprover)
                    }
                })
        replaceFragment(fragment)
    }


    /**
     *  Payment Handlers
     */
    private fun getApprovers () {
        showProgressDialog()
        paymentUser?.let { user ->
            HLFirebaseService.instance.getPaymentApprovers(user.uid, object: ResponseCallback<List<HLHorseManagerPaymentApproverModel>> {
                override fun onSuccess(data: List<HLHorseManagerPaymentApproverModel>) {
                    hideProgressDialog()
                    approverAdapter.clear()
                    approverAdapter.addAll(data)
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }

    private fun setCustomerData (newData: HLStripeCustomerModel) {
        customer = newData

        // save user data
        if (paymentUser?.uid == HLGlobalData.me?.uid) {
            HLGlobalData.me?.horseManager?.customer = newData
            App.preference.put(PreferenceKey.USER, Gson().toJson(HLGlobalData.me))
        } else {
            paymentUser?.horseManager?.customer = newData
        }

        // payment adapter
        cardAdapter.run {
            selectedIndex = newData.cards?.indexOfFirst { card -> newData.defaultSource == card.id } ?: 0

            clear()
            addAll(newData.cards)
        }
    }

    private fun updateDefaultCard (position: Int) {
        paymentUser?.let { user ->
            customer?.let { customer ->
                cardAdapter.getItem(position)?.let { card ->
                    showProgressDialog()
                    HLFirebaseService.instance.changeDefaultCard(user.uid, customer.id, card.id, object:
                        ResponseCallback<HLStripeCustomerModel> {
                        override fun onSuccess(data: HLStripeCustomerModel) {
                            hideProgressDialog()
                            setCustomerData (data)
                        }

                        override fun onFailure(error: String) {
                            showError(message = error)
                        }
                    })
                }
            }
        }
    }

    private fun deleteCard (position: Int) {
        paymentUser?.let { user ->
            customer?.let { customer ->
                cardAdapter.getItem(position)?.let { card ->
                    showProgressDialog()
                    HLFirebaseService.instance.deleteCard(user.uid, customer.id, card.id,
                        object: ResponseCallback<HLStripeCustomerModel> {
                            override fun onSuccess(data: HLStripeCustomerModel) {
                                hideProgressDialog()
                                setCustomerData (data)
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
