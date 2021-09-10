package com.horselinc.views.adapters.recyclerview

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import com.horselinc.R
import com.horselinc.models.data.HLHorseRegistrationModel
import com.horselinc.views.listeners.HLHorseRegistrationItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter


class HLHorseRegistrationAdapter(context: Context?,
                                 private val itemListener: HLHorseRegistrationItemListener) : RecyclerArrayAdapter<HLHorseRegistrationModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLHorseRegistrationViewHolder (parent, R.layout.item_horse_registration)
    }

    class HLHorseRegistrationViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseRegistrationModel>(parent, res) {

        private val nameEditText: EditText = itemView.findViewById(R.id.nameEditText)
        private val numberEditText: EditText = itemView.findViewById(R.id.numberEditText)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteImageView)

        init {
            nameEditText.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    onChangeName ()
                }
            })
            numberEditText.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    onChangeNumber ()
                }
            })
            deleteButton.setOnClickListener { onClickDelete() }
        }

        override fun setData(data: HLHorseRegistrationModel?) {
            data?.let { registration ->
                nameEditText.setText(registration.name)
                numberEditText.setText(registration.number)
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLHorseRegistrationAdapter>()?.run {
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

        private fun onChangeName () {
            getOwnerAdapter<HLHorseRegistrationAdapter>()?.run {
                val name = nameEditText.text.trim().toString()
                for (index in allData.indices) {
                    if (index != dataPosition && allData[index].name == name) {
                        nameEditText.error = "Exist name"
                    }
                }
                itemListener.onChangeName(dataPosition, name)
            }
        }

        private fun onChangeNumber () {
            getOwnerAdapter<HLHorseRegistrationAdapter>()?.run {
                itemListener.onChangeNumber(dataPosition, numberEditText.text.trim().toString())
            }
        }
    }
}