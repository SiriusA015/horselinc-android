package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.models.data.HLServiceShowModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter


class HLSearchServiceShowAdapter(context: Context?) : RecyclerArrayAdapter<HLServiceShowModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLSearchServiceShowViewHolder (parent, R.layout.item_search_service_show)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as HLSearchServiceShowViewHolder).apply {
            itemView.isClickable = true
        }
    }

    class HLSearchServiceShowViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLServiceShowModel>(parent, res) {

        private var tvName: TextView = itemView.findViewById(R.id.tvName)

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLServiceShowModel?) {
            data?.let { show ->
                tvName.text = show.name
            }
        }
    }
}