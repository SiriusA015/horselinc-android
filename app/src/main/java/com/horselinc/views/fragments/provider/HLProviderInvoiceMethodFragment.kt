package com.horselinc.views.fragments.provider


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.horselinc.HLInvoiceMethodType
import com.horselinc.R
import com.horselinc.hideKeyboard
import com.horselinc.models.data.HLPhoneContactModel
import com.horselinc.showInfoMessage
import com.horselinc.utils.PermissionUtil
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLInvoiceContactAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLSelectInvoiceContactItemListener
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jude.easyrecyclerview.decoration.DividerDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class HLProviderInvoiceMethodFragment(private val invoiceMethod: HLInvoiceMethodType,
                                      private val listener: HLSelectInvoiceContactItemListener) : HLBaseFragment() {

    /**
     *  Controls
     */
    private var searchEditText: EditText? = null
    private var contactRecyclerView: RecyclerView? = null


    /**
     *  Variables
     */
    private val phoneContacts = ArrayList<HLPhoneContactModel>()
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private val disposable = CompositeDisposable ()

    private val scrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            hideKeyboard()
        }
    }

    private lateinit var contactAdapter: HLInvoiceContactAdapter


    /**
     *  Permission Util
     */
    private lateinit var permissionUtil: PermissionUtil



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_invoice_method, container, false)

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
        contactRecyclerView?.removeOnScrollListener(scrollListener)
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
     *  Event Handlers
     */
    private fun onClickAdd () {
        searchEditText?.text?.toString()?.let { searchKey ->

            if (searchKey.isEmpty()) {
                showInfoMessage("Please enter search text.")
            } else {
                if (invoiceMethod == HLInvoiceMethodType.EMAIL) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(searchKey).matches()) {
                        showInfoMessage("Invalid email address.")
                    } else {
                        listener.onClickInvoiceContact(HLInvoiceMethodType.EMAIL, HLPhoneContactModel(emails = arrayListOf(searchKey)))
                        popFragment()
                    }
                } else if (invoiceMethod == HLInvoiceMethodType.SMS) {
                    try {
                        val phoneProto = phoneUtil.parse(searchKey, Locale.getDefault().country)
                        phoneUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164)
                        listener.onClickInvoiceContact(HLInvoiceMethodType.SMS, HLPhoneContactModel(phoneNumbers = arrayListOf(searchKey)))
                        popFragment()
                    } catch (e: NumberParseException) {
                        showInfoMessage("Invalid phone number.")
                        return
                    }
                }
            }

        } ?: showInfoMessage("Please enter search text.")
    }



    /**
     *  Initialize Handlers
     */
    private fun initControls () {

        // controls
        searchEditText = rootView?.findViewById(R.id.searchEditText)
        contactRecyclerView = rootView?.findViewById(R.id.contactRecyclerView)

        // adapter
        contactAdapter = HLInvoiceContactAdapter(activity).apply {
            clear()
            setOnItemClickListener { position ->
                listener.onClickInvoiceContact(invoiceMethod, getItem(position))
                popFragment()
            }
        }

        contactRecyclerView?.run {
            adapter = contactAdapter
            layoutManager = LinearLayoutManager(activity)

            val decoration = DividerDecoration(
                Color.parseColor("#1e000000"),
                ResourceUtil.dpToPx(1), ResourceUtil.dpToPx(16), 0)
            decoration.setDrawLastItem(false)
            addItemDecoration(decoration)

            addOnScrollListener(scrollListener)
        }

        // set reactive x
        setupReactiveX ()

        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<Button>(R.id.addButton)?.setOnClickListener { onClickAdd () }
    }

    @SuppressLint("DefaultLocale")
    private fun setupReactiveX () {
        searchEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { search ->
                        val searchKey = search.toString().toLowerCase()

                        contactAdapter.clear()

                        if (searchKey.isNotEmpty()) {
                            searchContact (searchKey)
                        }
                    }
            )
        }
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
                        contact.emails.add(curEmail.getString(curEmail.getColumnIndex(
                            ContactsContract.CommonDataKinds.Email.DATA)))
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
            showInfoMessage("No contacts available!")
        }

        hideProgressDialog()

        cursor?.close()
    }


    /**
     *  Search Contact Handler
     */
    @SuppressLint("DefaultLocale")
    private fun searchContact (searchKey: String) {
        if (invoiceMethod == HLInvoiceMethodType.EMAIL) {
            phoneContacts.forEach { originalContact ->
                originalContact.emails.forEach { email ->
                    if (originalContact.name.toLowerCase().contains(searchKey) || email.toLowerCase().contains(searchKey)) {
                        contactAdapter.add(HLPhoneContactModel(
                            originalContact.id,
                            originalContact.name,
                            originalContact.photoUri,
                            arrayListOf(email)
                        ))
                    }
                }
            }
        } else if (invoiceMethod == HLInvoiceMethodType.SMS) {
            phoneContacts.forEach { originalContact ->
                originalContact.phoneNumbers.forEach { phone ->
                    if (originalContact.name.toLowerCase().contains(searchKey) || phone.contains(searchKey)) {
                        contactAdapter.add(HLPhoneContactModel(
                            originalContact.id,
                            originalContact.name,
                            originalContact.photoUri,
                            phoneNumbers = arrayListOf(phone)
                        ))
                    }
                }
            }
        }
    }
}
