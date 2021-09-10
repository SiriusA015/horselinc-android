package com.horselinc.views.fragments.auth


import android.content.Intent
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
import com.horselinc.models.data.HLUserModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.activities.HLUserRoleActivity
import com.horselinc.views.fragments.HLBaseFragment
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

class HLSignUpFragment(private val email: String? = null,
                       private val password: String? = null) : HLBaseFragment() {

    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var signUpButton: Button? = null
    private var loginInsteadButton: Button? = null

    private var emailChangeObservable: Observable<CharSequence>? = null
    private var passwordChangeObservable: Observable<CharSequence>? = null
    private val disposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_sign_up, container, false)

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
        passwordEditText = rootView?.findViewById(R.id.passwordEditText)
        signUpButton = rootView?.findViewById(R.id.signUpButton)
        loginInsteadButton = rootView?.findViewById(R.id.loginInsteadButton)

        // initialize email and password
        emailEditText?.setText(email)
        passwordEditText?.setText(password)

        // sign up button enabled
        val isEnabled = (email?.isNotEmpty() == true && password?.isNotEmpty() == true)
        signUpButton?.isEnabled = isEnabled
        signUpButton?.alpha = if (isEnabled) 1.0f else 0.2f


        // bind progress button
        setProgressButton(signUpButton)

        // event handlers
        signUpButton?.setOnClickListener { onClickSignUp () }
        loginInsteadButton?.setOnClickListener { popFragment() }
    }

    private fun setReactiveX () {
        emailEditText?.let {
            emailChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        passwordEditText?.let {
            passwordChangeObservable = RxTextView.textChanges(it)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        if (emailChangeObservable != null && passwordChangeObservable != null) {
            disposable.add(
                Observable.combineLatest(
                    emailChangeObservable,
                    passwordChangeObservable,
                    BiFunction<CharSequence, CharSequence, Array<CharSequence>> { email, password ->
                        arrayOf(email.trim(), password)
                    }
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        val email = it[0]
                        val password = it[1]
                        val isEnabled = when {
                            email.isEmpty() -> {
                                emailEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                false
                            }
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                emailEditText?.error = "${ResourceUtil.getString(R.string.msg_err_invalid)} email address"
                                false
                            }
                            password.isEmpty() -> {
                                passwordEditText?.error = ResourceUtil.getString(R.string.msg_err_required)
                                false
                            }
                            else -> true
                        }
                        signUpButton?.isEnabled = isEnabled
                        signUpButton?.alpha = if (isEnabled) 1.0f else 0.2f
                    }
            )
        }
    }

    private fun onClickSignUp () {
        hideKeyboard()

        val email = emailEditText?.text.toString().trim()
        val password = passwordEditText?.text.toString().trim()

        showProgressButton(signUpButton)
        HLFirebaseService.instance.signUp(email, password, object: ResponseCallback<HLUserModel> {
            override fun onSuccess(data: HLUserModel) {

                hideProgressButton(signUpButton, stringResId = R.string.sign_up)

                HLGlobalData.me = data

                val intent = Intent(activity, HLUserRoleActivity::class.java).apply {
                    HLGlobalData.deepLinkInvite?.let {
                        it.invoiceId ?.let {
                            putExtra(IntentExtraKey.USER_ROLE_TYPE, HLUserType.MANAGER)
                        } ?: putExtra(IntentExtraKey.USER_ROLE_TYPE, HLUserType.PROVIDER)
                    }
                }
                activity?.startActivity(intent)
                activity?.finish()
            }

            override fun onFailure(error: String) {
                hideProgressButton(signUpButton, stringResId = R.string.sign_up)
                showError(error)
            }
        })
    }
}
