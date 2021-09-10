package com.horselinc.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.horselinc.R


object DialogUtil {
    @SuppressLint("InflateParams")
    fun showProgressDialog(context: Context) : AlertDialog? {
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.dlg_progress, null)
            val dlg = AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .show()

            dlg.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            return dlg
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}