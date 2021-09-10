package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.widget.RadioButton
import com.horselinc.R
import com.horselinc.models.data.HLHorseManagerModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter

class HLSpinnerHorseUserAdapter(context: Context?, selectedPosition: Int) : RecyclerArrayAdapter<HLHorseManagerModel>(context) {

    var selectedPosition = selectedPosition
        set(value) {
            field = value
            notifyItemRangeChanged(0, count)
        }

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HorseUserSpinnerViewHolder(parent, R.layout.item_spinner)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)
        (holder as HorseUserSpinnerViewHolder).radioButton.isChecked = position == selectedPosition
    }

    class HorseUserSpinnerViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseManagerModel>(parent, res) {

        var radioButton: RadioButton = itemView.findViewById(R.id.radioButton)

        init {
            radioButton.setOnClickListener { onClickRadio ()  }
        }

        override fun setData(data: HLHorseManagerModel?) {
            super.setData(data)

            data?.let {
                radioButton.text = if (it.userId.isEmpty()) "None" else it.name
            }
        }

        private fun onClickRadio () {
            getOwnerAdapter<HLSpinnerHorseUserAdapter>()?.run {
                selectedPosition = dataPosition
                itemView.callOnClick()
            }
        }
    }
}