package com.horselinc.views.fragments.role


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.*
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.ResourceUtil
import com.horselinc.utils.StorageUtil
import com.horselinc.views.activities.HLAddStripeAccountActivity
import com.horselinc.views.activities.HLServiceProviderMainActivity
import com.horselinc.views.adapters.recyclerview.HLEditProviderServiceAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.common.HLWebViewFragment
import com.horselinc.views.listeners.HLProviderServiceItemListener
import com.horselinc.views.listeners.HLProviderServiceListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jude.easyrecyclerview.EasyRecyclerView
import com.makeramen.roundedimageview.RoundedImageView
import com.stripe.android.model.Card
import com.yalantis.ucrop.UCrop
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import java.util.concurrent.TimeUnit as TimeUnit1


class HLCreateProfileFragment (private val userType: String, private val isHiddenBack: Boolean = false) : HLBaseFragment() {

    // Common Controls
    private var backButton: ImageView? = null
    private var profileImageView: RoundedImageView? = null
    private var nameEditText: EditText? = null
    private var phoneEditText: EditText? = null
    private var locationEditText: EditText? = null
    private var addressEditText: EditText? = null
    private var termsTextView: TextView? = null
    private var createButton: Button? = null

    // Horse Manager
    private var barnNameEditText: EditText? = null

    // Service Provider
    private var addPaymentButton: CardView? = null
    private var bankImageView: ImageView? = null
    private var bankNameTextView: TextView? = null
    private var serviceRecyclerView: EasyRecyclerView? = null
    private var addServiceButton: Button? = null
    private var serviceAdapter: HLEditProviderServiceAdapter? = null

    private lateinit var permissionUtil: PermissionUtil

    private var avatarUri: Uri? = null

    private var nameChangeObservable: Observable<CharSequence>? = null
    private var phoneChangeObservable: Observable<CharSequence>? = null
//    private var barnChangeObservable: Observable<CharSequence>? = null
    private val disposable = CompositeDisposable()

    private val cloneUser = HLGlobalData.me?.copy()
    private var cloneAccount = cloneUser?.serviceProvider?.account?.copy()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            val layout = if (userType == HLUserType.MANAGER) R.layout.fragment_create_horse_manager else R.layout.fragment_create_service_provider
            rootView = inflater.inflate(layout, container, false)

            permissionUtil = PermissionUtil(this)

            // initialize controls
            initControls ()

            // set reactive x
            setReactiveX ()

            displayCommonInfo()

            // get provider services
            if (userType == HLUserType.PROVIDER) {
                getProviderServices ()
            }
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
        if (resultCode == RESULT_OK) {
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
                ActivityRequestCode.ADD_STRIPE_ACCOUNT -> {
                    HLGlobalData.me?.let { user ->
                        showProgressDialog()
                        HLFirebaseService.instance.getUser(user.uid, object: ResponseCallback<HLUserModel> {
                            override fun onSuccess(data: HLUserModel) {
                                hideProgressDialog()

                                // set user data
                                HLGlobalData.me?.serviceProvider = data.serviceProvider
                                App.preference.put(PreferenceKey.USER, Gson().toJson(HLGlobalData.me))

                                // set account data
                                cloneAccount = data.serviceProvider?.account
                                setAccountData()
                            }

                            override fun onFailure(error: String) {
                                showError(message = error)
                            }
                        })
                    }
                }
            }
        }

        locationEditText?.clearFocus()
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

    private fun onClickAddBank () {
        if (cloneAccount == null) {
            activity?.let { act ->
                AlertDialog.Builder(act)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.add_payment_desc)
                    .setPositiveButton("Continue") { _, _ ->
                        startActivityForResult(Intent(activity, HLAddStripeAccountActivity::class.java), ActivityRequestCode.ADD_STRIPE_ACCOUNT)
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
            }
        } else {
            cloneAccount?.let {
                showProgressDialog()
                HLFirebaseService.instance.getExpressLoginUrl(it.id, object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        hideProgressDialog()
                        openBrowser(data)
                    }

                    override fun onFailure(error: String) {
                        showError(message = error)
                    }
                })
            }
        }
    }

    private fun onClickAddService (selectedPosition: Int = -1) {
        activity?.let {
            val fragment = HLAddProviderServiceDialogFragment(serviceAdapter, null,
                selectedPosition,
                object : HLProviderServiceListener {
                    override fun onAdd(service: HLServiceProviderServiceModel) {
                        serviceAdapter?.add(service)
                    }

                    override fun onUpdate(service: HLServiceProviderServiceModel, position: Int) {
                        serviceAdapter?.update(service, position)
                    }
                })
            fragment.show(it.supportFragmentManager, "Add Service Fragment")
        }
    }

    private fun onClickCreateAccount () {
        cloneUser?.let { user ->
            showProgressButton(createButton)

            val phoneUtil = PhoneNumberUtil.getInstance()

            if (userType == HLUserType.MANAGER) {
                user.horseManager = HLHorseManagerModel().apply {
                    userId = user.uid
                    name = nameEditText?.text?.trim().toString()

                    try {
                        val phoneProto = phoneUtil.parse(phoneEditText?.text?.trim().toString(), Locale.getDefault().country)
                        phone = phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164)

                        barnName = barnNameEditText?.text?.trim().toString()

                        avatarUrl = HLGlobalData.me?.serviceProvider?.avatarUrl

                        val locationString = locationEditText?.text?.trim().toString()
                        val addressString = addressEditText?.text?.trim().toString()
                        if (locationString.isNotEmpty()) {
                            location = locationString
                        } else if (addressString.isNotEmpty()) {
                            location = addressString
                        }

                    } catch (e: NumberParseException) {
                        showError(e.localizedMessage)
                        hideProgressButton(createButton, stringResId = R.string.create_account)
                        return
                    }
                }
            } else {
                user.serviceProvider = HLServiceProviderModel().apply {
                    userId = user.uid
                    name = nameEditText?.text?.trim().toString()

                    try {
                        val phoneProto = phoneUtil.parse(phoneEditText?.text?.trim().toString(), Locale.getDefault().country)
                        phone = phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164)

                        avatarUrl = HLGlobalData.me?.horseManager?.avatarUrl

                        cloneAccount?.let {
                            account = it
                        }

                        val locationString = locationEditText?.text?.trim().toString()
                        val addressString = addressEditText?.text?.trim().toString()
                        if (locationString.isNotEmpty()) {
                            location = locationString
                        } else if (addressString.isNotEmpty()) {
                            location = addressString
                        }

                    } catch (e: NumberParseException) {
                        showError(e.localizedMessage)
                        hideProgressButton(createButton, stringResId = R.string.create_account)
                        return
                    }
                }
            }

            if (avatarUri != null) {
                HLFirebaseService.instance.uploadAvatar(user.uid, userType, avatarUri!!, object : ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        if (userType === HLUserType.MANAGER) {
                            user.horseManager?.avatarUrl = data
                        } else {
                            user.serviceProvider?.avatarUrl = data
                        }
                        avatarUri = null
                        createUserProfile()
                    }

                    override fun onFailure(error: String) {
                        hideProgressButton(createButton, stringResId = R.string.create_account)
                        showError(error)
                    }
                })
            } else {
                createUserProfile ()
            }
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        backButton = rootView?.findViewById(R.id.backImageView)
        profileImageView = rootView?.findViewById(R.id.profileImageView)
        nameEditText = rootView?.findViewById(R.id.nameEditText)
        phoneEditText = rootView?.findViewById(R.id.phoneEditText)
        locationEditText = rootView?.findViewById(R.id.locationEditText)
        addressEditText = rootView?.findViewById(R.id.addressEditText)
        termsTextView = rootView?.findViewById(R.id.termsTextView)
        createButton = rootView?.findViewById(R.id.createButton)

        backButton?.visibility = if (isHiddenBack) View.INVISIBLE else View.VISIBLE

        if (userType == HLUserType.MANAGER) {
            barnNameEditText = rootView?.findViewById(R.id.barnNameEditText)
        } else {
            addPaymentButton = rootView?.findViewById(R.id.addPaymentButton)
            bankImageView = rootView?.findViewById(R.id.bankImageView)
            bankNameTextView = rootView?.findViewById(R.id.bankNameTextView)
            serviceRecyclerView = rootView?.findViewById(R.id.existServiceRecyclerView)
            addServiceButton = rootView?.findViewById(R.id.addServiceButton)

            if (cloneAccount == null) {
                bankImageView?.visibility = View.GONE
                bankNameTextView?.text = getString(R.string.enable_invoicing)
            } else {
                setAccountData ()
            }

            serviceAdapter = HLEditProviderServiceAdapter(activity, object: HLProviderServiceItemListener {
                override fun onClickEdit(position: Int) {
                    onClickAddService(position)
                }

                override fun onClickDelete(position: Int) {
                    serviceAdapter?.remove(position)
                }
            })
            serviceRecyclerView?.recyclerView?.setHasFixedSize(false)
            serviceRecyclerView?.adapter = serviceAdapter
            serviceRecyclerView?.setLayoutManager(LinearLayoutManager(activity))
        }

        // bind progress button
        setProgressButton(createButton)

        // event handlers
        backButton?.setOnClickListener { popFragment() }
        profileImageView?.setOnClickListener { onClickProfileImage () }
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
        createButton?.setOnClickListener { onClickCreateAccount () }

        if (userType == HLUserType.PROVIDER) {
            addPaymentButton?.setOnClickListener { onClickAddBank () }
            addServiceButton?.setOnClickListener { onClickAddService () }
        }


        // initialize terms text view
        initTermsTextView ()
    }

    private fun displayCommonInfo() {
        if (HLUserType.MANAGER == userType && null != HLGlobalData.me?.serviceProvider) {
            val provider = HLGlobalData.me!!.serviceProvider!!
            profileImageView?.loadImage(provider.avatarUrl, R.drawable.ic_camera_alt)
            nameEditText?.setText(provider.name)
            phoneEditText?.setText(provider.phone)
            locationEditText?.setText(provider.location)
        } else if (HLUserType.PROVIDER == userType && null != HLGlobalData.me?.horseManager) {
            val manager = HLGlobalData.me!!.horseManager!!
            profileImageView?.loadImage(manager.avatarUrl, R.drawable.ic_camera_alt)
            nameEditText?.setText(manager.name)
            phoneEditText?.setText(manager.phone)
            locationEditText?.setText(manager.location)
        }
   }

    private fun initTermsTextView () {
        val termsString = getString(R.string.create_account_terms)

        val termsStart = termsString.indexOf("Terms &\nConditions")
        val termsEnd = termsStart + "Terms &\nConditions".length

        val privacyStart = termsString.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        val color = Color.WHITE

        val spannableString = SpannableString (termsString).apply {

            // clickable span
            setSpan(object: ClickableSpan() {
                override fun onClick(view: View) {
                    replaceFragment(HLWebViewFragment(HLGlobalData.settings.urls.terms))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            },
                termsStart,
                termsEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // terms

            setSpan(object: ClickableSpan() {
                override fun onClick(view: View) {
                    replaceFragment(HLWebViewFragment(HLGlobalData.settings.urls.privacy))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            },
                privacyStart,
                privacyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // privacy

            // color span
            setSpan(ForegroundColorSpan(color),
                termsStart,
                termsEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // terms

            setSpan(ForegroundColorSpan(color),
                privacyStart,
                privacyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // privacy
        }

        termsTextView?.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun setReactiveX () {
        nameEditText?.let {
            nameChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit1.MILLISECONDS)
        }

        phoneEditText?.let {
            phoneChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit1.MILLISECONDS)
        }

        /*if (userType == HLUserType.MANAGER) {
            barnNameEditText?.let {
                barnChangeObservable = RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit1.MILLISECONDS)
            }

            if (nameChangeObservable != null && phoneChangeObservable != null && barnChangeObservable != null) {
                disposable.add(
                    Observable.combineLatest(
                        nameChangeObservable,
                        phoneChangeObservable,
                        barnChangeObservable,
                        Function3<CharSequence, CharSequence, CharSequence, Array<CharSequence>> { name, phone, barn ->
                            arrayOf(name.trim(), phone.trim(), barn.trim())
                        }
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            val name = it[0]
                            val phone = it[1]
                            val barn = it[2]

                            val isEnabled = when {
                                name.isEmpty() -> {
                                    nameEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                    false
                                }
                                phone.isEmpty() -> {
                                    phoneEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                    false
                                }
                                barn.isEmpty() -> {
                                    barnNameEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                    false
                                }
                                else -> true
                            }
                            createButton?.isEnabled = isEnabled
                            createButton?.alpha = if (isEnabled) 1.0f else 0.2f
                        }
                )
            }
        } else {*/
            if (nameChangeObservable != null && phoneChangeObservable != null) {
                disposable.add(
                    Observable.combineLatest(
                        nameChangeObservable,
                        phoneChangeObservable,
                        BiFunction<CharSequence, CharSequence, Array<CharSequence>> { name, phone ->
                            arrayOf(name.trim(), phone.trim())
                        }
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            val name = it[0]
                            val phone = it[1]

                            val isEnabled = when {
                                name.isEmpty() -> {
                                    nameEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                    false
                                }
                                phone.isEmpty() -> {
                                    phoneEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                    false
                                }
                                else -> true
                            }
                            createButton?.isEnabled = isEnabled
                            createButton?.alpha = if (isEnabled) 1.0f else 0.2f
                        }
                )
            }
//        }

        addressEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit1.MILLISECONDS)
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
     *  Others
     */
    private fun getProviderServices () {
        cloneUser?.let { me ->
            showProgressDialog()
            HLFirebaseService.instance.getProviderServices(me.uid, object: ResponseCallback<List<HLServiceProviderServiceModel>> {
                override fun onSuccess(data: List<HLServiceProviderServiceModel>) {
                    hideProgressDialog()
                    
                    serviceAdapter?.clear()
                    serviceAdapter?.addAll(data)
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                }
            })
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
                    start(act, this@HLCreateProfileFragment)
                }
            }
        }
    }

    private fun createUserProfile () {
        cloneUser?.let { user ->
            user.type = userType

            // invite deep link
            HLGlobalData.deepLinkInvite?.let { deepLinkModel ->
                val invited = HLInvitedModel(deepLinkModel.senderId, deepLinkModel.senderType, deepLinkModel.platform)
                if (userType === HLUserType.MANAGER) {
                    user.horseManager?.invited = invited
                } else {
                    user.serviceProvider?.invited = invited
                }
            }

            // create user
            HLFirebaseService.instance.updateUser(user, callback = object: ResponseCallback<String> {
                override fun onSuccess(data: String) {

                    // save data
                    HLGlobalData.me = cloneUser

                    // transition
                    if (userType == HLUserType.MANAGER) {
                        replaceFragment(HLAddPaymentFragment(), addToBackStack = false)
                    } else {
                        addProviderServices ()
                    }
                }

                override fun onFailure(error: String) {
                    hideProgressButton(createButton, stringResId = R.string.create_account)
                    showError(error)
                }
            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAccountData () {
        bankImageView?.visibility = View.VISIBLE
        val extAccounts = cloneAccount?.externalAccounts ?: ArrayList()
        if (extAccounts.isNotEmpty()) {
            if (extAccounts[0].`object` == HLExternalAccountType.CARD) {
                bankImageView?.setImageResource(Card.getBrandIcon(extAccounts[0].brand))
                bankNameTextView?.text = "${extAccounts[0].brand} ${extAccounts[0].last4}"
            } else {
                bankImageView?.setImageResource(R.drawable.ic_default_bank)
                bankNameTextView?.text = "${extAccounts[0].bankName} ${extAccounts[0].last4}"
            }

        }
    }

    private fun addProviderServices () {
        val services = serviceAdapter?.allData
        if (services == null) {
            finishActivity()
        } else {
            HLFirebaseService.instance.addProviderServices(services, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    finishActivity()
                }

                override fun onFailure(error: String) {
                    hideProgressButton(createButton, stringResId = R.string.create_account)
                    showError(error)
                }
            })
        }
    }

    private fun finishActivity () {
        activity?.startActivity(Intent(activity, HLServiceProviderMainActivity::class.java))
        activity?.finish()
    }
}
