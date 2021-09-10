package com.horselinc.views.fragments


import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.views.adapters.recyclerview.HLSpinnerDataAdapter
import com.horselinc.views.listeners.HLSpinnerDialogListener

class HLSpinnerDialogFragment (private var title: String = "",
                               private val data: List<Any>,
                               private var selectedIndex: Int = 0,
                               private val listener: HLSpinnerDialogListener) : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity!!)

        val view = LayoutInflater.from(context).inflate(R.layout.fragment_spinner_dialog, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.window?.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)

        // initialize controls
        view.findViewById<TextView>(R.id.titleTextView).text = title

        val negativeButton = view.findViewById<Button>(R.id.negativeButton)
        negativeButton.setOnClickListener { dismiss() }

        val positiveButton = view.findViewById<TextView>(R.id.positiveButton)
        positiveButton.setOnClickListener {
            listener.onClickPositive(selectedIndex, data[selectedIndex])
            dismiss()
        }

        val adapter = HLSpinnerDataAdapter(activity, selectedIndex)
        adapter.addAll(data)
        adapter.setOnItemClickListener {
            selectedIndex = it
            adapter.selectedPosition = it
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
//        recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        return dialog
    }

}
