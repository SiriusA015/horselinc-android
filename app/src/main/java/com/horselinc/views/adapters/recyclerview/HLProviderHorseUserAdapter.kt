package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLProviderHorseModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.listeners.HLProviderHorseItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.jude.easyrecyclerview.decoration.SpaceDecoration
import com.makeramen.roundedimageview.RoundedImageView


class HLProviderHorseUserAdapter(context: Context?,
                                 private val itemListener: HLProviderHorseItemListener
) : RecyclerArrayAdapter<HLProviderHorseModel>(context) {

    private val selectedUsers = ArrayList<HLHorseManagerModel?>()

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLProviderHorseUserViewHolder (parent, R.layout.item_provider_horse_user)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {

        (holder as HLProviderHorseUserViewHolder).isSelected = selectedUsers.any { it?.userId == getItem(position).manager.userId }

        super.OnBindViewHolder(holder, position)
    }

    fun addSelectedUser (selectedUser: HLHorseManagerModel) {
        selectedUsers.add(selectedUser)
    }

    fun removeSelectedUsers () {
        selectedUsers.clear()
    }

    class HLProviderHorseUserViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLProviderHorseModel>(parent, res) {

        var isSelected: Boolean = false

        private val managerContainer: ConstraintLayout = itemView.findViewById(R.id.managerContainer)
        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.profileImageView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val arrowImageView: ImageView = itemView.findViewById(R.id.arrowImageView)
        private val horseRecyclerView: RecyclerView = itemView.findViewById(R.id.horseRecyclerView)

        private var providerHorse: HLProviderHorseModel? = null
        private var horseAdapter: HLProviderHorseAdapter

        init {
            managerContainer.setOnClickListener { onClickManagerContainer() }

            horseAdapter = HLProviderHorseAdapter(context).apply {
                setOnItemClickListener {
                    onClickHorse (getItem(it))
                }
            }

            horseRecyclerView.run {
                adapter = horseAdapter
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SpaceDecoration(ResourceUtil.dpToPx(8)))
            }
        }

        override fun setData(data: HLProviderHorseModel?) {
            providerHorse = data

            // set manager
            profileImageView.loadImage(data?.manager?.avatarUrl, R.drawable.ic_profile)
            usernameTextView.text = data?.manager?.name

            // set selected
            if (isSelected) {
                arrowImageView.rotation = 90f
                horseAdapter.clear()
                horseAdapter.addAll(data?.horses)
            } else {
                arrowImageView.rotation = 0f
                horseAdapter.clear()
            }
        }

        /**
         *  Event Handlers
         */
        private fun onClickManagerContainer () {

            isSelected = !isSelected
            if (isSelected) {
                arrowImageView.rotation = 90f
                horseAdapter.clear()
                horseAdapter.addAll(providerHorse?.horses)
            } else {
                arrowImageView.rotation = 0f
                horseAdapter.clear()
            }

            getOwnerAdapter<HLProviderHorseUserAdapter>()?.run {
                if (isSelected) {
                    if (selectedUsers.none { it?.userId == providerHorse?.manager?.userId}) {
                        selectedUsers.add(providerHorse?.manager)
                    }
                } else {
                    val index = selectedUsers.indexOfFirst { it?.userId == providerHorse?.manager?.userId }
                    if (index >= 0) {
                        selectedUsers.removeAt(index)
                    }
                }
            }
        }

        private fun onClickHorse (horse: HLHorseModel) {
            getOwnerAdapter<HLProviderHorseUserAdapter>()?.run {
                itemListener.onClickHorse(dataPosition, horse)
            }
        }
    }
}