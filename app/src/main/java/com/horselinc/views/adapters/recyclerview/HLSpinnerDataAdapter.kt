package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.widget.RadioButton
import com.horselinc.R
import com.horselinc.models.data.HLBaseUserModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter

class HLSpinnerDataAdapter(context: Context?, selectedPosition: Int) : RecyclerArrayAdapter<Any>(context) {

    var selectedPosition = selectedPosition
        set(value) {
            field = value
            notifyItemRangeChanged(0, count)
        }

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return SpinnerViewHolder(parent, R.layout.item_spinner)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)
        (holder as SpinnerViewHolder).radioButton.isChecked = position == selectedPosition
    }

    class SpinnerViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<Any>(parent, res) {

        var radioButton: RadioButton = itemView.findViewById(R.id.radioButton)

        override fun setData(data: Any?) {
            super.setData(data)
            radioButton.setOnClickListener { itemView.callOnClick() }

            data?.let {
                radioButton.text = when (it) {
                    is String -> it
                    is HLBaseUserModel -> if (it.userId.isEmpty()) "None" else it.name
                    else -> ""
                }
            }
        }
    }
}