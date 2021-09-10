package com.horselinc.views.fragments.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.fragments.HLBaseFragment
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import java.util.concurrent.TimeUnit

/**
 *  Created by TengFei Li on 26, August, 2019
 */

class HLChangePasswordFragment : HLBaseFragment() {

    private var etOldPwd: EditText? = null
    private var etNewPwd: EditText? = null
    private var etConfirmPwd: EditText? = null
    private var btSave: Button? = null

    private val disposable = CompositeDisposable()
    private var oldChangeObservable: Observable<CharSequence>? = null
    private var newChangeObservable: Observable<CharSequence>? = null
    private var confirmChangeObservable: Observable<CharSequence>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {

            rootView = inflater.inflate(R.layout.fragment_change_password, container, false)

            initControls()
        }

        return rootView
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    private fun initControls() {
        // controls
        etOldPwd = rootView?.findViewById(R.id.etOldPwd)
        etNewPwd = rootView?.findViewById(R.id.etNewPwd)
        etConfirmPwd = rootView?.findViewById(R.id.etConfirmPwd)
        btSave = rootView?.findViewById(R.id.btSave)

        // reactive x
        setReactiveX()


        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btBack)?.setOnClickListener {  popFragment() }
        btSave?.setOnClickListener { onSave() }
    }

    private fun setReactiveX () {

        etOldPwd?.let {
            oldChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        etNewPwd?.let {
            newChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        etConfirmPwd?.let {
            confirmChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        if (oldChangeObservable != null && newChangeObservable != null && confirmChangeObservable != null) {
            disposable.add(
                Observable.combineLatest(
                    oldChangeObservable,
                    newChangeObservable,
                    confirmChangeObservable,

                    Function3<CharSequence, CharSequence, CharSequence, Array<CharSequence>> { oldPwd, newPwd, confirmPwd ->
                        arrayOf(oldPwd, newPwd, confirmPwd)
                    }
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        val oldPwd = it[0]
                        val newPwd = it[1]
                        val confirmPwd = it[2]

                        val isEnabled = when {
                            oldPwd.isEmpty() -> {
                                etOldPwd?.error = ResourceUtil.getString(R.string.msg_err_required)
                                false
                            }
                            newPwd.isEmpty() -> {
                                etNewPwd?.error = ResourceUtil.getString(R.string.msg_err_required)
                                false
                            }
                            confirmPwd.isEmpty() -> {
                                etConfirmPwd?.error = ResourceUtil.getString(R.string.msg_err_required)
                                false
                            }
                            newPwd.toString() != confirmPwd.toString() -> {
                                etConfirmPwd?.error = ResourceUtil.getString(R.string.msg_confirm_password_not_match)
                                false
                            }
                            else -> true
                        }
                        btSave?.isEnabled = isEnabled
                        btSave?.alpha = if (isEnabled) 1.0f else 0.2f
                    }
            )
        }
    }

    private fun updatePassword() {
        HLFirebaseService.instance.changePassword(etNewPwd?.text.toString(), object : ResponseCallback<String> {
            override fun onSuccess(data: String) {
                showSuccessMessage("Your password has been updated!")
                popFragment()
            }

            override fun onFailure(error: String) {
                hideProgressButton(btSave, stringResId = R.string.save_changes)
                showErrorMessage(error)
            }
        })
    }

    fun onSave() {
        showProgressButton(btSave)

        HLFirebaseService.instance.reauthenticate(etOldPwd?.text.toString(), object : ResponseCallback<Boolean> {
            override fun onSuccess(data: Boolean) {
                updatePassword()
            }

            override fun onFailure(error: String) {
                hideProgressButton(btSave, stringResId = R.string.save_changes)
                showErrorMessage("Please input the old password correctly")
            }
        })
    }

}