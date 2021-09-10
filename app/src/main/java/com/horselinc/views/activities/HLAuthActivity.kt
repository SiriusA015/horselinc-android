package com.horselinc.views.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.views.fragments.auth.HLLoginFragment
import com.horselinc.views.fragments.auth.HLSignUpFragment

class HLAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        supportFragmentManager.beginTransaction()
            .replace(R.id.authContainer, HLGlobalData.deepLinkInvite?.let { HLSignUpFragment() } ?: HLLoginFragment())
            .commit()
    }
}
