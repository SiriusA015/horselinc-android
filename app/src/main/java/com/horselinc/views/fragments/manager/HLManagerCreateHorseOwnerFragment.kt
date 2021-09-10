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
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLUserModel
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.StorageUtil
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLCreateHorseOwnerListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.makeramen.roundedimageview.RoundedImageView
import com.yalantis.ucrop.UCrop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.util.concurrent.TimeUnit

class HLManagerCreateHorseOwnerFragment(private val createOwnerListener: HLCreateHorseOwnerListener? = null) : HLBaseFragment() {

    /**
     * Controls
     */
    private var profileImageView: RoundedImageView? = null
    private var nameEditText: EditText? = null
    private var emailEditText: EditText? = null
    private var phoneEditText: EditText? = null
    private var locationEditText: EditText? = null
    private var addressEditText: EditText? = null
    private var barnNameEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var createButton: Button? = null


    /**
     *  Permission Util
     */
    private lateinit var permissionUtil: PermissionUtil

    /**
     *  Owner Profile Avatar Uri
     */
    private var avatarUri: Uri? = null

    private val disposable = CompositeDisposable()

    private lateinit var createdUser: HLUserModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_create_horse_owner, container, false)

            permissionUtil = PermissionUtil(this)

            // initialize
            initControls ()
        }

        return rootView
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
                        locationEditText?.setText(place.address)
                        addressEditText?.text?.clear()
                    }
                }
                ActivityRequestCode.CAMERA -> showCrop ()
                ActivityRequestCode.GALLERY -> cropImage(data?.data)
                UCrop.REQUEST_CROP -> try {
                    avatarUri = UCrop.getOutput(data!!)
                    if (avatarUri != null) {
                        profileImageView?.apply {
                            setImageURI(avatarUri)
                        }
                    } else {
                        showErrorMessage("Cannot retrieve cropped image")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        locationEditText?.clearFocus()
    }


    /**
     *  Initialize Handler
     */
    private fun initControls () {

        // initialize controls
        profileImageView = rootView?.findViewById(R.id.profileImageView)
        nameEditText = rootView?.findViewById(R.id.nameEditText)
        emailEditText = rootView?.findViewById(R.id.emailEditText)
        phoneEditText = rootView?.findViewById(R.id.phoneEditText)
        locationEditText = rootView?.findViewById(R.id.locationEditText)
        addressEditText = rootView?.findViewById(R.id.addressEditText)
        barnNameEditText = rootView?.findViewById(R.id.barnNameEditText)
        passwordEditText = rootView?.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = rootView?.findViewById(R.id.confirmPasswordEditText)
        createButton = rootView?.findViewById(R.id.createButton)

        // bind progress buttons
        setProgressButton(createButton)

        // bind event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        profileImageView?.setOnClickListener { onClickProfileImage() }
        createButton?.setOnClickListener { onClickCreate () }

        phoneEditText?.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        locationEditText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activity?.let {
                    val intent = Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.FULLSCREEN,
                        listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
                    )
                        .build(it)
                    startActivityForResult(intent, ActivityRequestCode.PLACE_AUTO_COMPLETE)
                }
            }
        }

        addressEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { address ->
                        if (address.trim().isNotEmpty()) {
                            locationEditText?.text?.clear()
                        }
                    }
            )
        }
    }

    /**
     *  Event Handlers
     */
    private fun onClickProfileImage () {
        hideKeyboard()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            selectPhoto()
        } else {
            checkPermission()
        }
    }

    private fun onClickCreate () {
        hideKeyboard()

        val name = nameEditText?.text?.trim().toString()
        val email = emailEditText?.text?.trim().toString()
        val phone = phoneEditText?.text?.trim().toString()
        val barn = barnNameEditText?.text?.trim().toString()
        val password = passwordEditText?.text.toString()
        val confirmPassword = confirmPasswordEditText?.text.toString()

        when {
            name.isEmpty() -> showError("Invalid name")
            email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()  -> {
                showError("Invalid email address")
            }
            phone.isEmpty() -> showError("Invalid phone number")
            barn.isEmpty() -> showError("Invalid barn name")
            password.isEmpty() -> showError("Invalid password")
            confirmPassword != password -> showError("Incorrect confirm password")
            else -> {
                showProgressButton(createButton)
                HLFirebaseService.instance.createHorseOwner(email, password, object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        createdUser = HLUserModel(data, email, status = HLUserOnlineStatus.OFFLINE, type = HLUserType.MANAGER, creatorId = HLGlobalData.me?.uid)

                        createdUser.horseManager = HLHorseManagerModel().apply {
                            userId = createdUser.uid
                            this.name = name
                            this.phone = phone
                            barnName = barn

                            val locationString = locationEditText?.text?.trim().toString()
                            val addressString = addressEditText?.text?.trim().toString()
                            if (locationString.isNotEmpty()) {
                                location = locationString
                            } else if (addressString.isNotEmpty()) {
                                location = addressString
                            }
                        }


                        if (avatarUri != null) {
                            uploadAvatar ()
                        } else {
                            createHorseOwner ()
                        }
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(createButton, stringResId = R.string.create)
                        showError(error)
                    }
                })
            }
        }
    }

    private fun uploadAvatar () {
        HLFirebaseService.instance.uploadAvatar(createdUser.uid, HLUserType.MANAGER, avatarUri!!, object : ResponseCallback<String> {
            override fun onSuccess(data: String) {
                createdUser.horseManager?.avatarUrl = data
                avatarUri = null
                createHorseOwner()
            }

            override fun onFailure(error: String) {
                hideProgressButton(createButton, stringResId = R.string.create_account)
                showError(error)
            }
        })
    }

    private fun createHorseOwner () {
        // create user
        HLFirebaseService.instance.createUser(createdUser, object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                hideProgressButton(createButton, stringResId = R.string.create_account)

                createdUser.horseManager?.let {
                    createOwnerListener?.onCreatedNewHorseOwner(it)
                }

                popFragment()
                replaceFragment(HLManagerEditPaymentOptionsFragment(createdUser))
            }

            override fun onFailure(error: String) {
                hideProgressButton(createButton, stringResId = R.string.create_account)
                showError(error)
            }
        })
    }

    /**
     *  Profile Image Handler
     */
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
                    start(act, this@HLManagerCreateHorseOwnerFragment)
                }
            }
        }
    }
}
