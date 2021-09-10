package com.horselinc.views.fragments.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.views.fragments.HLBaseFragment

/**
 *  Created by TengFei Li on 26, August, 2019
 */

class HLChangeEmailFragment : HLBaseFragment() {

    private var etEmail: EditText? = null
    private var btSave: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_change_email, container, false)

            initControls()
        }

        return rootView
    }

    private fun initControls() {
        // controls
        etEmail = rootView?.findViewById(R.id.etEmail)
        btSave = rootView?.findViewById(R.id.btSave)

        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btBack)?.setOnClickListener { popFragment() }
        btSave?.setOnClickListener { onSave() }

        etEmail?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!Patterns.EMAIL_ADDRESS.matcher(etEmail?.text?.trim().toString()).matches()) {
                    btSave?.isEnabled = false
                    btSave?.alpha = 0.2f
                } else {
                    btSave?.isEnabled = true
                    btSave?.alpha = 1.0f
                }
            }
        })
    }

    @SuppressLint("InflateParams")
    private fun showReAuthDialog() {
        val view = layoutInflater.inflate(R.layout.layout_reauth, null)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)

        val dialog = AlertDialog.Builder(activity!!)
            .setTitle(R.string.app_name)
            .setView(view)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            (it as AlertDialog).apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val email = etEmail.text.trim().toString()
                    val password = etPassword.text.toString()

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        showErrorMessage("Please, input valid email address")
                        return@setOnClickListener
                    }
                    if (password.isBlank()) {
                        showErrorMessage("Please, input password")
                        return@setOnClickListener
                    }

                    showProgressDialog()
                    HLFirebaseService.instance.reauthenticate(email, password, object : ResponseCallback<Boolean> {
                        override fun onSuccess(data: Boolean) {
                            hideProgressDialog()
                            dialog.dismiss()
                            onSave()
                        }

                        override fun onFailure(error: String) {
                            hideProgressDialog()
                            showErrorMessage(error)
                        }
                    })
                }
                getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { dialog.dismiss() }
            }
        }
        dialog.show()
    }


    fun onSave() {
        showProgressButton(btSave)

        HLGlobalData.me?.copy()?.let { cloned ->
            cloned.email = etEmail?.text?.trim().toString()

            HLFirebaseService.instance.updateUser(cloned, true, object : ResponseCallback<String> {
                override fun onSuccess(data: String) {
                    hideProgressButton(btSave, stringResId = R.string.save_changes)
                    showSuccessMessage("Your email has been updated!")

                    HLGlobalData.me = cloned

                    popFragment()
                }

                override fun onFailure(error: String) {
                    hideProgressButton(btSave, stringResId = R.string.save_changes)

                    if ("FirebaseAuthRecentLoginRequiredException" == error) {
                        showReAuthDialog()
                    } else {
                        showErrorMessage(error)
                    }
                }
            })
        }
    }

}