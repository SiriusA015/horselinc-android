package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.*
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.listeners.HLManagerServiceRequestItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView
import kotlinx.android.synthetic.main.item_manager_service_request.view.*
import java.util.*


class HLManagerServiceRequestAdapter(context: Context?,
                                     private val itemListener: HLManagerServiceRequestItemListener) : RecyclerArrayAdapter<HLServiceRequestModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLManagerServiceRequestViewHolder (parent, R.layout.item_manager_service_request)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as HLManagerServiceRequestViewHolder).apply {
            if (position == 0) {
                itemView.dateTextView.visibility = View.VISIBLE
            } else {
                val prevDate = allData[position - 1].getRequestDateCalendar()
                val currentDate = allData[position].getRequestDateCalendar()
                itemView.dateTextView.visibility = if (prevDate.isSameDay(currentDate)) View.GONE else View.VISIBLE
            }
        }
    }


    /**
     *  View Holders
     */
    class HLManagerServiceRequestViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLServiceRequestModel>(parent, res) {

        private var dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private var showNameTextView: TextView = itemView.findViewById(R.id.showNameTextView)
        private var optionButton: ImageView = itemView.findViewById(R.id.optionImageView)
        private var competitionClassTextView: TextView = itemView.findViewById(R.id.competitionClassTextView)
        private var providerImageView: RoundedImageView = itemView.findViewById(R.id.providerImageView)
        private var providerNameTextView: TextView = itemView.findViewById(R.id.providerNameTextView)
        private var servicesTextView: TextView = itemView.findViewById(R.id.servicesTextView)
        private var statusMarkView: View = itemView.findViewById(R.id.statusMarkView)
        private var statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private var chooseProviderButton: Button = itemView.findViewById(R.id.chooseProviderButton)

        private var request: HLServiceRequestModel? = null

        init {
            optionButton.setOnClickListener { onClickOption () }
            providerImageView.setOnClickListener { onClickProvider () }
            chooseProviderButton.setOnClickListener { onClickChooseProvider () }

            dateTextView.text = ""
            showNameTextView.text = ""
            competitionClassTextView.text = ""
            providerNameTextView.text = ""
            servicesTextView.text = ""
            statusTextView.text = ""
        }

        @SuppressLint("SetTextI18n", "DefaultLocale")
        override fun setData(data: HLServiceRequestModel?) {
            data?.let { request ->
                this.request = request

                // set request date
                dateTextView.text = request.getRequestDateCalendar().time.formattedString("EEE, MMMM d, YYYY")

                // option button
                if (request.status == HLServiceRequestStatus.PENDING
                    || request.status == HLServiceRequestStatus.ACCEPTED
                    || request.status == HLServiceRequestStatus.DECLINED) {
                    optionButton.visibility = View.VISIBLE
                } else {
                    optionButton.visibility = View.GONE
                }

                // set show name
                showNameTextView.text = (request.show?.name ?: "Non-Show Request").toUpperCase()

                // set class name
                val className = request.competitionClass ?: ""
                competitionClassTextView.visibility = if (className.isEmpty()) View.GONE else View.VISIBLE
                if (request.status == HLServiceRequestStatus.COMPLETED) {
                    competitionClassTextView.text = request.competitionClass + "\nPayment Requested"
                } else {
                    competitionClassTextView.text = request.competitionClass
                }

                // set provider image
                providerImageView.loadImage(request.serviceProvider?.avatarUrl, R.drawable.ic_profile)

                // set provider name
                providerNameTextView.text = request.serviceProvider?.name

                // set services
                val services = ArrayList<String>()
                request.services.forEach {
                    services.add("${it.service} (x${it.quantity})")
                }
                servicesTextView.text = services.joinToString()

                // set status
                statusMarkView.visibility = View.VISIBLE
                statusTextView.visibility = View.VISIBLE
                chooseProviderButton.visibility = View.GONE

                when (request.status) {
                    HLServiceRequestStatus.COMPLETED -> {
                        val color = ResourceUtil.getColor(R.color.colorServiceRequestCompleted)
                        statusMarkView.setBackgroundColor(color)
                        statusTextView.setTextColor(color)
                        statusTextView.text = "Completed"
                    }
                    HLServiceRequestStatus.PENDING -> {
                        val color = ResourceUtil.getColor(R.color.colorServiceRequestPending)
                        statusMarkView.setBackgroundColor(color)
                        statusTextView.setTextColor(color)
                        statusTextView.text = "Pending"
                    }
                    HLServiceRequestStatus.ACCEPTED -> {
                        val color = ResourceUtil.getColor(R.color.colorServiceRequestAccepted)
                        statusMarkView.setBackgroundColor(color)
                        statusTextView.setTextColor(color)
                        statusTextView.text = "Accepted"
                    }
                    HLServiceRequestStatus.DECLINED -> {
                        val color = ResourceUtil.getColor(R.color.colorServiceRequestDeclined)
                        statusMarkView.setBackgroundColor(color)
                        statusTextView.setTextColor(color)
                        statusTextView.text = "DECLINED"
                        chooseProviderButton.visibility = View.VISIBLE
                    }
                    HLServiceRequestStatus.PAID -> {
                        val color = ResourceUtil.getColor(R.color.colorServiceRequestCompleted)
                        statusMarkView.setBackgroundColor(color)
                        statusTextView.setTextColor(color)
                        statusTextView.text = "Paid"
                    }
                    HLServiceRequestStatus.INVOICED -> {
                        val color = ResourceUtil.getColor(R.color.colorServiceRequestCompleted)
                        statusMarkView.setBackgroundColor(color)
                        statusTextView.setTextColor(color)
                        statusTextView.text = "Invoiced"
                    }
                }
            }
        }

        /**
         * Event Handlers
         */
        private fun onClickOption () {
            getOwnerAdapter<HLManagerServiceRequestAdapter>()?.run {
                itemListener.onClickOption(request, dataPosition - 1)
            }
        }

        private fun onClickProvider () {
            getOwnerAdapter<HLManagerServiceRequestAdapter>()?.run {
                itemListener.onClickProvider(request?.serviceProvider)
            }
        }

        private fun onClickChooseProvider () {
            getOwnerAdapter<HLManagerServiceRequestAdapter>()?.run {
                itemListener.onClickChooseProvider(request, dataPosition - 1)
            }
        }
    }
}