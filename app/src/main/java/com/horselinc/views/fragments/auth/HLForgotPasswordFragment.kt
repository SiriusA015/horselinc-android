package com.horselinc.views.fragments.auth


import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.fragments.HLBaseFragment
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class HLForgotPasswordFragment : HLBaseFragment() {

    private var emailEditText: EditText? = null
    private var sendEmailButton: Button? = null
    private var loginInsteadButton: Button? = null

    private val disposable = CompositeDisposable ()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_forgot_password, container, false)

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

    private fun initControls () {
        // variables
        emailEditText = rootView?.findViewById(R.id.emailEditText)
        sendEmailButton = rootView?.findViewById(R.id.sendEmailButton)
        loginInsteadButton = rootView?.findViewById(R.id.loginInsteadButton)

        // bind progress button
        setProgressButton(sendEmailButton)

        // event handlers
        sendEmailButton?.setOnClickListener { onClickSendEmail () }
        loginInsteadButton?.setOnClickListener { popFragment () }

        emailEditText?.setOnEditorActionListener { _, _, _ ->
            onClickSendEmail()
            true
        }
    }

    private fun setReactiveX () {
        emailEditText?.let {
            disposable.add(
                RxTextView.textChanges(it)
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { email ->
                        val isEnabled = when {
                            email.trim().isEmpty() -> {
                                emailEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                false
                            }
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                emailEditText?.error = "${ResourceUtil.getString(R.string.msg_err_invalid)} email address"
                                false
                            }
                            else -> true
                        }
                        sendEmailButton?.isEnabled = isEnabled
                        sendEmailButton?.alpha = if (isEnabled) 1.0f else 0.2f
                    }
            )
        }
    }

    private fun onClickSendEmail () {
        hideKeyboard()

        val email = emailEditText?.text.toString().trim()
        showProgressButton(sendEmailButton)
        HLFirebaseService.instance.forgotPassword(email, object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                hideProgressButton(sendEmailButton, false, R.string.email_instructions)
                showSuccessMessage(data)
            }

            override fun onFailure(error: String) {
                hideProgressButton(sendEmailButton, stringResId = R.string.email_instructions)
                showError(error)
            }
        })
    }
}
