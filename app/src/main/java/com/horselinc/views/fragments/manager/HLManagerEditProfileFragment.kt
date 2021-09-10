package com.horselinc.views.fragments.manager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.event.HLUpdateUserEvent
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.StorageUtil
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.auth.HLChangeEmailFragment
import com.horselinc.views.fragments.auth.HLChangePasswordFragment
import com.jakewharton.rxbinding2.widget.RxTextView
import com.makeramen.roundedimageview.RoundedImageView
import com.yalantis.ucrop.UCrop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.TimeUnit

/**
 *  Created by TengFei Li on 26, August, 2019
 */

class HLManagerEditProfileFragment : HLBaseFragment() {

    private var ivAvatar: RoundedImageView? = null
    private var etName: EditText? = null
    private var etBarnName: EditText? = null
    private var etPhone: EditText? = null
    private var tvLocation: TextView? = null
    private var etLocation: EditText? = null
    private var btSave: Button? = null

    private lateinit var permissionUtil: PermissionUtil
    private var avatarUri: Uri? = null
    private val disposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_manager_edit_profile, container, false)

            initControls()
        }

        return rootView
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return

        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (PermissionUtil.PERMISSION_REQUEST_CODE == requestCode) {
            if (granted) {
                selectPhoto()
            } else {
                showInfoMessage("Until grant the permission, you cannot use this function")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode ) {
                ActivityRequestCode.PLACE_AUTO_COMPLETE -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(it)
                        tvLocation?.text = place.address
                        etLocation?.setText("")
                    }
                }
                ActivityRequestCode.CAMERA -> showCrop ()
                ActivityRequestCode.GALLERY -> cropImage(data?.data)
                UCrop.REQUEST_CROP -> try {
                    avatarUri = UCrop.getOutput(data!!)
                    if (avatarUri != null) {
                        ivAvatar?.apply {
                            setImageURI(avatarUri)
                        }
                    } else {
                        showInfoMessage("Cannot retrieve cropped image")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun initControls() {
        // controls
        ivAvatar = rootView?.findViewById(R.id.ivAvatar)
        etName = rootView?.findViewById(R.id.etName)
        etBarnName = rootView?.findViewById(R.id.etBarnName)
        etPhone = rootView?.findViewById(R.id.etPhone)
        tvLocation = rootView?.findViewById(R.id.tvLocation)
        etLocation = rootView?.findViewById(R.id.etLocation)
        btSave = rootView?.findViewById(R.id.btSave)

        permissionUtil = PermissionUtil(this)

        HLGlobalData.me?.horseManager?.let {
            ivAvatar?.loadImage(it.avatarUrl, R.drawable.ic_camera_alt)
            etName?.setText(it.name)
            etBarnName?.setText(it.barnName)
            etPhone?.setText(it.phone)
            tvLocation?.text = it.location
        }

        etLocation?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { address ->
                        if (address.trim().isNotEmpty()) {
                            tvLocation?.text = ""
                        }
                    }
            )
        }

        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btBack)?.setOnClickListener { popFragment() }
        ivAvatar?.setOnClickListener { onAvatar() }
        rootView?.findViewById<RelativeLayout>(R.id.lytLocation)?.setOnClickListener { onLocation() }
        rootView?.findViewById<TextView>(R.id.tvChangeEmail)?.setOnClickListener { replaceFragment(HLChangeEmailFragment()) }
        rootView?.findViewById<TextView>(R.id.tvChangePassword)?.setOnClickListener { replaceFragment(HLChangePasswordFragment()) }
        btSave?.setOnClickListener { onSave() }
    }

    private fun selectPhoto () {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Select Photo")
                .setItems(arrayOf("Take Photo", "Choose Photo")) { _, which ->
                    if (which == 0) {
                        showCamera()
                    } else {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        intent.type = "image/*"
                        startActivityForResult(
                            Intent.createChooser(intent, getString(R.string.app_name)),
                            ActivityRequestCode.GALLERY)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun checkPermission () {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!permissionUtil.checkPermissions(permissions)) {
            permissionUtil.requestPermissions(permissions)
        } else {
            selectPhoto()
        }
    }

    private fun showCamera() {
        activity?.let {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val file = File(StorageUtil.getAppExternalDataDirectoryFileForCache(), "/photo.jpg")
            val fileUri = FileProvider.getUriForFile(it, "com.horselinc.provider", file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            startActivityForResult(intent, ActivityRequestCode.CAMERA)
        }
    }

    private fun showCrop () {
        val file = File(StorageUtil.getAppExternalDataDirectoryFileForCache(), "/photo.jpg")
        cropImage (Uri.fromFile(file))
    }

    private fun cropImage (uri: Uri?) {
        uri?.let {
            var uCrop = UCrop.of(it, Uri.fromFile(File(StorageUtil.getAppExternalDataDirectoryFileForCache(), "/avatar_horse_manager.jpg")))

            // basic option
            uCrop = uCrop.withAspectRatio(1f, 1f)

            // advanced option
            val options = UCrop.Options().apply {
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                withMaxResultSize(640, 640)
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)
            }

            uCrop.withOptions(options).apply {
                activity?.let { act ->
                    start(act, this@HLManagerEditProfileFragment)
                }
            }
        }
    }

    // MARK: - Update Account Handlers
    private fun uploadAvatarImage(avatarUri: Uri) {
        HLGlobalData.me?.let {
            HLFirebaseService.instance.uploadAvatar(it.uid, HLUserType.MANAGER, avatarUri, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    HLGlobalData.me?.horseManager?.avatarUrl = data
                    updateAccount()
                }

                override fun onFailure(error: String) {
                    hideProgressButton(btSave, stringResId = R.string.save)
                    showError(error)
                }
            })
        }
    }

    private fun updateAccount() {
        HLGlobalData.me?.copy()?.apply {
            horseManager?.name = etName?.text?.trim().toString()
            horseManager?.barnName = etBarnName?.text?.trim().toString()
            horseManager?.phone = etPhone?.text?.trim().toString()
            horseManager?.location = if ((tvLocation?.text ?: "").isNotBlank()) tvLocation?.text.toString() else etLocation?.text?.trim().toString()

            HLFirebaseService.instance.updateUser(this, false, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    HLGlobalData.me = this@apply
                    EventBus.getDefault().post(HLUpdateUserEvent())
                    popFragment()
                }

                override fun onFailure(error: String) {
                    hideProgressButton(btSave, stringResId = R.string.save)
                    showError(error)
                }
            })
        } ?: hideProgressButton(btSave, stringResId = R.string.save)

    }

    private fun onAvatar() {
        hideKeyboard()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            selectPhoto()
        } else {
            checkPermission()
        }
    }

    private fun onLocation() {
        activity?.let {
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN,
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
            )
                .build(it)
            startActivityForResult(intent, ActivityRequestCode.PLACE_AUTO_COMPLETE)
        }
    }

    private fun onSave() {
        if ((etName?.text ?: "").isBlank()) {
            showErrorMessage("Name is required")
            return
        }
        if ((etBarnName?.text ?: "").isBlank()) {
            showErrorMessage("Barn Name is required")
            return
        }
        if ((etPhone?.text ?: "").isBlank()) {
            showErrorMessage("Phone number is required")
            return
        }

        showProgressButton(btSave)

        avatarUri?.let(::uploadAvatarImage) ?: updateAccount()
    }

}