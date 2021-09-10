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
import android.telephony.PhoneNumberFormattingTextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.models.event.HLUpdateUserEvent
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.StorageUtil
import com.horselinc.views.adapters.recyclerview.HLProviderServiceEditDeleteAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.auth.HLChangeEmailFragment
import com.horselinc.views.fragments.auth.HLChangePasswordFragment
import com.horselinc.views.fragments.role.HLAddProviderServiceDialogFragment
import com.horselinc.views.listeners.HLProviderServiceEditDeleteListener
import com.horselinc.views.listeners.HLProviderServiceListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.makeramen.roundedimageview.RoundedImageView
import com.yalantis.ucrop.UCrop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/** Created by jcooperation0137 on 2019-08-31.
 */
class HLProviderEditProfileFragment: HLBaseFragment() {

    private var ivAvatar: RoundedImageView? = null
    private var etName: EditText? = null
    private var etPhone: EditText? = null
    private var tvLocation: TextView? = null
    private var etLocation: EditText? = null
    private var btSave: Button? = null
    private var recyclerView: RecyclerView? = null

    private lateinit var permissionUtil: PermissionUtil
    private var avatarUri: Uri? = null
    private val disposable = CompositeDisposable()
    private var adapter: HLProviderServiceEditDeleteAdapter? = null
    private var providerServices = ArrayList<HLServiceProviderServiceModel>()

    private val phoneUtil = PhoneNumberUtil.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_provider_edit_profile, container, false)
            getServices()
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
        permissionUtil = PermissionUtil(this)

        // controls
        ivAvatar = rootView?.findViewById(R.id.ivAvatar)
        etName = rootView?.findViewById(R.id.etName)
        etPhone = rootView?.findViewById(R.id.etPhone)
        tvLocation = rootView?.findViewById(R.id.tvLocation)
        etLocation = rootView?.findViewById(R.id.etLocation)
        btSave = rootView?.findViewById(R.id.btSave)
        recyclerView = rootView?.findViewById(R.id.recyclerView)

        HLGlobalData.me?.serviceProvider?.let {
            ivAvatar?.loadImage(it.avatarUrl, R.drawable.ic_camera_alt)
            etName?.setText(it.name)
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
        etPhone?.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        rootView?.findViewById<RelativeLayout>(R.id.lytLocation)?.setOnClickListener { onLocation() }
        rootView?.findViewById<TextView>(R.id.tvChangeEmail)?.setOnClickListener { replaceFragment(HLChangeEmailFragment()) }
        rootView?.findViewById<TextView>(R.id.tvChangePassword)?.setOnClickListener { replaceFragment(HLChangePasswordFragment()) }
        btSave?.setOnClickListener { onSave() }
    }

    private fun setServices() {
        adapter?.let {
            recyclerView?.adapter?.notifyDataSetChanged()
        }

        adapter?:let {
            adapter = HLProviderServiceEditDeleteAdapter(context,
                providerServices,
                object : HLProviderServiceEditDeleteListener {
                    override fun onClickDelete(
                        rowIndex: Int,
                        service: HLServiceProviderServiceModel
                    ) {
                        val dialog = android.app.AlertDialog.Builder(activity)
                        dialog.apply {
                            setTitle(getString(R.string.app_name))
                            setMessage("Do you want to delete this service?")
                            setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                deleteService(rowIndex, service)
                            }.show()
                        }
                    }

                    override fun onClickEdit(
                        rowIndex: Int,
                        service: HLServiceProviderServiceModel
                    ) {
                        editService(rowIndex)
                    }

                    override fun onClickAddAnother() {
                        editService(-1)
                    }
                })
            recyclerView?.layoutManager = LinearLayoutManager(activity)
            recyclerView?.adapter = adapter
        }
    }

    private fun deleteService(rowIndeX: Int, service: HLServiceProviderServiceModel) {
        HLFirebaseService.instance.deleteProviderService(service.uid,
            object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    providerServices.removeAt(rowIndeX)
                    recyclerView?.adapter?.notifyDataSetChanged()
                }

                override fun onFailure(error: String) {
                    showErrorMessage(error)
                }
            })
    }

    private fun editService (selectedPosition: Int = -1) {
        activity?.let {
            val fragment = HLAddProviderServiceDialogFragment(null, adapter,
                selectedPosition,
                object : HLProviderServiceListener {
                    override fun onAdd(service: HLServiceProviderServiceModel) {
                        providerServices.add(service)
                        adapter?.notifyItemInserted(providerServices.size - 1)
                    }

                    override fun onUpdate(service: HLServiceProviderServiceModel, position: Int) {
                        adapter?.notifyItemChanged(position)
                    }
                })
            fragment.show(it.supportFragmentManager, "Add Service Fragment")
        }
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
                    start(act, this@HLProviderEditProfileFragment)
                }
            }
        }
    }

    // MARK: - Get data from the server.
    private fun getServices() {
        showProgressDialog()
        HLGlobalData.me?.serviceProvider?.userId?.let {
            HLFirebaseService.instance.getProviderServices(it, object :
                ResponseCallback<List<HLServiceProviderServiceModel>> {
                override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                    hideProgressDialog()
                    providerServices.addAll(data)
                    setServices()
                }

                override fun onFailure(error: String) {
                    hideProgressDialog()
                }
            })
        }
    }

    // MARK: - Update Account Handlers
    private fun uploadAvatarImage(avatarUri: Uri) {
        HLGlobalData.me?.let {
            HLFirebaseService.instance.uploadAvatar(it.uid, HLUserType.PROVIDER, avatarUri, object :
                ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    HLGlobalData.me?.serviceProvider?.avatarUrl = data
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
            serviceProvider?.name = etName?.text?.trim().toString()
            serviceProvider?.location = if ((tvLocation?.text ?: "").isNotBlank()) tvLocation?.text.toString() else etLocation?.text?.trim().toString()

            try {
                val phoneProto = phoneUtil.parse(etPhone?.text?.trim().toString(), Locale.getDefault().country)
                serviceProvider?.phone = phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164)

                HLFirebaseService.instance.updateUser(this, false, object : ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        HLGlobalData.me = this@apply
                        EventBus.getDefault().post(HLUpdateUserEvent())
                        saveAllServices()
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(btSave, stringResId = R.string.save)
                        showError(error)
                    }
                })
            } catch (e: NumberParseException) {
                showError(e.localizedMessage)
                hideProgressButton(btSave, stringResId = R.string.save)
            }
        } ?: hideProgressButton(btSave, stringResId = R.string.save)

    }

    private fun saveAllServices() {
        // Add service provider info to services.
        providerServices.forEach { service ->
            HLGlobalData.me?.serviceProvider?.userId?.let {
                service.userId = it
            }
        }

        HLFirebaseService.instance.putServiceProviderServices(providerServices, object : ResponseCallback<List<HLServiceProviderServiceModel>> {
            override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                popFragment()
            }

            override fun onFailure(error: String) {
                showErrorMessage(error)
            }
        })
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

        if ((etPhone?.text ?: "").isBlank()) {
            showErrorMessage("Phone number is required")
            return
        }

        showProgressButton(btSave)

        avatarUri?.let(::uploadAvatarImage) ?: updateAccount()
    }
}