package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLHorseOwnerModel
import kotlinx.android.synthetic.main.item_horse_owner_name.view.*


class HLHorseOwnerNameAdapter(private var context: Context?
                              , private var owners: List<HLHorseOwnerModel>) : RecyclerView.Adapter<HLHorseOwnerNameAdapter.HLHorseOwnerNameViewHolder>() {

    override fun onBindViewHolder(holder: HLHorseOwnerNameViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HLHorseOwnerNameViewHolder {
        var v = LayoutInflater.from(context).inflate(R.layout.item_horse_owner_name, parent, false)
        return HLHorseOwnerNameViewHolder(v)
    }

    override fun getItemCount() = owners.size

    inner class HLHorseOwnerNameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindData(position: Int) {
            val owner = owners[position]

            itemView.tvName.text = owner.name
        }

    }
}