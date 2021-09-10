package com.horselinc.views.fragments.common


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLServiceProviderModel
import com.horselinc.showInfoMessage
import com.horselinc.utils.PermissionUtil
import com.horselinc.views.adapters.recyclerview.HLPublicUserServiceRateAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.jude.easyrecyclerview.EasyRecyclerView
import com.makeramen.roundedimageview.RoundedImageView


class HLPublicUserProfileFragment(private var opponent: HLBaseUserModel?) : HLBaseFragment() {

    /**
     *  Controls
     */
    private var backButton: ImageView? = null
    private var profileImageView: RoundedImageView? = null
    private var usernameTextView: TextView? = null
    private var userBarnNameTextView: TextView? = null
    private var phoneButton: Button? = null
    private var userLocationTextView: TextView? = null
    private var rateTextView: TextView? = null
    private var rateRecyclerView: EasyRecyclerView? = null

    private lateinit var rateAdapter: HLPublicUserServiceRateAdapter
    private lateinit var permissionUtil: PermissionUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_public_user_profile, container, false)

            // initialize controls
            initControls ()

            // permission util
            permissionUtil = PermissionUtil(this)
        }

        // set user data
        setUserData ()

        return rootView
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return

        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (PermissionUtil.PERMISSION_REQUEST_CODE == requestCode) {
            if (granted) {
                openPhoneCall ()
            } else {
                showInfoMessage("Until grant the permission, you cannot use this function")
            }
        }
    }

    /**
     *  Initialize Handler
     */
    private fun initControls () {
        // controls
        backButton = rootView?.findViewById(R.id.backImageView)
        profileImageView = rootView?.findViewById(R.id.profileImageView)
        usernameTextView = rootView?.findViewById(R.id.usernameTextView)
        userBarnNameTextView = rootView?.findViewById(R.id.userBarnNameTextView)
        phoneButton = rootView?.findViewById(R.id.phoneButton)
        userLocationTextView = rootView?.findViewById(R.id.userLocationTextView)
        rateTextView = rootView?.findViewById(R.id.rateTextView)
        rateRecyclerView = rootView?.findViewById(R.id.rateRecyclerView)

        if (opponent is HLHorseManagerModel) {
            rateTextView?.visibility = View.GONE
            rateRecyclerView?.visibility = View.GONE
        } else {
            rateTextView?.visibility = View.VISIBLE
            rateRecyclerView?.visibility = View.VISIBLE
        }

        // initialize recycler view
        if (opponent is HLServiceProviderModel) {
            rateAdapter = HLPublicUserServiceRateAdapter(activity).apply {
                addAll((opponent as HLServiceProviderModel).rates)
            }

            rateRecyclerView?.run {
                adapter = rateAdapter
                setLayoutManager(LinearLayoutManager(activity))
            }
        }

        // event handlers
        backButton?.setOnClickListener { popFragment() }
        phoneButton?.setOnClickListener { onClickPhone () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickPhone () {
        activity?.let {act ->
            AlertDialog.Builder(act)
                .setTitle(opponent?.phone)
                .setItems(arrayOf("Call", "Text")) { _, which ->
                    if (which == 0) {
                        callPhone ()
                    } else {
                        openSMS ()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun callPhone () {
        val permissions = arrayOf(Manifest.permission.CALL_PHONE)
        if (!permissionUtil.checkPermissions(permissions)) {
            permissionUtil.requestPermissions(permissions)
        } else {
            openPhoneCall()
        }
    }

    private fun openPhoneCall () {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${opponent?.phone}"))
        startActivity(intent)
    }

    private fun openSMS () {
        val uri = Uri.parse("smsto: ${opponent?.phone ?: ""}")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        intent.putExtra("sms_body", "")
        startActivity(intent)
    }

    /**
     *  Set User Data
     */
    @SuppressLint("SetTextI18n")
    private fun setUserData () {

        // set user profile
        profileImageView?.loadImage(opponent?.avatarUrl, R.drawable.ic_profile)

        // set user name
        usernameTextView?.text = opponent?.name

        // barn name & location
        if (opponent is HLHorseManagerModel) {
            userBarnNameTextView?.visibility = View.VISIBLE
            userBarnNameTextView?.text = (opponent as HLHorseManagerModel).barnName

            userLocationTextView?.visibility = View.VISIBLE
            userLocationTextView?.text = opponent?.location

        } else {
            userBarnNameTextView?.visibility = View.GONE
            userLocationTextView?.visibility = View.GONE
        }

        // phone number
        if (opponent?.phone != null) {
            phoneButton?.text = opponent?.phone
            phoneButton?.isEnabled = true
        } else {
            phoneButton?.text = "No phone number"
            phoneButton?.isEnabled = false
        }
    }
}
