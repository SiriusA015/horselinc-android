package com.horselinc.views.fragments.provider


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.event.HLUpdateHorsePrivateNoteEvent
import com.horselinc.views.fragments.HLBaseFragment
import org.greenrobot.eventbus.EventBus

class HLProviderPrivateNoteFragment(private val horse: HLHorseModel) : HLBaseFragment() {

    private var privateNoteTextView: TextView? = null
    private var privateNoteEditText: TextView? = null
    private var saveButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_private_note, container, false)

            initControls ()
        }
        return rootView
    }

    /**
     *  Initialize Handlers
     */
    @SuppressLint("SetTextI18n")
    private fun initControls () {
        // controls
        privateNoteTextView = rootView?.findViewById(R.id.privateNoteTextView)
        privateNoteEditText = rootView?.findViewById(R.id.privateNoteEditText)
        saveButton = rootView?.findViewById(R.id.saveButton)

        privateNoteTextView?.text = "My notes for ${horse.barnName}"
        privateNoteEditText?.text = horse.privateNote

        // bind progress buttons
        setProgressButton(saveButton)

        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        saveButton?.setOnClickListener { onClickSave () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickSave () {
        val note = privateNoteEditText?.text?.trim().toString()
        if (note.isEmpty()) {
            showInfoMessage("Please enter private note")
            return
        }

        showProgressButton(saveButton)
        HLFirebaseService.instance.updateHorse(horse.uid, hashMapOf("privateNote" to note), object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                hideProgressButton(saveButton, stringResId = R.string.save)
                EventBus.getDefault().post(HLUpdateHorsePrivateNoteEvent(note))
                popFragment()
            }

            override fun onFailure(error: String) {
                hideProgressButton(saveButton, stringResId = R.string.save)
                showError(error)
            }
        })
    }
}
