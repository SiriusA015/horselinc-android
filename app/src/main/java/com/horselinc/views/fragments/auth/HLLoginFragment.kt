package com.horselinc.views.fragments.auth


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.biometric.BiometricPrompt
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLUserModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.activities.HLHorseManagerMainActivity
import com.horselinc.views.activities.HLServiceProviderMainActivity
import com.horselinc.views.activities.HLUserRoleActivity
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.webservices.WebServiceAPI
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class HLLoginFragment : HLBaseFragment() {

    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var loginButton: Button? = null
    private var forgotPasswordButton: Button? = null
    private var signUpButton: Button? = null
    private var faceIdImageView: ImageView? = null
    private var touchIdImageView: ImageView? = null

    private var emailChangeObservable: Observable<CharSequence>? = null
    private var passwordChangeObservable: Observable<CharSequence>? = null
    private val disposable = CompositeDisposable()

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_login, container, false)

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

    /**
     *  Others
     */
    private fun initControls () {
        // variables
        emailEditText = rootView?.findViewById(R.id.emailEditText)
        passwordEditText = rootView?.findViewById(R.id.passwordEditText)
        loginButton = rootView?.findViewById(R.id.loginButton)
        forgotPasswordButton = rootView?.findViewById(R.id.forgotPasswordButton)
        signUpButton = rootView?.findViewById(R.id.signUpButton)
        faceIdImageView = rootView?.findViewById(R.id.faceIdImageView)
        touchIdImageView = rootView?.findViewById(R.id.touchIdImageView)

        // bind progress buttons
        setProgressButton(loginButton)

        val email = App.preference.get(PreferenceKey.USER_EMAIL, "")
        val password = App.preference.get(PreferenceKey.USER_PASSWORD, "")
        touchIdImageView?.visibility = if (email.isNotEmpty()
            && password.isNotEmpty()
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) == true) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // event handlers
        loginButton?.setOnClickListener { onClickLogin () }
        forgotPasswordButton?.setOnClickListener { replaceFragment(HLForgotPasswordFragment()) }
        signUpButton?.setOnClickListener { onClickSignUp () }

        touchIdImageView?.setOnClickListener { onClickTouchId () }
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
                        loginButton?.isEnabled = isEnabled
                        loginButton?.alpha = if (isEnabled) 1.0f else 0.2f
                    }
            )
        }
    }

    private fun onClickTouchId () {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle("Fingerprint Authentication")
            .setDescription("Please touch your fingerprint sensor to authenticate.")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(activity!!, executor, object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {

                } else {
                    activity?.runOnUiThread {
                        showErrorMessage(errString.toString())
                    }
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                activity?.runOnUiThread {
                    loginButton?.alpha = 1.0f
                    onClickLogin(true)
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                activity?.runOnUiThread {
                    showErrorMessage("Fingerprint authentication was failed.")
                }
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }


    private fun onClickLogin (isFingerPrint: Boolean = false) {
        hideKeyboard()

        val email = if (isFingerPrint) App.preference.get(PreferenceKey.USER_EMAIL, "") else emailEditText?.text.toString().trim()
        val password = if (isFingerPrint) App.preference.get(PreferenceKey.USER_PASSWORD, "") else passwordEditText?.text.toString().trim()

        showProgressButton(loginButton)
        HLFirebaseService.instance.login(email, password, object: ResponseCallback<HLUserModel> {
            override fun onSuccess(data: HLUserModel) {

                hideProgressButton(loginButton, stringResId = R.string.log_in)

                HLGlobalData.me = data

                App.preference.put(PreferenceKey.USER_EMAIL, email)
                App.preference.put(PreferenceKey.USER_PASSWORD, password)

                // check next activity
                val cls = when (data.type) {
                    null -> HLUserRoleActivity::class.java
                    HLUserType.MANAGER -> HLHorseManagerMainActivity::class.java
                    else -> HLServiceProviderMainActivity::class.java
                }
                activity?.startActivity(Intent(activity, cls))
                activity?.finish()
            }

            override fun onFailure(error: String) {
                /*hideProgressButton(loginButton, stringResId = R.string.log_in)
                showError(error)*/
                signInOldServer (email, password)
            }
        })
    }

    private fun onClickSignUp () {
        val email = emailEditText?.text?.trim().toString()
        val password = passwordEditText?.text?.toString()

        replaceFragment(HLSignUpFragment(email, password))
    }

    /**
     *  SignIn Old Server Handler to update exist users' password
     */
    private fun signInOldServer (email: String, password: String) {
        WebServiceAPI.signIn(email, password, object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                signInFirebase (email, password)
            }

            override fun onFailure(error: String) {
                hideProgressButton(loginButton, stringResId = R.string.log_in)
                showError(error)
            }
        })
    }

    private fun signInFirebase (email: String, password: String) {
        HLFirebaseService.instance.login(email, HLConstants.FIREBASE_DEFAULT_PASSWORD, object: ResponseCallback<HLUserModel> {
            override fun onSuccess(data: HLUserModel) {

                // update firebase password
                HLFirebaseService.instance.changePassword(password, object : ResponseCallback<String> {
                })

                // sign in success
                hideProgressButton(loginButton, stringResId = R.string.log_in)

                HLGlobalData.me = data

                App.preference.put(PreferenceKey.USER_EMAIL, email)
                App.preference.put(PreferenceKey.USER_PASSWORD, password)

                // check next activity
                val cls = when (data.type) {
                    null -> HLUserRoleActivity::class.java
                    HLUserType.MANAGER -> HLHorseManagerMainActivity::class.java
                    else -> HLServiceProviderMainActivity::class.java
                }
                activity?.startActivity(Intent(activity, cls))
                activity?.finish()
            }

            override fun onFailure(error: String) {
                hideProgressButton(loginButton, stringResId = R.string.log_in)
                showError(error)
            }
        })
    }

}
