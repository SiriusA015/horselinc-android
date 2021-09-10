package com.horselinc.views.fragments.provider

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLInvoiceModel
import com.horselinc.models.data.HLInvoiceShareInfo
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.models.event.HLEventPaymentInvoiceCreated
import com.horselinc.views.adapters.recyclerview.HLHorseOwnerNameAdapter
import com.horselinc.views.adapters.recyclerview.HLInvoiceServiceViewAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.common.HLWebViewFragment
import com.horselinc.views.fragments.manager.HLManagerEditPaymentOptionsFragment
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 *  Created by TengFei Li on 27, August, 2019
 */

class HLProviderConfirmInvoiceFragment(private val serviceRequest: HLServiceRequestModel, val popFragment: Class<*>?,
                                       private val name: String?, private val email: String?, private val phone: String?) : HLBaseFragment() {

    private var btBack: ImageView? = null
    private var lytOwners: View? = null
    private var lytLeased: View? = null
    private var lytShow: View? = null
    private var lytCompetition: View? = null
    private var lytNote: View? = null
    private var tvHorse: TextView? = null
    private var tvTrainer: TextView? = null
    private var lstOwners: RecyclerView? = null
    private var tvLeased: TextView? = null
    private var tvDate: TextView? = null
    private var tvShow: TextView? = null
    private var lstServices: RecyclerView? = null
    private var tvCompetition: TextView? = null
    private var tvSubTotalTitle: TextView? = null
    private var tvSubTotal: TextView? = null
    private var tvNote: TextView? = null
    private var tvTotal: TextView? = null
    private var btConfirm: Button? = null
    private var btSave: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_provider_confirm_invoice, container, false)

            initControls()
        }

        return rootView
    }

    @SuppressLint("SetTextI18n")
    private fun initControls() {
        // controls
        btBack = rootView?.findViewById(R.id.btBack)
        btBack?.setOnClickListener {
            popFragment()
        }
        lytOwners = rootView?.findViewById(R.id.lytOwners)
        lytLeased = rootView?.findViewById(R.id.lytLeased)
        lytShow = rootView?.findViewById(R.id.lytShow)
        lytCompetition = rootView?.findViewById(R.id.lytCompetition)
        lytNote = rootView?.findViewById(R.id.lytNote)
        tvHorse = rootView?.findViewById(R.id.tvHorse)
        tvTrainer = rootView?.findViewById(R.id.tvTrainer)
        lstOwners = rootView?.findViewById(R.id.lstOwners)
        tvLeased = rootView?.findViewById(R.id.tvLeased)
        tvDate = rootView?.findViewById(R.id.tvDate)
        tvShow = rootView?.findViewById(R.id.tvShow)
        lstServices = rootView?.findViewById(R.id.lstServices)
        tvCompetition = rootView?.findViewById(R.id.tvCompetition)
        tvSubTotalTitle = rootView?.findViewById(R.id.tvSubTotalTitle)
        tvSubTotal = rootView?.findViewById(R.id.tvSubTotal)
        tvNote = rootView?.findViewById(R.id.tvNote)
        tvTotal = rootView?.findViewById(R.id.tvTotal)
        btConfirm = rootView?.findViewById(R.id.btConfirm)
        btSave = rootView?.findViewById(R.id.btSave)

        // event handlers
        btSave?.setOnClickListener { onSaveToDrafts() }
        btConfirm?.setOnClickListener { onConfirmAndSubmit() }


        var name = serviceRequest.horse?.barnName ?: ""
        if (!serviceRequest.horse?.displayName.isNullOrEmpty()) {
            name += " \"${serviceRequest.horse?.displayName}\""
        }
        tvHorse?.text = name
        tvTrainer?.text = serviceRequest.horse?.trainer?.name

        tvDate?.text = serviceRequest.requestDate.date.formattedString("EEEE, MMMM d, YYYY")

        lytOwners?.visibility = if (0 < serviceRequest.horse?.owners?.size ?: 0) {
            lstOwners?.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            lstOwners?.adapter = HLHorseOwnerNameAdapter(activity, serviceRequest.horse!!.owners!!)
            VISIBLE
        } else GONE

        lytLeased?.visibility = serviceRequest.horse?.leaser?.let {
            tvLeased?.text = serviceRequest.horse?.leaser?.name
            VISIBLE
        } ?:GONE

        lytShow?.visibility = if (!serviceRequest.show?.name.isNullOrEmpty()) {
            tvShow?.text = serviceRequest.show?.name
            VISIBLE
        } else GONE

        lytCompetition?.visibility = if (!serviceRequest.competitionClass.isNullOrEmpty()) {
            tvCompetition?.text = serviceRequest.competitionClass
            VISIBLE
        } else GONE

        lytNote?.visibility = if (!serviceRequest.instruction.isNullOrEmpty()) {
            tvNote?.text = serviceRequest.instruction
            VISIBLE
        } else GONE

        lstServices?.run {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            adapter = HLInvoiceServiceViewAdapter(activity, serviceRequest.services)
        }

        var totalAmount = 0f
        serviceRequest.services.forEach {
            totalAmount += it.quantity.toFloat() * it.rate
        }

        tvSubTotalTitle?.text = if (serviceRequest.services.size == 1) "1 service" else "${serviceRequest.services.size} services"
        tvSubTotal?.text = "$ ${"%.2f".format(totalAmount)}"
        tvTotal?.text = "$ ${"%.2f".format(totalAmount)}"
    }

    private fun checkPaymentMethodValid() : Boolean {
        if (HLGlobalData.me?.type.isNullOrEmpty()) return false

        if (HLGlobalData.me?.type == HLUserType.MANAGER) {
            return if (!HLGlobalData.me?.horseManager?.customer?.id.isNullOrEmpty()) {
                true
            } else {
                replaceFragment(HLManagerEditPaymentOptionsFragment ())
                false
            }
        } else {
            if (!HLGlobalData.me?.serviceProvider?.account?.id.isNullOrEmpty()) {
                return true
            } else {
                HLGlobalData.me?.serviceProvider?.account?.id?.let {
                    showProgressDialog()
                    HLFirebaseService.instance.getExpressLoginUrl(it, object : ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()
                            replaceFragment(
                                HLWebViewFragment(
                                    data
                                )
                            )
                        }

                        override fun onFailure(error: String) {
                            hideProgressDialog()
                            showErrorMessage(error)
                        }
                    })
                }

                return false
            }
        }
    }

    private fun onSaveToDrafts() {
        serviceRequest.shareInfo = HLInvoiceShareInfo(name, phone, email)

        showProgressButton(btSave, ContextCompat.getColor(activity!!, R.color.colorPrimary))
        HLFirebaseService.instance.putServiceRequest(serviceRequest, object : ResponseCallback<HLServiceRequestModel> {
            override fun onSuccess(data: HLServiceRequestModel) {
                hideProgressButton(btSave, stringResId = R.string.save_to_drafts)
                showSuccessMessage("Successfully submitted!")

                EventBus.getDefault().post(HLEventPaymentInvoiceCreated(HLInvoiceModel()))

                if (popFragment == HLProviderHorseDetailFragment::class.java) {
                    popFragment(popFragment)
                } else {
                    popToMain()
                }
            }

            override fun onFailure(error: String) {
                hideProgressButton(btSave, stringResId = R.string.save_to_drafts)
                showError(error)
            }
        })
    }

    private fun onConfirmAndSubmit() {
        // Check user has the payment methiod or not.
        if (!checkPaymentMethodValid()) {
            showInfoMessage("You have to add a payment method before create an invoice")
            return
        }

        val invoice = HLInvoiceModel().apply {
            requests = arrayListOf(serviceRequest)
            amount = 0.0
            tip = 0.0
            createdAt = Date().time
            updatedAt = Date().time
            name = serviceRequest.horse?.barnName.orEmpty()
            status = HLInvoiceStatusType.SUBMITTED
            shareInfo = HLInvoiceShareInfo(name, phone, email)
        }

        showProgressButton(btConfirm)
        HLFirebaseService.instance.putInvoice(invoice, false, object : ResponseCallback<HLInvoiceModel> {
            override fun onSuccess(data: HLInvoiceModel) {
                if ((serviceRequest.horse?.creatorId ?: "").isEmpty()) {
                    shareInviteLink (data)
                } else {
                    hideProgressButton(btConfirm, stringResId = R.string.confirm_and_submit_invoice)
                    showSuccessMessage("Successfully submitted!")
                    popFragmentHandler ()
                }
            }

            override fun onFailure(error: String) {
                hideProgressButton(btConfirm, stringResId = R.string.confirm_and_submit_invoice)
                showError(error)
            }
        })
    }

    private fun shareInviteLink (invoice: HLInvoiceModel) {
        HLGlobalData.me?.let { user ->
            activity?.let { act ->
                /*HLBranchService.createInviteLink(act, user.uid, HLUserType.PROVIDER, invoice.uid, serviceRequest.horseId,
                    object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressDialog()

                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Signup for HorseLinc $data to manager all of your equine needs.")
                                type = "text/plain"
                            }

                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)

                            popFragmentHandler()
                        }

                        override fun onFailure(error: String) {
                            showError(error)
                        }
                })*/

                HLFirebaseService.instance.shareInvoice(user.uid, serviceRequest.horseId, invoice.uid, phone, email,
                    object: ResponseCallback<String> {
                        override fun onSuccess(data: String) {
                            hideProgressButton(btConfirm, stringResId = R.string.confirm_and_submit_invoice)
                            popFragmentHandler()
                        }

                        override fun onFailure(error: String) {
                            hideProgressButton(btConfirm, stringResId = R.string.confirm_and_submit_invoice)
                            showError(error)
                        }
                    })
            }
        }
    }

    private fun popFragmentHandler () {
        if (popFragment == HLProviderHorseDetailFragment::class.java) {
            popFragment(popFragment)
        } else {
            popToMain()
        }
    }

}