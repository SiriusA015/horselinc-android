package com.horselinc.views.fragments.provider


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLPhoneContactModel
import com.horselinc.models.data.HLProviderHorseModel
import com.horselinc.models.event.HLSearchEvent
import com.horselinc.utils.PermissionUtil
import com.horselinc.views.adapters.recyclerview.HLProviderContactHorseUserAdapter
import com.horselinc.views.adapters.recyclerview.HLSearchHorseAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLProviderHorseItemListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class HLProviderSearchHorseFragment (private val isCreateInvoice: Boolean = false) : HLBaseFragment() {

    /**
     * Controls
     */
    private var searchEditText: EditText? = null
    private var searchIconImageView: ImageView? = null
    private var createHorseButton: Button? = null
    private var contactTitleTextView: TextView? = null
    private var contactRecyclerView: EasyRecyclerView? = null
    private var horseRecyclerView: EasyRecyclerView? = null


    /**
     *  Permission Util
     */
    private lateinit var permissionUtil: PermissionUtil

    /**
     *  Variables
     */
    private lateinit var horseUserAdapter: HLProviderContactHorseUserAdapter
    private val phoneContacts = ArrayList<HLPhoneContactModel> ()
    private var isContactLoadMore = true

    private val phoneUtil = PhoneNumberUtil.getInstance()

    private lateinit var horseAdapter: HLSearchHorseAdapter
    private val disposable = CompositeDisposable ()
    private var isHorseLoadMore = true
    private var lastHorseId = ""

    private val scrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            hideKeyboard()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_search_horse, container, false)

            // event bus
            EventBus.getDefault().register(this)

            // initialize controls
            initControls ()

            // permission util
            permissionUtil = PermissionUtil(this)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                getContacts()
            } else {
                checkPermission()
            }
        }

        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        horseRecyclerView?.removeOnScrollListener(scrollListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return

        val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (PermissionUtil.PERMISSION_REQUEST_CODE == requestCode) {
            if (granted) {
                getContacts()
            } else {
                showInfoMessage("Until grant the permission, you cannot use this function")
            }
        }
    }


    /**
     * Initialize Handles
     */
    private fun initControls () {

        // controls
        searchEditText = rootView?.findViewById(R.id.etSearch)
        searchIconImageView = rootView?.findViewById(R.id.ivSearch)
        createHorseButton = rootView?.findViewById(R.id.createHorseButton)
        contactTitleTextView = rootView?.findViewById(R.id.contactTitleTextView)
        contactRecyclerView = rootView?.findViewById(R.id.contactRecyclerView)
        horseRecyclerView = rootView?.findViewById(R.id.horseRecyclerView)

        createHorseButton?.visibility = if (isCreateInvoice) View.VISIBLE else View.GONE
        contactTitleTextView?.visibility = View.INVISIBLE

        initHorses ()
        initContacts ()
        setupSearchEdit()

        // event handlers
        rootView?.findViewById<ImageView>(R.id.closeImageView)?.setOnClickListener { activity?.finish() }
        createHorseButton?.setOnClickListener { replaceFragment(HLProviderCreateHorseProfileFragment()) }
    }

    private fun setupSearchEdit() {
        searchEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { searchName ->
                        val searchKey = searchName.trim().toString()

                        horseAdapter.clear()
                        lastHorseId = ""

                        if (searchKey.length < 3) {
                            searchIconImageView?.visibility = View.VISIBLE
                        } else {
                            searchIconImageView?.visibility = View.GONE
                            searchHorse(searchKey)
                        }
                    }
            )
        }
    }

    private fun initHorses () {
        horseAdapter = HLSearchHorseAdapter(activity).apply {
            setMore(R.layout.load_more_horse, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (isHorseLoadMore && isNetworkConnected) {
                        searchHorse(searchEditText?.text.toString())
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() {}
            })

            setOnItemClickListener { position ->
                EventBus.getDefault().post(HLSearchEvent(horseAdapter.getItem(position)))
                activity?.finish()
            }
        }

        horseRecyclerView?.run {
            setLayoutManager(LinearLayoutManager(activity))
            adapter = horseAdapter
            visibility = View.GONE

            addOnScrollListener(scrollListener)
            recyclerView.setHasFixedSize(false)
        }
    }

    private fun initContacts () {
        horseUserAdapter = HLProviderContactHorseUserAdapter(activity, object: HLProviderHorseItemListener {
            override fun onClickHorse(position: Int, horse: HLHorseModel) {
                EventBus.getDefault().post(HLSearchEvent(horse))
                activity?.finish()
            }
        }).apply {

            clear()

            setMore(R.layout.load_more_horse, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (isContactLoadMore && isNetworkConnected) {
                        getContactHorseUsers ()
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() { }
            })
        }

        contactRecyclerView?.run {
            setLayoutManager(LinearLayoutManager(activity))
            adapter = horseUserAdapter
        }
    }


    /**
     * Search Horse Handler
     */
    private fun searchHorse(searchKey: String) {
        val uid = HLGlobalData.me?.uid ?: return

        HLFirebaseService.instance.searchHorsesForManager(uid, lastHorseId, null, object: ResponseCallback<List<HLHorseModel>> {
            override fun onSuccess(data: List<HLHorseModel>) {
                horseAdapter.addAll(data)

                isHorseLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSES
                if (data.isNotEmpty()) lastHorseId = data.last().uid

                horseRecyclerView?.visibility = if (horseAdapter.count == 0) View.GONE else View.VISIBLE
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        }, searchKey)
    }



    /**
     * Get Phone Contacts Handler
     */
    private fun checkPermission () {
        if (!permissionUtil.checkPermission(Manifest.permission.READ_CONTACTS)) {
            permissionUtil.requestPermission(Manifest.permission.READ_CONTACTS)
        } else {
            getContacts()
        }
    }

    private fun getContacts () {
        showProgressDialog()

        val cursor = activity?.contentResolver?.query(
            ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {

                val contact = HLPhoneContactModel()

                contact.id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                contact.name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                // get emails
                val curEmail = activity?.contentResolver?.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?", arrayOf(contact.id), null)

                if (curEmail != null && curEmail.count > 0) {
                    while (curEmail.moveToNext()) {
                        contact.emails.add(curEmail.getString(curEmail.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)))
                    }
                }

                curEmail?.close()

                // get phone numbers
                val phoneNumber = (cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))).toInt()
                if (phoneNumber > 0) {
                    val cursorPhone = activity?.contentResolver?.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(contact.id), null)

                    if (cursorPhone != null && cursorPhone.count > 0) {
                        while (cursorPhone.moveToNext()) {
                            try {
                                val contactPhoneNumber = cursorPhone.getString(cursorPhone.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER))
                                val phoneProto = phoneUtil.parse(contactPhoneNumber, Locale.getDefault().country)
                                contact.phoneNumbers.add(phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164))
                            } catch (e: NumberParseException) {
                                continue
                            }
                        }
                    }

                    cursorPhone?.close()
                }

                phoneContacts.add(contact)
            }
        } else {
            hideProgressDialog()
            showInfoMessage("No contacts available!")
        }

        cursor?.close()

        // get contact horse users
        if (phoneContacts.isNotEmpty()) {
            contactTitleTextView?.visibility = View.VISIBLE
            horseUserAdapter.clear()
            getContactHorseUsers()
        } else {
            hideProgressDialog()
            contactTitleTextView?.visibility = View.INVISIBLE
        }
    }

    private fun getContactHorseUsers () {
        val lastUserId = if (horseUserAdapter.count == 0) null else horseUserAdapter.allData.last().manager.userId

        val phoneNumbers = ArrayList<String>()
        val emails = ArrayList<String>()
        phoneContacts.forEach { contact ->
            val filteredPhones = contact.phoneNumbers.filter { phone ->
                !horseUserAdapter.allData.map { horseUser -> horseUser.manager }.map { manager -> manager.phone }.contains(phone)
            }
            contact.phoneNumbers.clear()
            contact.phoneNumbers.addAll(filteredPhones)
            phoneNumbers.addAll(filteredPhones)

            val filteredEmails = contact.emails.filter { email ->
                !horseUserAdapter.allData.map { horseUser -> horseUser.manager }.map { manager -> manager.email }.contains(email)
            }
            contact.emails.clear()
            contact.emails.addAll(filteredEmails)
            emails.addAll(filteredEmails)
        }

        HLFirebaseService.instance.searchContactUsers(phoneNumbers, emails, lastUserId, object:
            ResponseCallback<List<HLProviderHorseModel>> {
            override fun onSuccess(data: List<HLProviderHorseModel>) {
                hideProgressDialog()
                horseUserAdapter.addAll(data)
                isContactLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSES

                contactTitleTextView?.visibility = if (horseUserAdapter.count == 0) View.INVISIBLE else View.VISIBLE
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }
}
