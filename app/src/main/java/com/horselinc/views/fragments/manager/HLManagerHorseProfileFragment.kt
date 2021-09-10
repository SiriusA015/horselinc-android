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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLHorseOwnerModel
import com.horselinc.models.data.HLHorseRegistrationModel
import com.horselinc.models.event.HLRefreshHorsesEvent
import com.horselinc.models.event.HLSelectBaseUserEvent
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.StorageUtil
import com.horselinc.views.activities.HLSearchUserActivity
import com.horselinc.views.adapters.recyclerview.HLHorseOwnerAdapter
import com.horselinc.views.adapters.recyclerview.HLHorseRegistrationAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.HLSelectYearDialogFragment
import com.horselinc.views.fragments.HLSpinnerDialogFragment
import com.horselinc.views.listeners.*
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jude.easyrecyclerview.EasyRecyclerView
import com.makeramen.roundedimageview.RoundedImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.concurrent.TimeUnit

class HLManagerHorseProfileFragment : HLBaseFragment() {

    /**
     *  Controls
     */
    private var backButton: ImageView? = null
    private var titleTextView: TextView? = null

    private var horseProfileImageView: RoundedImageView? = null
    private var horseBarnNameEditText: EditText? = null
    private var horseNameEditText: EditText? = null
    private var horseGenderTextView: TextView? = null
    private var horseBirthYearTextView: TextView? = null

    private var horseDescEditText: EditText? = null

    private var horseTrainerLayout: ConstraintLayout? = null
    private var horseTrainerImageView: RoundedImageView? = null
    private var horseTrainerTextView: TextView? = null

    private var horseOwnerRecyclerView: EasyRecyclerView? = null
    private var horseOwnerErrorTextView: TextView? = null
    private var addHorseOwnerButton: Button? = null

    private var horseLeasedToLayout: ConstraintLayout? = null
    private var horseLeasedToImageView: RoundedImageView? = null
    private var horseLeasedToTextView: TextView? = null

    private var horseRegistrationRecyclerView: EasyRecyclerView? = null
    private var addRegistrationButton: Button? = null

    private var horseSireEditText: EditText? = null
    private var horseDamEditText: EditText? = null
    private var horseColorEditText: EditText? = null
    private var horseHeightEditText: EditText? = null

    private var deleteButton: Button? = null
    private var saveButton: Button? = null

    /**
     *  Adapters
     */
    private lateinit var ownerAdapter: HLHorseOwnerAdapter
    private lateinit var registrationAdapter: HLHorseRegistrationAdapter

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
    private var selectedTrainer: HLHorseManagerModel? = null
        set(value) {
            field = value
            setTrainerData()
        }
    private var selectedLeasedTo: HLHorseManagerModel? = null
        set(value) {
            field = value
            setLeasedToData()
        }
    private var selectedOwnerIndex: Int = -1
    private var updateOwnerIds: ArrayList<String> = ArrayList()

    /**
     *  Observables
     */
    /*private var observableChangeBarnName: Observable<CharSequence>? = null
    private var observableChangeName: Observable<CharSequence>? = null
    private var observableChangeGender: Observable<CharSequence>? = null*/

    private val disposable = CompositeDisposable ()

    companion object {
        fun new (selectedHorse: HLHorseModel): HLManagerHorseProfileFragment {
            return HLManagerHorseProfileFragment ().apply {
                cloneHorse = selectedHorse
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // root view
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_horse_profile, container, false)

            permissionUtil = PermissionUtil(this)

            EventBus.getDefault().register(this)

            // initialize controls
            initControls ()

            // set reactive x
            setReactiveX ()

            // setting data
            setHorseData ()
        }

        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
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
     *  Event Bus Handler
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    fun onReceivedSelectUser (event: HLSelectBaseUserEvent) {
        try {
            when (event.selectType) {
                HLBaseUserSelectType.HORSE_TRAINER -> {
                    selectedTrainer = event.baseUser as HLHorseManagerModel
                }
                HLBaseUserSelectType.HORSE_OWNER -> {
                    val updateOwner = HLHorseOwnerModel().apply {
                        update (event.baseUser as HLHorseManagerModel)
                    }
                    ownerAdapter.update(updateOwner, selectedOwnerIndex)
                }
                HLBaseUserSelectType.HORSE_LEASED_TO -> {
                    selectedLeasedTo = event.baseUser as HLHorseManagerModel
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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

    private fun onClickSearch (selectType: Int) {
        hideKeyboard()

        // build exclude users
        val excludeUsers = ArrayList<String>()

        HLGlobalData.me?.let {
            excludeUsers.add(it.uid)
        }

        selectedTrainer?.let {
            excludeUsers.add(it.userId)
        }

        selectedLeasedTo?.let {
            excludeUsers.add(it.userId)
        }

        if (ownerAdapter.count > 0) {
            excludeUsers.addAll(ownerAdapter.allData.map { it.userId })
        }

        val intent = Intent (activity, HLSearchUserActivity::class.java)
        intent.putExtra(IntentExtraKey.BASE_USER_SELECT_TYPE, selectType)
        intent.putStringArrayListExtra(IntentExtraKey.BASE_USER_EXCLUDE_IDS, excludeUsers)
        startActivity(intent)
    }

    private fun onClickDelete () {
        hideKeyboard()

        activity?.let { act ->
            android.app.AlertDialog.Builder(act)
                .setTitle(act.getString(R.string.app_name))
                .setMessage(R.string.msg_alert_delete_horse)
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Yes") { dialog, _ ->
                    deleteHorse ()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun onClickSave () {
        hideKeyboard()

        saveButton?.isEnabled = false

        val barnName = horseBarnNameEditText?.text?.trim().toString()
        val name = horseNameEditText?.text?.trim().toString()
        val gender = horseGenderTextView?.text?.trim().toString()
        val birthYear = horseBirthYearTextView?.text?.trim().toString()

        if (cloneHorse == null) {
            cloneHorse = HLHorseModel().apply {
                this.barnName = barnName
                displayName = name
                this.gender = gender

                if (birthYear.isNotEmpty()) {
                    this.birthYear = birthYear.toInt()
                }


                // trainer
                selectedTrainer?.let {  trainer ->
                    this.trainerId = trainer.userId
                }

                // owners
                ownerAdapter.allData?.let { owners ->
                    this.owners = ArrayList ()
                    val filteredOwners = owners.filter { it.name.isNotEmpty() && it.percentage > 0.0f }
                    this.owners?.addAll(filteredOwners)
                    this.ownerIds = ArrayList()
                    this.ownerIds?.addAll(filteredOwners.map { it.userId })
                }

                // creator id
                HLGlobalData.me?.let { me ->
                    creatorId = me.uid
                }

                // leaser id
                selectedLeasedTo?.let { leaser ->
                    leaserId = leaser.userId
                }

                // note
                description = horseDescEditText?.text?.trim().toString()

                // color
                color = horseColorEditText?.text?.trim().toString()

                // sire
                sire = horseSireEditText?.text?.trim().toString()

                // dam
                dam = horseDamEditText?.text?.trim().toString()

                // height
                val heightString = horseHeightEditText?.text?.trim().toString()
                if (heightString.isNotEmpty()) {
                    height = heightString.toDouble()
                }

                // registrations
                registrationAdapter.allData?.let { registrations ->
                    this.registrations = ArrayList ()
                    this.registrations?.addAll(registrations.filter { it.name.isNotEmpty() && it.number.isNotEmpty() })
                }
            }
            addHorse ()
        } else {
            cloneHorse?.run {
                this.barnName = barnName
                displayName = name
                this.gender = gender

                if (birthYear.isNotEmpty()) {
                    this.birthYear = birthYear.toInt()
                }

                // trainer
                selectedTrainer?.let { trainer ->
                    this.trainerId = trainer.userId
                }

                // owners
                ownerAdapter.allData?.let { owners ->
                    this.owners = ArrayList ()
                    val filteredOwners = owners.filter { it.name.isNotEmpty() && it.percentage > 0.0f }
                    this.owners?.addAll(filteredOwners)
                    this.ownerIds = ArrayList()
                    this.ownerIds?.addAll(filteredOwners.map { it.userId })
                }

                // leaser id
                selectedLeasedTo?.let { leaser ->
                    leaserId = leaser.userId
                }

                // note
                description = horseDescEditText?.text?.trim().toString()

                // color
                color = horseColorEditText?.text?.trim().toString()

                // sire
                sire = horseSireEditText?.text?.trim().toString()

                // dam
                dam = horseDamEditText?.text?.trim().toString()

                // height
                val heightString = horseHeightEditText?.text?.trim().toString()
                if (heightString.isNotEmpty()) {
                    height = heightString.toDouble()
                }

                // registrations
                registrationAdapter.allData?.let { registrations ->
                    this.registrations = ArrayList ()
                    this.registrations?.addAll(registrations.filter { it.name.isNotEmpty() && it.number.isNotEmpty() })
                }
            }
            updateHorse ()
        }
    }

    private fun onClickCreateHorseOwner () {
        val createHorseOwnerListener = object: HLCreateHorseOwnerListener {
            override fun onCreatedNewHorseOwner(horseManager: HLHorseManagerModel) {
                val newOwner = HLHorseOwnerModel()
                newOwner.update(horseManager)
                ownerAdapter.add(newOwner)
            }
        }

        replaceFragment(HLManagerCreateHorseOwnerFragment(createHorseOwnerListener))
    }


    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        backButton = rootView?.findViewById(R.id.backImageView)
        titleTextView = rootView?.findViewById(R.id.titleTextView)

        horseProfileImageView = rootView?.findViewById(R.id.horseProfileImageView)
        horseBarnNameEditText = rootView?.findViewById(R.id.horseBarnNameEditText)
        horseNameEditText = rootView?.findViewById(R.id.horseShowNameEditText)
        horseGenderTextView = rootView?.findViewById(R.id.horseGenderTextView)
        horseBirthYearTextView = rootView?.findViewById(R.id.horseBirthYearTextView)

        horseDescEditText = rootView?.findViewById(R.id.horseDescEditText)

        horseTrainerLayout = rootView?.findViewById(R.id.horseTrainerLayout)
        horseTrainerImageView = rootView?.findViewById(R.id.horseTrainerImageView)
        horseTrainerTextView = rootView?.findViewById(R.id.horseTrainerTextView)

        horseOwnerRecyclerView = rootView?.findViewById(R.id.horseOwnerRecyclerView)
        horseOwnerErrorTextView = rootView?.findViewById(R.id.horseOwnerErrorTextView)
        addHorseOwnerButton = rootView?.findViewById(R.id.addHorseOwnerButton)

        horseLeasedToLayout = rootView?.findViewById(R.id.horseLeasedToLayout)
        horseLeasedToImageView = rootView?.findViewById(R.id.horseLeasedToImageView)
        horseLeasedToTextView = rootView?.findViewById(R.id.horseLeasedToTextView)

        horseRegistrationRecyclerView = rootView?.findViewById(R.id.horseRegistrationRecyclerView)
        addRegistrationButton = rootView?.findViewById(R.id.addRegistrationButton)

        horseSireEditText = rootView?.findViewById(R.id.horseSireEditText)
        horseDamEditText = rootView?.findViewById(R.id.horseDamEditText)
        horseColorEditText = rootView?.findViewById(R.id.horseColorEditText)
        horseHeightEditText = rootView?.findViewById(R.id.horseHeightEditText)

        deleteButton = rootView?.findViewById(R.id.deleteButton)
        saveButton = rootView?.findViewById(R.id.saveButton)

        // initialize recycler views
        ownerAdapter = HLHorseOwnerAdapter(activity, object: HLHorseOwnerItemListener {
            override fun onChangePercentage(position: Int, percentage: Float) {
                // update percentage
                val owner = ownerAdapter.getItem(position)
                owner.percentage = percentage
                setOwnerError ()

                // update owner ids
                if (owner.uid.isNotEmpty() && !updateOwnerIds.contains(owner.uid)) {
                    updateOwnerIds.add(owner.uid)
                }
            }

            override fun onClickDelete(position: Int) {
                // update owner id
                val ownerUId = ownerAdapter.getItem(position).uid
                if (ownerUId.isNotEmpty()) {
                    updateOwnerIds.add(ownerUId)
                }

                // remove onwer
                ownerAdapter.remove(position)
                if (ownerAdapter.allData.isEmpty()) {
                    ownerAdapter.add(HLHorseOwnerModel())
                }
                setOwnerError()
            }
        }).apply {
            setOnItemClickListener { position ->
                selectedOwnerIndex = position
                onClickSearch (HLBaseUserSelectType.HORSE_OWNER)
            }
        }
        horseOwnerRecyclerView?.adapter = ownerAdapter
        horseOwnerRecyclerView?.setLayoutManager(LinearLayoutManager(activity))
        horseOwnerRecyclerView?.recyclerView?.setHasFixedSize(false)

        registrationAdapter = HLHorseRegistrationAdapter(activity, object: HLHorseRegistrationItemListener {
            override fun onChangeName(position: Int, name: String) {
                registrationAdapter.getItem(position).name = name
            }

            override fun onChangeNumber(position: Int, number: String) {
                registrationAdapter.getItem(position).number = number
            }

            override fun onClickDelete(position: Int) {
                registrationAdapter.remove(position)
                if (registrationAdapter.allData.isEmpty()) {
                    registrationAdapter.add(HLHorseRegistrationModel())
                }
            }
        })
        horseRegistrationRecyclerView?.adapter = registrationAdapter
        horseRegistrationRecyclerView?.setLayoutManager(LinearLayoutManager(activity))
        horseRegistrationRecyclerView?.recyclerView?.setHasFixedSize(false)


        // event handler
        backButton?.setOnClickListener { popFragment() }

        horseProfileImageView?.setOnClickListener { onClickProfileImage () }

        horseGenderTextView?.setOnClickListener { onClickGender () }
        horseBirthYearTextView?.setOnClickListener { onClickBirthYear () }

        horseTrainerLayout?.setOnClickListener { onClickSearch (HLBaseUserSelectType.HORSE_TRAINER) }

        addHorseOwnerButton?.setOnClickListener { ownerAdapter.add(HLHorseOwnerModel()) }

        rootView?.findViewById<Button>(R.id.createHorseOwnerButton)?.setOnClickListener { onClickCreateHorseOwner() }

        horseLeasedToLayout?.setOnClickListener { onClickSearch(HLBaseUserSelectType.HORSE_LEASED_TO) }

        addRegistrationButton?.setOnClickListener { registrationAdapter.add(HLHorseRegistrationModel()) }

        deleteButton?.setOnClickListener { onClickDelete () }
        saveButton?.setOnClickListener { onClickSave () }
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

                    saveButton?.isEnabled = isEnabled
                    saveButton?.alpha = if (isEnabled) 1.0f else 0.2f
                })
        }

        /*horseBarnNameEditText?.let {
            observableChangeBarnName = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        horseNameEditText?.let {
            observableChangeName = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        horseGenderTextView?.let {
            observableChangeGender = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        if (observableChangeBarnName != null
            && observableChangeName != null
            && observableChangeGender != null) {

            disposable.add (
                Observable.combineLatest(
                    observableChangeBarnName,
                    observableChangeName,
                    observableChangeGender,
                    Function3<CharSequence, CharSequence, CharSequence, Array<String>> { barn, name, gender ->
                        arrayOf(barn.trim().toString(), name.trim().toString(), gender.trim().toString())
                    }
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { aryString ->
                        val barnName = aryString[0]
                        val name = aryString[1]
                        val gender = aryString[2]

                        val isEnabled = when {
                            barnName.isEmpty() -> {
                                horseBarnNameEditText?.error = getString(R.string.msg_err_required)
                                false
                            }
                            name.isEmpty() -> {
                                horseNameEditText?.error = getString(R.string.msg_err_required)
                                false
                            }
                            gender.isEmpty() -> {
                                horseGenderTextView?.error = getString(R.string.msg_err_required)
                                false
                            }
                            else -> true
                        }

                        saveButton?.isEnabled = isEnabled
                        saveButton?.alpha = if (isEnabled) 1.0f else 0.2f
                    }
            )
        }*/
    }

    private fun setHorseData () {
        if (cloneHorse == null) { // create new horse profile

            // title
            titleTextView?.text = getString(R.string.create_horse_profile)

            // trainer information
            selectedTrainer?:let {
                selectedTrainer = HLGlobalData.me?.horseManager
            }

            // owner information
            ownerAdapter.add(HLHorseOwnerModel())

            // registration information
            registrationAdapter.add(HLHorseRegistrationModel())

            // hide delete button
            deleteButton?.visibility = View.GONE

            // save button
            saveButton?.isEnabled = false
            saveButton?.alpha = 0.2f

        } else { // edit horse profile
            titleTextView?.text = getString(R.string.edit_horse_profile)

            // profile image
            cloneHorse?.avatarUrl?.let { avatar ->
                if (avatar.isNotEmpty()) {
                    Picasso.get()
                        .load(avatar)
                        .into(horseProfileImageView, object: Callback {
                            override fun onSuccess() {
                                horseProfileImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
                            }

                            override fun onError(e: java.lang.Exception?) { }
                        })
                }
            }

            // barn name
            horseBarnNameEditText?.setText(cloneHorse?.barnName)

            // show name
            horseNameEditText?.setText(cloneHorse?.displayName)

            // gender
            horseGenderTextView?.text = HLConstants.HORSE_GENDERS.firstOrNull { it == cloneHorse?.gender }

            // birth year
            horseBirthYearTextView?.text = cloneHorse?.birthYear?.toString()

            // note
            horseDescEditText?.setText(cloneHorse?.description)

            // trainer information
            selectedTrainer = cloneHorse?.trainer

            // owner information
            val owners = cloneHorse?.owners ?: ArrayList ()
            ownerAdapter.clear()
            if (owners.isEmpty()) {
                ownerAdapter.add(HLHorseOwnerModel())
            } else {
                ownerAdapter.addAll(owners)
            }

            setOwnerError()

            // leased to
            selectedLeasedTo = cloneHorse?.leaser
            setLeasedToData()

            // registration
            val registrations = cloneHorse?.registrations ?: ArrayList ()
            registrationAdapter.clear()
            if (registrations.isEmpty()) {
                registrationAdapter.add(HLHorseRegistrationModel())
            } else {
                registrationAdapter.addAll(registrations)
            }

            // sire
            horseSireEditText?.setText(cloneHorse?.sire)

            // dam
            horseDamEditText?.setText(cloneHorse?.dam)

            // color
            horseColorEditText?.setText(cloneHorse?.color)

            // height
            horseHeightEditText?.setText(cloneHorse?.height?.toString())

            // delete button
            deleteButton?.visibility = View.VISIBLE

            // save button
            saveButton?.isEnabled = true
            saveButton?.alpha = 1.0f
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
                    start(act, this@HLManagerHorseProfileFragment)
                }
            }
        }
    }

    /**
     *  Trainer Handler
     */
    private fun setTrainerData () {
        horseTrainerImageView?.loadImage(selectedTrainer?.avatarUrl, R.drawable.ic_profile)
        horseTrainerTextView?.text = selectedTrainer?.name
    }

    /**
     *  Owner Handler
     */
    private fun setOwnerError () {
        val totalPercentage = ownerAdapter.allData.map { it.percentage }.sum()
        horseOwnerErrorTextView?.visibility = if (totalPercentage != 100.0f) View.VISIBLE else View.GONE
    }

    /**
     *  Leased To Handler
     */
    private fun setLeasedToData () {
        horseLeasedToImageView?.loadImage(selectedLeasedTo?.avatarUrl, R.drawable.ic_profile)
        horseLeasedToTextView?.text = selectedLeasedTo?.name
    }

    /**
     *  Horse Handlers
     */
    private fun addHorse () {
        cloneHorse?.let { horse ->
            showProgressDialog()
            HLFirebaseService.instance.addHorse(horse, avatarUri, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    finishHorseProfile()
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                    cloneHorse = null
                    saveButton?.isEnabled = true
                }
            })
        }
    }

    private fun updateHorse () {
        cloneHorse?.let { horse ->
            showProgressDialog()
            HLFirebaseService.instance.updateHorse(horse, avatarUri, updateOwnerIds, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    finishHorseProfile()
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                    saveButton?.isEnabled = true
                }
            })
        }
    }

    private fun deleteHorse () {
        cloneHorse?.let { horse ->
            showProgressDialog()
            HLFirebaseService.instance.deleteHorse(horse.uid, object: ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    finishHorseProfile()
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                }
            })
        }
    }

    /**
     *  Finish Handler
     */
    private fun finishHorseProfile () {
        hideProgressDialog()
        EventBus.getDefault().post(HLRefreshHorsesEvent())
        popFragment()
    }
}
