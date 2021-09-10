package com.horselinc.views.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.horselinc.IntentExtraKey
import com.horselinc.R
import com.horselinc.views.fragments.role.HLCreateProfileFragment
import com.horselinc.views.fragments.role.HLSelectRoleFragment

class HLUserRoleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_role)

        val userType = intent.getStringExtra(IntentExtraKey.USER_ROLE_TYPE)
        val fragment = userType?.let {
            HLCreateProfileFragment (it, true)
        } ?: HLSelectRoleFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.roleContainer, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
