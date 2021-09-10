package com.horselinc.views.fragments.role


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerPaymentApproverModel
import com.horselinc.models.data.HLStripeCardModel
import com.horselinc.models.data.HLStripeCustomerModel
import com.horselinc.views.activities.HLHorseManagerMainActivity
import com.horselinc.views.adapters.recyclerview.HLPaymentApproverAdapter
import com.horselinc.views.adapters.recyclerview.HLPaymentCardAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLAddPaymentCardListener
import com.horselinc.views.listeners.HLPaymentApproverItemListener
import com.horselinc.views.listeners.HLPaymentApproverListener
import com.horselinc.views.listeners.HLPaymentMethodItemListener
import com.jude.easyrecyclerview.EasyRecyclerView
import java.util.*
import kotlin.collections.ArrayList

class HLAddPaymentFragment : HLBaseFragment() {

    private var doneButton: Button? = null
    private var lastNumberTextView: TextView? = null
    private var paymentCardRecyclerView: EasyRecyclerView? = null
    private var addCardButton: Button? = null

    private var approverRecyclerView: EasyRecyclerView? = null
    private var addApproverButton: Button? = null

    private var cardAdapter: HLPaymentCardAdapter? = null
    private var approverAdapter: HLPaymentApproverAdapter? = null

    private var customer: HLStripeCustomerModel? = HLGlobalData.me?.horseManager?.customer?.copy()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_add_payment_horse_manager, container, false)

            // initialize controls
            initControls ()
        }
        return rootView
    }

    /**
     *  Event Handlers
     */
    private fun onClickDone () {
        // save user
        App.preference.put(PreferenceKey.USER, Gson().toJson(HLGlobalData.me))

        // invite deep link handler
        HLGlobalData.deepLinkInvite?.let { deepLinkModel ->

            showProgressDialog()

            when {
                !deepLinkModel.horseId.isNullOrEmpty() -> updateHorse(deepLinkModel.horseId!!)
                else -> finishActivity()
            }

        } ?: finishActivity()
    }

    private fun onClickAddCard () {
        if (customer == null) {
            HLGlobalData.me?.let { user ->
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

    private fun onClickAddApprover () {
        val fragment =
            HLAddPaymentApproverFragment(approvers = approverAdapter?.allData,
                paymentApproverListener = object : HLPaymentApproverListener {
                    override fun onAdd(newApprover: HLHorseManagerPaymentApproverModel) {
                        approverAdapter?.add(newApprover)
                    }
            })
        replaceFragment(fragment)
    }

    private fun showAddCard () {
        activity?.let {
            val dialogFragment =
                HLAddPaymentCardDialogFragment(addPaymentCardListener = object : HLAddPaymentCardListener {
                    override fun onAddToCustomer(newCustomer: HLStripeCustomerModel) {
                        setCustomerData(newCustomer)
                    }
                })
            dialogFragment.show(it.supportFragmentManager, "Add Payment Card Fragment")
        }
    }

    private fun finishActivity () {
        hideProgressDialog()

        // finish
        val intent = Intent(activity, HLHorseManagerMainActivity::class.java)
        activity?.startActivity(intent)
        activity?.finish()
    }


    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        doneButton = rootView?.findViewById(R.id.doneButton)
        lastNumberTextView = rootView?.findViewById(R.id.lastNumberTextView)
        paymentCardRecyclerView = rootView?.findViewById(R.id.paymentCardRecyclerView)
        addCardButton = rootView?.findViewById(R.id.addCardButton)

        paymentCardRecyclerView?.recyclerView?.setHasFixedSize(false)
        paymentCardRecyclerView?.setLayoutManager(LinearLayoutManager(activity))


        approverRecyclerView = rootView?.findViewById(R.id.approverRecyclerView)
        addApproverButton = rootView?.findViewById(R.id.addApproverButton)

        setCardLastNumber (customer?.cards)

        // payment methods
        cardAdapter = HLPaymentCardAdapter(activity,
            customer?.cards?.indexOfFirst { card -> customer?.defaultSource == card.id } ?: 0,
            object: HLPaymentMethodItemListener {
                override fun onClickRadio(position: Int) {
                    updateDefaultCard(position)
                }

                override fun onClickDelete(position: Int) {
                    deleteCard (position)
                }
            })
        cardAdapter?.addAll(customer?.cards)
        paymentCardRecyclerView?.adapter = cardAdapter

        // payment approvers
        approverAdapter = HLPaymentApproverAdapter(activity, object: HLPaymentApproverItemListener {
            override fun onClickEdit(position: Int) {
                val fragment = HLAddPaymentApproverFragment(approverIndex = position,
                    approvers = approverAdapter?.allData,
                    paymentApproverListener = object : HLPaymentApproverListener {
                        override fun onUpdate(updateApprover: HLHorseManagerPaymentApproverModel, updatePosition: Int) {
                            approverAdapter?.update(updateApprover, updatePosition)
                        }
                    })
                replaceFragment(fragment)
            }

            override fun onClickDelete(position: Int) {
                approverAdapter?.getItem(position)?.let { approver ->
                    showProgressDialog()
                    HLFirebaseService.instance.deleteApprover(approver.uid, object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()
                            approverAdapter?.remove(position)
                        }

                        override fun onFailure(error: String) {
                            showError(message = error)
                        }
                    })
                }
            }
        })
        approverRecyclerView?.adapter = approverAdapter
        approverRecyclerView?.recyclerView?.setHasFixedSize(false)
        approverRecyclerView?.setLayoutManager(LinearLayoutManager(activity))

        // get approvers
        getApprovers ()

        // event handler
        doneButton?.setOnClickListener { onClickDone () }
        addCardButton?.setOnClickListener { onClickAddCard () }
        addApproverButton?.setOnClickListener { onClickAddApprover () }
    }

    /**
     *  Horse Manager Handlers
     */
    private fun getApprovers () {
        showProgressDialog()
        HLGlobalData.me?.let { user ->
            HLFirebaseService.instance.getPaymentApprovers(user.uid, object: ResponseCallback<List<HLHorseManagerPaymentApproverModel>> {
                override fun onSuccess(data: List<HLHorseManagerPaymentApproverModel>) {
                    hideProgressDialog()
                    approverAdapter?.clear()
                    approverAdapter?.addAll(data)
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                }
            })
        }
    }

    private fun setCardLastNumber (cards: ArrayList<HLStripeCardModel>?) {
        val cardCount = cards?.size ?: 0
        if (cardCount > 0) {
            cards?.first { card -> customer?.defaultSource == card.id }?.let {
                lastNumberTextView?.text = it.last4
            }
        }
    }

    private fun setCustomerData (newData: HLStripeCustomerModel) {
        customer = newData

        // save user data
        HLGlobalData.me?.horseManager?.customer = newData
        App.preference.put(PreferenceKey.USER, Gson().toJson(HLGlobalData.me))

        // set last number
        setCardLastNumber(newData.cards)

        // payment adapter
        cardAdapter?.run {
            selectedIndex = newData.cards?.indexOfFirst { card -> newData.defaultSource == card.id } ?: 0

            clear()
            addAll(newData.cards)
        }
    }

    private fun updateDefaultCard (position: Int) {
        HLGlobalData.me?.let { user ->
            customer?.let { customer ->
                cardAdapter?.getItem(position)?.let { card ->
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
        HLGlobalData.me?.let { user ->
            customer?.let { customer ->
                cardAdapter?.getItem(position)?.let { card ->
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

    /**
     *  DeepLink Handler
     */
    private fun updateHorse (horseId: String) {
        HLGlobalData.me?.let { user ->
            val updateData = hashMapOf<String, Any>(
                "trainerId" to user.uid,
                "creatorId" to user.uid
            )

            HLFirebaseService.instance.updateHorse(horseId, updateData, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    finishActivity()
                }

                override fun onFailure(error: String) {
                    showError(error)
                    finishActivity()
                }
            })
        } ?: finishActivity()
    }
}
