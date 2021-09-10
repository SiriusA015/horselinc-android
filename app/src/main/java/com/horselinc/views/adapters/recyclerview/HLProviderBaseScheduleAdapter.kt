package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.*
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.listeners.HLProviderServiceRequestItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView
import java.util.*


open class HLProviderBaseScheduleAdapter(context: Context?,
                                         private val shouldExtend: Boolean = false,
                                         private val itemListener: HLProviderServiceRequestItemListener
) : RecyclerArrayAdapter<HLServiceRequestModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {

        return if (shouldExtend) {
            HLProviderExtendScheduleViewHolder (parent, R.layout.item_provider_extend_schedule)
        } else {
            HLProviderBaseScheduleViewHolder (parent, R.layout.item_provider_base_schedule)
        }
    }

    open class HLProviderBaseScheduleViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLServiceRequestModel>(parent, res) {

        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val horseImageView: RoundedImageView = itemView.findViewById(R.id.horseImageView)
        private val horseBarnNameTextView: TextView = itemView.findViewById(R.id.horseBarnNameTextView)
        private val horseDisplayNameTextView: TextView = itemView.findViewById(R.id.horseDisplayNameTextView)
        val moreImageView: ImageView = itemView.findViewById(R.id.moreImageView)
        private val requestShowTextView: TextView = itemView.findViewById(R.id.requestShowTextView)
        private val requestClassTextView: TextView = itemView.findViewById(R.id.requestClassTextView)
        private val requestServicesTextView: TextView = itemView.findViewById(R.id.requestServicesTextView)
        private val requestNoteTextView: TextView = itemView.findViewById(R.id.requestNoteTextView)
        private val userProfileImageView: RoundedImageView = itemView.findViewById(R.id.userProfileImageView)
        private val userInfoTextView: TextView = itemView.findViewById(R.id.userInfoTextView)
        val markCompleteButton: Button = itemView.findViewById(R.id.markCompleteButton)
        val assignToTextView: TextView = itemView.findViewById(R.id.assignToTextView)
        val assignJobButton: Button = itemView.findViewById(R.id.assignJobButton)
        val acceptJobButton: Button = itemView.findViewById(R.id.acceptJobButton)

        var request: HLServiceRequestModel? = null

        init {
            horseImageView.setOnClickListener { onClickHorse () }
            moreImageView.setOnClickListener { onClickMore () }
            userProfileImageView.setOnClickListener { onClickTrainer () }
            markCompleteButton.setOnClickListener { onClickMarkComplete () }
            assignJobButton.setOnClickListener { onClickAssignJob () }
            acceptJobButton.setOnClickListener { onClickAcceptJob () }
        }

        override fun setData(data: HLServiceRequestModel?) {
            request = data

            setDate()
            setHorse ()
            setOptionButton ()
            setRequest()
            setTrainer ()
            setStatus()
        }

        open fun setDate () {
            if (dataPosition == 0) {
                dateTextView.visibility = View.VISIBLE
                dateTextView.text = request?.requestDate?.date?.formattedString("EEE, MMMM d, YYYY")
            } else {
                getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                    val prevRequest = getItem(dataPosition - 1)

                    if (prevRequest?.requestDate?.calendar?.isSameDay(request?.requestDate?.calendar) == true) {
                        dateTextView.visibility = View.GONE
                    } else {
                        dateTextView.visibility = View.VISIBLE
                        dateTextView.text = request?.requestDate?.date?.formattedString("EEE, MMMM d, YYYY")
                    }
                }
            }
        }

        private fun setHorse () {
            horseImageView.loadImage(request?.horse?.avatarUrl, R.drawable.ic_horse_placeholder)
            horseBarnNameTextView.text = request?.horse?.barnName
            horseDisplayNameTextView.text = request?.horse?.displayName
        }

        open fun setOptionButton () {
            moreImageView.visibility = View.GONE
        }

        private fun setRequest () {
            requestShowTextView.text = request?.show?.name ?: "Non-Show Request"
            requestClassTextView.text = request?.competitionClass

            val serviceStrings = ArrayList<String>()
            request?.services?.forEach {
                serviceStrings.add("${it.service} (x${it.quantity})")
            }
            requestServicesTextView.text = serviceStrings.joinToString("\n")

            requestNoteTextView.text = request?.instruction
        }

        @SuppressLint("SetTextI18n")
        private fun setTrainer () {
            userProfileImageView.loadImage(request?.horse?.trainer?.avatarUrl, R.drawable.ic_profile)
            userInfoTextView.text = "${request?.horse?.trainer?.name ?: ""} ${request?.horse?.trainer?.barnName ?: ""}"
        }

        open fun setStatus () {
            markCompleteButton.visibility = View.GONE
            (assignToTextView.parent as ViewGroup).visibility = View.GONE
            assignJobButton.visibility = View.GONE
            acceptJobButton.visibility = View.GONE
            (assignJobButton.parent as ViewGroup).visibility = View.GONE

            if (request?.status == HLServiceRequestStatus.PENDING
                && ((request?.isMainServiceProvider == true && request?.assignerId == null)
                        || request?.isReassignedServiceProvider == true)) {

                (assignJobButton.parent as ViewGroup).visibility = View.VISIBLE

                assignJobButton.visibility = if (request?.isMainServiceProvider == true) View.VISIBLE else View.GONE
                acceptJobButton.visibility = View.VISIBLE
            } else if (request?.isMainServiceProvider == true && request?.assignerId != null) {

                (assignToTextView.parent as ViewGroup).visibility = View.VISIBLE
                assignToTextView.text = request?.assigner?.name

            } else if ((request?.isMainServiceProvider == true && request?.assignerId == null && request?.status == HLServiceRequestStatus.ACCEPTED)
                || (request?.isReassignedServiceProvider == true && request?.status == HLServiceRequestStatus.ACCEPTED)) {

                markCompleteButton.visibility = View.VISIBLE
            }
        }


        /**
         *  Event Handlers
         */
        private fun onClickHorse () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickHorse(request, dataPosition)
            }
        }

        private fun onClickMore () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickOption(request, dataPosition)
            }
        }

        private fun onClickTrainer () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickTrainer(request, dataPosition)
            }
        }

        private fun onClickMarkComplete () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickMarkComplete(request, dataPosition)
            }
        }

        fun onClickAssignJob () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickAssignJob(request, dataPosition)
            }
        }

        private fun onClickAcceptJob () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickAcceptJob(request, dataPosition)
            }
        }
    }

    class HLProviderExtendScheduleViewHolder(parent: ViewGroup?, res: Int) : HLProviderBaseScheduleViewHolder(parent, res) {

        private val completeButton: Button = itemView.findViewById(R.id.completeButton)
        private val assignToStatusTextView: TextView = itemView.findViewById(R.id.assignToStatusTextView)
        private val declinedTextView: TextView = itemView.findViewById(R.id.declinedTextView)
        private val chooseProviderButton: Button = itemView.findViewById(R.id.chooseProviderButton)
        private val dismissButton: Button = itemView.findViewById(R.id.dismissButton)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)


        init {
            chooseProviderButton.setOnClickListener { onClickChooseProvider () }
            dismissButton.setOnClickListener { onClickDismiss () }
        }

        /**
         *  Set Data Handlers
         */
        @SuppressLint("SetTextI18n")
        override fun setDate () {
            if (dataPosition == 0) {
                dateTextView.visibility = View.VISIBLE
                if (DateUtils.isToday(request?.requestDate ?: 0)) {
                    dateTextView.text = "Today\n"
                } else {
                    dateTextView.text = "Upcoming\n\n${request?.requestDate?.date?.formattedString("EEE, MMMM d, YYYY")}"
                }
            } else {
                if (DateUtils.isToday(request?.requestDate ?: 0)) {
                    dateTextView.visibility = View.GONE
                } else {
                    getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                        val prevRequest = getItem(dataPosition - 1)

                        if (prevRequest?.requestDate?.calendar?.isSameDay(request?.requestDate?.calendar) == true) {
                            dateTextView.visibility = View.GONE
                        } else {
                            dateTextView.visibility = View.VISIBLE
                            dateTextView.text = request?.requestDate?.date?.formattedString("EEE, MMMM d, YYYY")
                        }
                    }
                }
            }
        }

        override fun setOptionButton() {
            if (request?.status == HLServiceRequestStatus.DELETED || request?.status == HLServiceRequestStatus.COMPLETED) {
                moreImageView.visibility = View.GONE
            } else {
                moreImageView.visibility = View.VISIBLE
            }
        }

        @SuppressLint("SetTextI18n")
        override fun setStatus() {
            markCompleteButton.visibility = View.GONE
            completeButton.visibility = View.GONE
            (assignToTextView.parent as ViewGroup).visibility = View.GONE
            (declinedTextView.parent as ViewGroup).visibility = View.GONE
            (dismissButton.parent as ViewGroup).visibility = View.GONE
            statusTextView.visibility = View.GONE
            assignJobButton.visibility = View.GONE
            acceptJobButton.visibility = View.GONE
            (assignJobButton.parent as ViewGroup).visibility = View.GONE

            if (DateUtils.isToday(request?.requestDate ?: 0)) {
                if ((request?.isMainServiceProvider == true && request?.assignerId == null && request?.status == HLServiceRequestStatus.ACCEPTED)
                    || (request?.isReassignedServiceProvider == true && request?.status == HLServiceRequestStatus.ACCEPTED)) {

                    markCompleteButton.visibility = View.VISIBLE

                } else if ((request?.isMainServiceProvider == true && request?.assignerId == null && request?.status == HLServiceRequestStatus.COMPLETED)
                    || (request?.isReassignedServiceProvider == true && request?.status == HLServiceRequestStatus.COMPLETED)) {

                    completeButton.visibility = View.VISIBLE

                } else if (request?.isMainServiceProvider == false && request?.isReassignedServiceProvider == false) {

                    (assignToTextView.parent as ViewGroup).visibility = View.VISIBLE
                    if (request?.assigner != null) {
                        assignToTextView.text = request?.assigner?.name
                    } else {
                        assignToTextView.text = request?.serviceProvider?.name
                    }

                    assignToStatusTextView.text = "Reassigned"
                    assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestReassigned))
                } else if (request?.isMainServiceProvider == true && request?.assignerId != null) {

                    (assignToTextView.parent as ViewGroup).visibility = View.VISIBLE
                    assignToTextView.text = request?.assigner?.name

                    when (request?.status) {
                        HLServiceRequestStatus.PENDING -> {
                            assignToStatusTextView.text = "Pending"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestPending))
                        }
                        HLServiceRequestStatus.ACCEPTED -> {
                            assignToStatusTextView.text = "Accepted"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestAccepted))
                        }
                        HLServiceRequestStatus.COMPLETED -> {
                            assignToStatusTextView.text = "Completed"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestCompleted))
                        }
                        HLServiceRequestStatus.DECLINED -> {
                            (assignToTextView.parent as ViewGroup).visibility = View.GONE
                            (declinedTextView.parent as ViewGroup).visibility = View.VISIBLE
                            declinedTextView.text = "${request?.assigner?.name ?: ""} has declined."
                        }
                        HLServiceRequestStatus.PAID -> {
                            assignToStatusTextView.text = "Paid"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestAccepted))
                        }
                        HLServiceRequestStatus.INVOICED -> {
                            assignToStatusTextView.text = "Completed"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestCompleted))
                        }
                    }
                } else if (request?.status == HLServiceRequestStatus.PENDING
                    && ((request?.isMainServiceProvider == true && request?.assignerId == null)
                            || (request?.isReassignedServiceProvider == true))) {

                    (assignJobButton.parent as ViewGroup).visibility = View.VISIBLE

                    assignJobButton.visibility = if (request?.isMainServiceProvider == true) View.VISIBLE else View.GONE
                    acceptJobButton.visibility = View.VISIBLE

                } else if (request?.status == HLServiceRequestStatus.DELETED) {

                    (dismissButton.parent as ViewGroup).visibility = View.VISIBLE

                } else if (request?.status == HLServiceRequestStatus.PAID) {

                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Paid"
                    statusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestAccepted))

                } else if (request?.status == HLServiceRequestStatus.INVOICED) {
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Completed"
                    statusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestCompleted))
                }
            } else {
                if (request?.status == HLServiceRequestStatus.ACCEPTED
                    && ((request?.isMainServiceProvider == true && request?.assignerId == null)
                            || (request?.isReassignedServiceProvider == true)))  {
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Accepted"
                    statusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestAccepted))
                } else if (request?.isReassignedServiceProvider == true && request?.status == HLServiceRequestStatus.COMPLETED) {
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Completed"
                    statusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestCompleted))
                } else if (request?.isMainServiceProvider == true && request?.assignerId != null) {

                    (assignToTextView.parent as ViewGroup).visibility = View.VISIBLE
                    assignToTextView.text = request?.assigner?.name

                    when (request?.status) {
                        HLServiceRequestStatus.PENDING -> {
                            assignToStatusTextView.text = "Pending"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestPending))
                        }
                        HLServiceRequestStatus.ACCEPTED -> {
                            assignToStatusTextView.text = "Accepted"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestAccepted))
                        }
                        HLServiceRequestStatus.COMPLETED -> {
                            assignToStatusTextView.text = "Completed"
                            assignToStatusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestCompleted))
                        }
                        HLServiceRequestStatus.DECLINED -> {
                            (assignToTextView.parent as ViewGroup).visibility = View.GONE
                            (declinedTextView.parent as ViewGroup).visibility = View.VISIBLE
                            declinedTextView.text = "${request?.assigner?.name ?: ""} has declined."
                        }
                    }
                } else if (request?.status == HLServiceRequestStatus.PENDING
                    && ((request?.isMainServiceProvider == true && request?.assignerId == null)
                            || request?.isReassignedServiceProvider == true)) {

                    (assignJobButton.parent as ViewGroup).visibility = View.VISIBLE
                    assignJobButton.visibility = if (request?.isMainServiceProvider == true) View.VISIBLE else View.GONE
                    acceptJobButton.visibility = View.VISIBLE

                } else if (request?.status == HLServiceRequestStatus.DELETED) {

                    (dismissButton.parent as ViewGroup).visibility = View.VISIBLE

                } else if (request?.status == HLServiceRequestStatus.PAID) {
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Paid"
                    statusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestAccepted))
                } else if (request?.status == HLServiceRequestStatus.INVOICED) {
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Completed"
                    statusTextView.setTextColor(ResourceUtil.getColor(R.color.colorServiceRequestCompleted))
                }
            }
        }


        /**
         *  Event Handlers
         */
        private fun onClickChooseProvider () {
            onClickAssignJob()
        }

        private fun onClickDismiss () {
            getOwnerAdapter<HLProviderBaseScheduleAdapter>()?.run {
                itemListener.onClickDismiss(request, dataPosition)
            }
        }
    }
}