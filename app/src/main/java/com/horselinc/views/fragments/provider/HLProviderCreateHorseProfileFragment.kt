package com.horselinc.views.fragments.provider


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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.event.HLSearchEvent
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.StorageUtil
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSelectYearDialogFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.listeners.HLSelectYearDialogListener
import com.horselinc.views.listeners.HLSpinnerDialogListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.makeramen.roundedimageview.RoundedImageView
import com.yalantis.ucrop.UCrop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.TimeUnit

class HLProviderCreateHorseProfileFragment : HLBaseFragment() {

    /**
     * Controls
     */
    private var horseProfileImageView: RoundedImageView? = null
    private var horseBarnNameEditText: EditText? = null
    private var horseNameEditText: EditText? = null
    private var horseGenderTextView: TextView? = null
    private var horseBirthYearTextView: TextView? = null

    private var horseDescEditText: EditText? = null

    private var createButton: Button? = null

    /**
     *  Permission Util
     */
    private lateinit var permissionUtil: PermissionUtil

    /**
     *  Horse Profile Avatar Uri
     */
    private var avatarUri: Uri? = null

    /**
     *  Horse Data
     */
    private var cloneHorse: HLHorseModel? = null

    /**
     *  Observables
     */
    private val disposable = CompositeDisposable ()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_create_horse_profile, container, false)

            permissionUtil = PermissionUtil(this)

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
                ActivityRequestCode.CAMERA -> showCrop ()
                ActivityRequestCode.GALLERY -> cropImage(data?.data)
                UCrop.REQUEST_CROP -> try {
                    avatarUri = UCrop.getOutput(data!!)
                    if (avatarUri != null) {
                        horseProfileImageView?.apply {
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

    private fun onClickGender () {
        hideKeyboard()

        fragmentManager?.let { fm ->
            val selectedPosition = HLConstants.HORSE_GENDERS.indexOf(horseGenderTextView?.text.toString())
            val fragment = HLSpinnerDialogFragment (
                "Select Horse Gender",
                HLConstants.HORSE_GENDERS,
                if (selectedPosition == -1) 0 else selectedPosition,
                object: HLSpinnerDialogListener {
                    override fun onClickPositive(position: Int, data: Any) {
                        horseGenderTextView?.text = if (position == 0) "" else (data as String)
                    }
                })
            fragment.show(fm, "Select Horse Gender Fragment")
        }
    }

    private fun onClickBirthYear () {
        hideKeyboard()

        fragmentManager?.let { fm ->
            val selectedYear = horseBirthYearTextView?.text.toString()
            val fragment = HLSelectYearDialogFragment (
                if (selectedYear.isEmpty()) -1 else selectedYear.toInt(),
                object: HLSelectYearDialogListener {
                    override fun onClickPositive(year: Int) {
                        horseBirthYearTextView?.text = year.toString()
                    }
                })
            fragment.show(fm, "Select Horse Birth Year Fragment")
        }
    }

    private fun onClickCreate () {
        hideKeyboard()

        createButton?.isEnabled = false

        val barnName = horseBarnNameEditText?.text?.trim().toString()
        val name = horseNameEditText?.text?.trim().toString()
        val gender = horseGenderTextView?.text?.trim().toString()
        val birthYear = horseBirthYearTextView?.text?.trim().toString()

        cloneHorse = HLHorseModel().apply {
            this.barnName = barnName
            displayName = name
            this.gender = gender

            if (birthYear.isNotEmpty()) {
                this.birthYear = birthYear.toInt()
            }

            // note
            description = horseDescEditText?.text?.trim().toString()
        }
        addHorse ()
    }



    /**
     * Initialize Handlers
     */
    private fun initControls () {

        // controls
        horseProfileImageView = rootView?.findViewById(R.id.horseProfileImageView)
        horseBarnNameEditText = rootView?.findViewById(R.id.horseBarnNameEditText)
        horseNameEditText = rootView?.findViewById(R.id.horseShowNameEditText)
        horseGenderTextView = rootView?.findViewById(R.id.horseGenderTextView)
        horseBirthYearTextView = rootView?.findViewById(R.id.horseBirthYearTextView)

        horseDescEditText = rootView?.findViewById(R.id.horseDescEditText)

        createButton = rootView?.findViewById(R.id.createButton)
        createButton?.isEnabled = false
        createButton?.alpha = 0.2f

        // event handler
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }

        horseProfileImageView?.setOnClickListener { onClickProfileImage () }

        horseGenderTextView?.setOnClickListener { onClickGender () }
        horseBirthYearTextView?.setOnClickListener { onClickBirthYear () }

        createButton?.setOnClickListener { onClickCreate () }
    }

    private fun setReactiveX () {
        horseBarnNameEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { cs ->
                        val barnName = cs.trim().toString()
                        val isEnabled = if (barnName.isEmpty()) {
                            horseBarnNameEditText?.error = getString(R.string.msg_err_required)
                            false
                        } else {
                            true
                        }

                        createButton?.isEnabled = isEnabled
                        createButton?.alpha = if (isEnabled) 1.0f else 0.2f
                    })
        }
    }


    /**
     *  Profile Image Handlers
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
            val file = File(StorageUtil.getAppExternalDataDirectoryFileForCache(), "/horse.jpg")
            val fileUri = FileProvider.getUriForFile(it, "com.horselinc.provider", file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            startActivityForResult(intent, ActivityRequestCode.CAMERA)
        }
    }

    private fun showCrop () {
        val file = File(StorageUtil.getAppExternalDataDirectoryFileForCache(), "/horse.jpg")
        cropImage (Uri.fromFile(file))
    }

    private fun cropImage (uri: Uri?) {
        uri?.let {
            var uCrop = UCrop.of(it, Uri.fromFile(File(StorageUtil.getAppExternalDataDirectoryFileForCache(), "/avatar_horse.jpg")))

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
                    start(act, this@HLProviderCreateHorseProfileFragment)
                }
            }
        }
    }


    /**
     *  Horse Handlers
     */
    private fun addHorse () {
        cloneHorse?.let { horse ->
            showProgressDialog()
            HLFirebaseService.instance.addHorse(horse, avatarUri, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {

                    hideProgressDialog()
                    horse.uid = data // horse uid

                    EventBus.getDefault().post(HLSearchEvent(horse))
                    activity?.finish()
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                    cloneHorse = null
                    createButton?.isEnabled = true
                }
            })
        }
    }
}
