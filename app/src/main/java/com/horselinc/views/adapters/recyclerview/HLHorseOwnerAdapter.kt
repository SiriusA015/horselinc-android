package com.horselinc.views.adapters.recyclerview

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseOwnerModel
import com.horselinc.views.listeners.HLHorseOwnerItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLHorseOwnerAdapter(context: Context?,
                          private val itemListener: HLHorseOwnerItemListener) : RecyclerArrayAdapter<HLHorseOwnerModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLHorseOwnerViewHolder (parent, R.layout.item_horse_owner)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as HLHorseOwnerViewHolder).apply {
            itemView.isClickable = false
            setSelectable (allData[position].userId.isEmpty())
        }
    }

    class HLHorseOwnerViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseOwnerModel>(parent, res) {

        private val ownerImageView: RoundedImageView = itemView.findViewById(R.id.horseOwnerImageView)
        private val ownerTextView: TextView = itemView.findViewById(R.id.horseOwnerTextView)
        private val percentEditText: EditText = itemView.findViewById(R.id.percentEditText)
        private val percentTextView: TextView = itemView.findViewById(R.id.percentTextView)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteImageView)

        init {
            ownerTextView.setOnClickListener { itemView.callOnClick() }
            deleteButton.setOnClickListener { onClickDelete () }

            percentEditText.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    onChangePercentage ()
                }
            })
        }

        override fun setData(data: HLHorseOwnerModel?) {
            data?.let { owner ->
                // profile image
                ownerImageView.loadImage(owner.avatarUrl, R.drawable.ic_profile)

                // name
                ownerTextView.text = owner.name

                // percent
                if (owner.percentage > 0.0f) {
                    percentEditText.setText(String.format("%.02f", owner.percentage))
                }
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLHorseOwnerAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_delete_horse_owner)
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("Yes") { dialog, _ ->
                        itemListener.onClickDelete(dataPosition)
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        private fun onChangePercentage () {
            getOwnerAdapter<HLHorseOwnerAdapter>()?.run {
                val percentage = percentEditText.text.trim().toString()
                itemListener.onChangePercentage(dataPosition, if (percentage.isEmpty()) 0.0f else percentage.toFloat())
            }
        }

        fun setSelectable (isSelectable: Boolean) {
            if (isSelectable) {
                ownerTextView.isClickable = true
                deleteButton.visibility = View.INVISIBLE
                percentEditText.visibility = View.INVISIBLE
                percentTextView.visibility = View.INVISIBLE
            } else {
                ownerTextView.isClickable = false
                deleteButton.visibility = View.VISIBLE
                percentEditText.visibility = View.VISIBLE
                percentTextView.visibility = View.VISIBLE
                percentEditText.isEnabled = true
            }
        }
    }
}