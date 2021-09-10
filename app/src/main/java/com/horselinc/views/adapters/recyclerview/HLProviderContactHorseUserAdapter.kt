package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLProviderHorseModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.listeners.HLProviderHorseItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.jude.easyrecyclerview.decoration.DividerDecoration
import com.makeramen.roundedimageview.RoundedImageView


class HLProviderContactHorseUserAdapter(context: Context?,
                                        private val itemListener: HLProviderHorseItemListener
) : RecyclerArrayAdapter<HLProviderHorseModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLProviderContactHorseUserViewHolder (parent, R.layout.item_provider_contact_horse_user)
    }

    class HLProviderContactHorseUserViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLProviderHorseModel>(parent, res) {

        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.profileImageView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val horseRecyclerView: RecyclerView = itemView.findViewById(R.id.horseRecyclerView)

        private var providerHorse: HLProviderHorseModel? = null
        private var horseAdapter: HLProviderContactHorseAdapter

        init {
            horseAdapter = HLProviderContactHorseAdapter(context).apply {
                setOnItemClickListener {
                    onClickHorse (getItem(it))
                }
            }

            horseRecyclerView.run {
                adapter = horseAdapter
                layoutManager = LinearLayoutManager(context)

                val decoration = DividerDecoration(Color.parseColor("#1e000000"),
                    ResourceUtil.dpToPx(1), ResourceUtil.dpToPx(16), 0)
                decoration.setDrawLastItem(false)
                addItemDecoration(decoration)
            }
        }

        override fun setData(data: HLProviderHorseModel?) {
            providerHorse = data

            // set manager
            profileImageView.loadImage(data?.manager?.avatarUrl, R.drawable.ic_profile)
            usernameTextView.text = data?.manager?.name

            // set horse data
            horseAdapter.clear()
            horseAdapter.addAll(providerHorse?.horses)
            if (horseAdapter.count == 0) {
                horseRecyclerView.setBackgroundColor(Color.parseColor("#f1f1f1"))
            } else {
                horseRecyclerView.setBackgroundColor(Color.WHITE)
            }
        }

        /**
         *  Event Handlers
         */
        private fun onClickHorse (horse: HLHorseModel) {
            getOwnerAdapter<HLProviderContactHorseUserAdapter>()?.run {
                itemListener.onClickHorse(dataPosition, horse)
            }
        }
    }
}