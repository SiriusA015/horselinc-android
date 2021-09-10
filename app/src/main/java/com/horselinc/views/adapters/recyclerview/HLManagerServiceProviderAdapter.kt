package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.models.data.HLHorseManagerProviderModel
import com.horselinc.views.listeners.HLEditBaseUserItemListener
import com.horselinc.views.listeners.HLManagerServiceProviderItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter


class HLManagerServiceProviderAdapter(context: Context?,
                                      private val itemListener: HLManagerServiceProviderItemListener? = null
) : RecyclerArrayAdapter<List<HLHorseManagerProviderModel>>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLManagerServiceProviderViewHolder (parent, R.layout.item_manager_service_provider)
    }

    class HLManagerServiceProviderViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<List<HLHorseManagerProviderModel>>(parent, res) {

        private val serviceTypeTextView: TextView = itemView.findViewById(R.id.serviceTypeTextView)
        private val providerRecyclerView: RecyclerView = itemView.findViewById(R.id.providerRecyclerView)
        private var providerAdapter: HLEditBaseUserAdapter

        init {
            providerAdapter = HLEditBaseUserAdapter(context, itemListener = object: HLEditBaseUserItemListener {
                override fun onClickDelete(position: Int, user: HLBaseUserModel) {
                    getOwnerAdapter<HLManagerServiceProviderAdapter>()?.run {
                        itemListener?.onClickDelete(dataPosition, user as HLHorseManagerProviderModel)
                    }
                }
            })

            providerRecyclerView.run {
                adapter = providerAdapter
                layoutManager = LinearLayoutManager(context)
            }
        }

        override fun setData(data: List<HLHorseManagerProviderModel>?) {
            data?.let { providers ->

                // service type
                serviceTypeTextView.text = if (providers.isNotEmpty()) {
                    providers.first().serviceType
                } else {
                    ""
                }

                // providers
                providerAdapter.clear()
                providerAdapter.addAll(providers)
            }
        }
    }
}