package com.horselinc.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionUtil {

    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var context: Context

    constructor(activity: Activity) {
        this.activity = activity
        this.context = activity
    }

    constructor(fragment: Fragment) {
        this.fragment = fragment
        this.context = fragment.activity!!
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1989
    }

    fun checkPermission(permission: String): Boolean {
        return checkPermissions(arrayOf(permission))
    }

    fun checkPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            val result = ContextCompat.checkSelfPermission(context, permission)
            if (result != PackageManager.PERMISSION_GRANTED) return false
        }

        return true
    }

    fun requestPermission(permission: String) {
        requestPermissions(arrayOf(permission))
    }

    fun requestPermissions(permissions: Array<String>) {
        if (null != fragment) {
            fragment!!.requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            return
        }

        if (null != activity) {
            ActivityCompat.requestPermissions(activity!!, permissions, PERMISSION_REQUEST_CODE)
            return
        }
    }
}