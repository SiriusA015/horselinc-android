package com.horselinc.views.fragments.provider

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.faltenreich.skeletonlayout.SkeletonLayout
import com.google.gson.Gson
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLPaymentAccountModel
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.models.data.HLUserModel
import com.horselinc.models.event.HLNewNotificationEvent
import com.horselinc.models.event.HLSubmitInstantPayoutEvent
import com.horselinc.models.event.HLUpdateUserEvent
import com.horselinc.views.activities.*
import com.horselinc.views.adapters.recyclerview.HLRateAdapter
import com.horselinc.views.fragments.HLEventBusFragment
import com.horselinc.views.fragments.common.HLNotificationFragment
import com.horselinc.views.fragments.common.HLWebViewFragment
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 *  Created by TengFei Li on 26, August, 2019
 */

class HLProviderProfileFragment : HLEventBusFragment() {

    private var ivNotiBadge: ImageView? = null
    private var ivAvatar: RoundedImageView? = null
    private var tvName: TextView? = null
    private var tvPhone: TextView? = null
    private var lstRates: RecyclerView? = null
    private var tvViewStripe: TextView? = null

    private var skeletonLayout: SkeletonLayout? = null
    private var balanceTextView: TextView? = null
    private var descTextView1: TextView? = null
    private var descTextView2: TextView? = null
    private var payoutButton: Button? = null

    private val services = ArrayList<HLServiceProviderServiceModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_provider_profile, container, false)
            initControls()
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
        showProfileInfo()
        getProviderServices()
        getUnreadNotificationCount()
        getAccountInfo ()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode ) {
                ActivityRequestCode.ADD_STRIPE_ACCOUNT -> {
                    HLGlobalData.me?.let { user ->
                        showProgressDialog()
                        HLFirebaseService.instance.getUser(user.uid, object: ResponseCallback<HLUserModel> {
                            override fun onSuccess(data: HLUserModel) {
                                hideProgressDialog()

                                // set user data
                                HLGlobalData.me?.serviceProvider = data.serviceProvider
                                App.preference.put(PreferenceKey.USER, Gson().toJson(HLGlobalData.me))

                                showProfileInfo()
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedUpdateUserEvent (event: HLUpdateUserEvent) {
        try {
            showProfileInfo()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedSubmitInstantPayoutEvent (event: HLSubmitInstantPayoutEvent) {
        try {
            activity?.let { act ->
                AlertDialog.Builder(act)
                    .setTitle("Instant payout submitted")
                    .setMessage("Your instant payout has been submitted. It should be credited to your account in under 30 minutes. Your remaining balance and any future payments will be automatically deposited unless you initiate another instant payout.")
                    .setNegativeButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private fun initControls() {
        // controls
        ivNotiBadge = rootView?.findViewById(R.id.ivNotiBadge)
        ivAvatar = rootView?.findViewById(R.id.ivAvatar)
        tvName = rootView?.findViewById(R.id.tvName)
        tvPhone = rootView?.findViewById(R.id.tvPhone)
        lstRates = rootView?.findViewById(R.id.lstRates)
        tvViewStripe = rootView?.findViewById(R.id.tvViewStripe)

        skeletonLayout = rootView?.findViewById(R.id.skeletonLayout)
        balanceTextView = rootView?.findViewById(R.id.balanceTextView)
        descTextView1 = rootView?.findViewById(R.id.descTextView1)
        descTextView2 = rootView?.findViewById(R.id.descTextView2)
        payoutButton = rootView?.findViewById(R.id.payoutButton)

        lstRates?.run {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            adapter = HLRateAdapter(activity, services)
        }

        skeletonLayout?.showSkeleton()
        /*descTextView2?.visibility = View.GONE
        descTextView1?.visibility = View.GONE
        payoutButton?.visibility = View.GONE*/

        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btNotification)?.setOnClickListener { replaceFragment(HLNotificationFragment(), R.id.mainContainer) }
        rootView?.findViewById<ImageButton>(R.id.btEdit)?.setOnClickListener { replaceFragment(HLProviderEditProfileFragment(), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytViewStripe)?.setOnClickListener { onViewStripe() }
        rootView?.findViewById<RelativeLayout>(R.id.lytContact)?.setOnClickListener { onContactHorseLinc() }
        rootView?.findViewById<RelativeLayout>(R.id.lytTerms)?.setOnClickListener { replaceFragment(
            HLWebViewFragment(HLGlobalData.settings.urls.terms), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytPrivacy)?.setOnClickListener { replaceFragment(
            HLWebViewFragment(HLGlobalData.settings.urls.privacy), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytShare)?.setOnClickListener { onClickShare() }
        rootView?.findViewById<RelativeLayout>(R.id.lytLogout)?.setOnClickListener { onLogout() }
        rootView?.findViewById<RelativeLayout>(R.id.lytSwitch)?.setOnClickListener { onSwitchToHorseManager() }
        payoutButton?.setOnClickListener { onClickPayout () }
    }

    /**
     * Get Data Handlers
     */
    private fun getProviderServices() {
        HLGlobalData.me?.serviceProvider?.let {
            HLFirebaseService.instance.getProviderServices(it.userId, object : ResponseCallback<List<HLServiceProviderServiceModel>> {
                override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                    services.clear()
                    services.addAll(data)

                    lstRates?.adapter?.notifyDataSetChanged()
                }

                override fun onFailure(error: String) {
                    showErrorMessage(error)
                }
            })
        }
    }

    private fun getUnreadNotificationCount() {
        HLGlobalData.me?.serviceProvider?.let {
            HLFirebaseService.instance.getUnreadNotificationCount(it.userId, object :
                ResponseCallback<Int> {
                override fun onSuccess(data: Int) {
                    ivNotiBadge?.visibility = if (0 < data) View.VISIBLE else View.GONE
                }
            })
        }
    }

    private fun getAccountInfo () {
        HLGlobalData.me?.serviceProvider?.account?.let { account ->
            HLFirebaseService.instance.retrieveAccountInfo(account.id, object: ResponseCallback<HLPaymentAccountModel> {
                override fun onSuccess(data: HLPaymentAccountModel) {

                    // set controls
                    skeletonLayout?.showOriginal()
                    setAccountInfo (data)
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        } ?: hideAccountInfo ()
    }

    /**
     * Set Information Handlers
     */
    @SuppressLint("SetTextI18n")
    private fun showProfileInfo() {
        HLGlobalData.me?.serviceProvider?.apply {
            ivAvatar?.loadImage(avatarUrl, R.drawable.ic_profile)
            tvName?.text = name
            tvPhone?.text = "$location $phone"

            tvViewStripe?.text = account?.let {
                getString(R.string.view_stripe_merchat_account)
            } ?: getString(R.string.enable_invoicing)
        }
    }

    private fun setAccountInfo (account: HLPaymentAccountModel) {
        HLGlobalData.paymentAccount = account

        if (account.isInstant) {
            descTextView1?.visibility = View.GONE
            descTextView2?.visibility = View.GONE
            payoutButton?.visibility = View.VISIBLE
        } else {
            descTextView1?.visibility = View.VISIBLE
            descTextView2?.visibility = View.VISIBLE
            payoutButton?.visibility = View.GONE
        }

        balanceTextView?.text = account.getCurrencyString()

        if (account.balance.amount > 0) {
            payoutButton?.isEnabled = true
            payoutButton?.alpha = 1.0f
        } else {
            payoutButton?.isEnabled = false
            payoutButton?.alpha = 0.2f
        }
    }

    private fun hideAccountInfo () {
        skeletonLayout?.showOriginal()
        skeletonLayout?.visibility = View.GONE
    }


    /**
     *  Event Handlers
     */
    private fun onClickPayout () {
        startActivity(Intent(activity, HLInstantPayoutActivity::class.java))
    }

    private fun openEmail() {
        val i = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${HLGlobalData.settings.emails.contact}")
            putExtra(Intent.EXTRA_SUBJECT, "Contact Us")
        }

        try {
            startActivity(Intent.createChooser(i, "Send mail..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            showErrorMessage("There are no email clients installed.")
        }

    }

    private fun openCall() {
        // need to check if permission is granted to call phone
        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${HLGlobalData.settings.phones.contact}"))
            startActivity(intent)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1000)
        }
    }

    private fun editPaymentInformation() {
        HLGlobalData.me?.serviceProvider?.account?.id?.let {
            showProgressDialog()
            HLFirebaseService.instance.getExpressLoginUrl(it, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    hideProgressDialog()
                    replaceFragment(HLWebViewFragment(data), R.id.mainContainer)
                }

                override fun onFailure(error: String) {
                    hideProgressDialog()
                    showErrorMessage(error)
                }
            })
        }
    }

    private fun addPaymentInformation() {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.app_name)
            .setMessage(R.string.add_payment_desc)
            .setPositiveButton("Continue") { _, _ ->
                startActivityForResult(Intent(activity, HLAddStripeAccountActivity::class.java), ActivityRequestCode.ADD_STRIPE_ACCOUNT)
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .show()
    }

    private fun logout () {
        showProgressDialog()
        HLFirebaseService.instance.logout(object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                hideProgressDialog()

                HLFirebaseService.instance.removeListeners()
                HLGlobalData.me = null

                activity?.startActivity(Intent(activity, HLAuthActivity::class.java))
                activity?.finish()
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }

    private fun onViewStripe() {
        HLGlobalData.me?.serviceProvider?.account?.let {
            editPaymentInformation()
        } ?: addPaymentInformation()
    }

    private fun onContactHorseLinc() {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.app_name)
            .setCancelable(true)
            .setItems(arrayOf("Email", "Call")) { _, i ->
                when(i) {
                    0 -> openEmail()
                    1 -> openCall()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->

            }.show()
    }

    private fun onClickShare () {
        HLGlobalData.me?.let { user ->
            activity?.let { act ->
                showProgressDialog()
                HLBranchService.createInviteLink(act, user.uid, HLUserType.PROVIDER, callback = object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        hideProgressDialog()

                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Signup for HorseLinc $data to manager all of your equine needs.")
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        startActivity(shareIntent)
                    }

                    override fun onFailure(error: String) {
                        showError(error)
                    }
                })
            }
        }
    }

    private fun onLogout() {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.app_name))
                .setMessage(R.string.msg_alert_logout)
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Continue") { dialog, _ ->
//                    HLHorseManager.shared.removeServiceRequestListener()
                    logout ()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun onSwitchToHorseManager() {
        HLGlobalData.me?.horseManager?.let {
            val cloned = HLGlobalData.me!!.copy()
            cloned.type = HLUserType.MANAGER

            showProgressDialog()

            HLFirebaseService.instance.updateUser(cloned, false, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    hideProgressDialog()
                    HLGlobalData.me = cloned
                    HLFirebaseService.instance.removeListeners()

                    activity?.apply {
                        startActivity(Intent(this, HLHorseManagerMainActivity::class.java))
                        finish()
                    }
                }

                override fun onFailure(error: String) {
                    hideProgressDialog()
                    showErrorMessage(error)
                }
            })
        } ?: kotlin.run {
            activity?.apply {
                AlertDialog.Builder(this)
                    .setTitle("Manager Signup")
                    .setMessage("The next steps will create your manager profile. You will need your bank account or debit card.")
                    .setPositiveButton("Continue") { _, _ ->
                        HLFirebaseService.instance.removeListeners()
                        val intent = Intent(this, HLUserRoleActivity::class.java).apply {
                            putExtra(IntentExtraKey.USER_ROLE_TYPE, HLUserType.MANAGER)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewNotification (event: HLNewNotificationEvent) {
        try {
            getUnreadNotificationCount()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}