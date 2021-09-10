package com.horselinc.views.fragments.manager


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLDeepLinkInviteModel
import com.horselinc.models.data.HLUserModel
import com.horselinc.models.event.HLNewNotificationEvent
import com.horselinc.models.event.HLUpdateUserEvent
import com.horselinc.views.activities.HLAuthActivity
import com.horselinc.views.activities.HLServiceProviderMainActivity
import com.horselinc.views.activities.HLUserRoleActivity
import com.horselinc.views.fragments.HLEventBusFragment
import com.horselinc.views.fragments.common.HLNotificationFragment
import com.horselinc.views.fragments.common.HLWebViewFragment
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 *  Created by TengFei Li on 25, August, 2019
 */

class HLManagerProfileFragment : HLEventBusFragment() {

    private var ivNotiBadge: ImageView? = null
    private var ivAvatar: RoundedImageView? = null
    private var tvName: TextView? = null
    private var tvBarn: TextView? = null
    private var tvInfo: TextView? = null
    private var tvEditPayment: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_manager_profile, container, false)

            initControls()
        }

        showProfileInfo()
        getUnreadNotificationCount()

        return rootView
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedUpdateUserEvent (event: HLUpdateUserEvent) {
        try {
            showProfileInfo()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initControls() {
        // controls
        ivNotiBadge = rootView?.findViewById(R.id.ivNotiBadge)
        ivAvatar = rootView?.findViewById(R.id.ivAvatar)
        tvName = rootView?.findViewById(R.id.tvName)
        tvBarn = rootView?.findViewById(R.id.tvBarn)
        tvInfo = rootView?.findViewById(R.id.tvInfo)
        tvEditPayment = rootView?.findViewById(R.id.tvEditPayment)


        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btNotification)?.setOnClickListener { replaceFragment(HLNotificationFragment(), R.id.mainContainer) }
        rootView?.findViewById<ImageButton>(R.id.btEdit)?.setOnClickListener { replaceFragment(HLManagerEditProfileFragment(), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytProviders)?.setOnClickListener { replaceFragment(HLManagerServiceProvidersFragment(), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytEditPayment)?.setOnClickListener { replaceFragment(HLManagerEditPaymentOptionsFragment (), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytContact)?.setOnClickListener { onContactHorseLinc() }
        rootView?.findViewById<RelativeLayout>(R.id.lytTerms)?.setOnClickListener { replaceFragment(
            HLWebViewFragment(HLGlobalData.settings.urls.terms), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytPrivacy)?.setOnClickListener { replaceFragment(
            HLWebViewFragment(HLGlobalData.settings.urls.privacy), R.id.mainContainer) }
        rootView?.findViewById<RelativeLayout>(R.id.lytShare)?.setOnClickListener { onClickShare() }
        rootView?.findViewById<RelativeLayout>(R.id.lytLogout)?.setOnClickListener { onLogout() }
        rootView?.findViewById<RelativeLayout>(R.id.lytSwitch)?.setOnClickListener { onSwitchToProviderProfile() }

    }

    @SuppressLint("SetTextI18n")
    private fun showProfileInfo() {
        HLGlobalData.me?.horseManager?.run {

            ivAvatar?.loadImage(avatarUrl, R.drawable.ic_profile)

            tvName?.text = name
            tvBarn?.text = "\"$barnName\""
            tvInfo?.text = "$location $phone"

            tvEditPayment?.text = customer?.defaultSource?.let {
                if (it.isNotEmpty()) getString(R.string.edit_payment_information)
                else getString(R.string.add_payment_information)
            } ?: getString(R.string.add_payment_information)
        }
    }

    private fun getUnreadNotificationCount() {
        HLGlobalData.me?.horseManager?.let {
            HLFirebaseService.instance.getUnreadNotificationCount(it.userId, object : ResponseCallback<Int> {
                override fun onSuccess(data: Int) {
                    ivNotiBadge?.visibility = if (0 < data) View.VISIBLE else View.GONE
                }
            })
        }
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
                HLBranchService.createInviteLink(act, user.uid, HLUserType.MANAGER, callback = object: ResponseCallback<String> {
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
                    HLFirebaseService.instance.removeListeners()
                    logout ()
                    dialog.dismiss()
                }
                .show()
        }
    }


    private fun onSwitchToProviderProfile() {
        HLGlobalData.me?.serviceProvider?.let {
            val cloned = HLGlobalData.me!!.copy()
            cloned.type = HLUserType.PROVIDER

            showProgressDialog()

            HLFirebaseService.instance.updateUser(cloned, false, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    hideProgressDialog()
                    HLGlobalData.me = cloned

                    activity?.apply {
                        startActivity(Intent(this, HLServiceProviderMainActivity::class.java))
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
                    .setTitle("Provider Sign Up")
                    .setMessage("The next steps will create your provider profile. You will need your bank account or debit card.")
                    .setPositiveButton("Continue") { _, _ ->
                        val intent = Intent(this, HLUserRoleActivity::class.java).apply {
                            putExtra(IntentExtraKey.USER_ROLE_TYPE, HLUserType.PROVIDER)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
            }
        }
    }

    /**
     *  Invite Deep Link Handler
     */
    fun handleInvite(deepLinkModel: HLDeepLinkInviteModel) {
        activity?.let { act ->
            AlertDialog.Builder(act)
                .setTitle(getString(R.string.app_name))
                .setMessage(R.string.msg_confirm_add_service_provider)
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Yes") { dialog, _ ->
                    getServiceProvider (deepLinkModel.senderId)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun getServiceProvider (userId: String) {
        showProgressDialog()
        HLFirebaseService.instance.getUser(userId, object: ResponseCallback<HLUserModel> {
            override fun onSuccess(data: HLUserModel) {
                hideProgressDialog()
                data.serviceProvider?.let { provider ->
                    // transfer to service providers
                    replaceFragment(HLManagerServiceProvidersFragment(), R.id.mainContainer)

                    // transfer to add provider
                    replaceFragment(HLManagerAddServiceProviderFragment(provider), R.id.mainContainer)
                }
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
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