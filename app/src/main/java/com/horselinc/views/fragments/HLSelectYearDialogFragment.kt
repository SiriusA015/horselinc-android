package com.horselinc.views.fragments


import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.horselinc.R
import com.horselinc.views.listeners.HLSelectYearDialogListener
import java.util.*


class HLSelectYearDialogFragment (private val selectedYear: Int = -1,
                                  private val listener: HLSelectYearDialogListener) : DialogFragment() {

    private lateinit var numberPicker: NumberPicker

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity!!)

        val view = LayoutInflater.from(context).inflate(R.layout.fragment_select_year_dialog, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // initialize controls
        numberPicker = view.findViewById<NumberPicker>(R.id.numberPicker).apply {
            val calendar = Calendar.getInstance()
            maxValue = calendar.get(Calendar.YEAR)
            minValue = maxValue - 100
            wrapSelectorWheel = false
            value = if (selectedYear == -1) maxValue else selectedYear
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        view.findViewById<Button>(R.id.negativeButton).setOnClickListener { dismiss() }
        view.findViewById<TextView>(R.id.positiveButton).setOnClickListener {
            listener.onClickPositive(numberPicker.value)
            dismiss()
        }

        return dialog
    }

}
