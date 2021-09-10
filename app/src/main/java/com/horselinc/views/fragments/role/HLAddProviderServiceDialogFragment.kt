package com.horselinc.views.fragments.role


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.fragment.app.DialogFragment
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLEditProviderServiceAdapter
import com.horselinc.views.adapters.recyclerview.HLProviderServiceEditDeleteAdapter
import com.horselinc.views.listeners.HLProviderServiceListener
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import me.abhinay.input.CurrencyEditText
import me.abhinay.input.CurrencySymbols
import java.util.concurrent.TimeUnit


class HLAddProviderServiceDialogFragment(private val adapter: HLEditProviderServiceAdapter?,
                                         private val editDeleteAdapter: HLProviderServiceEditDeleteAdapter?,
                                         private val selectedPosition: Int = -1,
                                         private val serviceListener: HLProviderServiceListener) : DialogFragment() {

    private lateinit var nameEditText: EditText
    private lateinit var rateEditText: CurrencyEditText
    private lateinit var addButton: Button
    private lateinit var contentView: View

    private lateinit var nameChangeObservable: Observable<CharSequence>
    private lateinit var rateChangeObservable: Observable<CharSequence>
    private val disposable = CompositeDisposable ()

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity!!)

        contentView = LayoutInflater.from(context).inflate(R.layout.fragment_add_provider_service_dialog, null)
        dialog.setContentView(contentView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)

        // initialize controls
        initControls ()

        // set reactive x
        setReactiveX ()

        return dialog
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    private fun onClickAdd () {
        val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(contentView.windowToken, 0)

        val name = nameEditText.text.trim().toString()
        val rate = rateEditText.cleanDoubleValue.toFloat()

        if (selectedPosition == -1) {
            val newService = HLServiceProviderServiceModel().apply {
                HLGlobalData.me?.let { me ->
                    userId = me.uid
                }
                service = name
                this.rate = rate
            }
            serviceListener.onAdd(newService)
        } else {
            adapter?.let {
                it.allData?.get(selectedPosition)?.run {
                    service = name
                    this.rate = rate

                    serviceListener.onUpdate(this, selectedPosition)
                }
            }

            editDeleteAdapter?.let {
                it.services[selectedPosition].run {
                    service = name
                    this.rate = rate

                    serviceListener.onUpdate(this, selectedPosition)
                }
            }
        }


        dismiss()
    }

    private fun initControls () {
        nameEditText = contentView.findViewById(R.id.nameEditText)
        rateEditText = contentView.findViewById(R.id.rateEditText)
        rateEditText.setCurrency(CurrencySymbols.USA)
        addButton = contentView.findViewById(R.id.addButton)
        addButton.isEnabled = false
        addButton.alpha = 0.2F

        // event handler
        addButton.setOnClickListener { onClickAdd () }
        contentView.findViewById<Button>(R.id.cancelButton).setOnClickListener { dismiss() }

        // set data
        if (selectedPosition > -1) {

            adapter?.let {
                nameEditText.setText(it.getItem(selectedPosition)?.service)
                rateEditText.setText(String.format("%.02f", it.getItem(selectedPosition)?.rate))
            }

            editDeleteAdapter?.let {
                nameEditText.setText(it.services[selectedPosition].service)
                rateEditText.setText(String.format("%.02f", it.services[selectedPosition].rate))
            }

            addButton.setText(R.string.save)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setReactiveX () {
        if (selectedPosition == -1) {
            nameChangeObservable = RxTextView.textChanges(nameEditText)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)

            rateChangeObservable = RxTextView.textChanges(rateEditText)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
        } else {
            nameChangeObservable = RxTextView.textChanges(nameEditText)
                .debounce(300, TimeUnit.MILLISECONDS)

            rateChangeObservable = RxTextView.textChanges(rateEditText)
                .debounce(300, TimeUnit.MILLISECONDS)
        }

        disposable.add(
            Observable.combineLatest(
                nameChangeObservable,
                rateChangeObservable,
                BiFunction<CharSequence, CharSequence, Array<String>> { name, rate ->
                    arrayOf(name.trim().toString(), rate.toString())
                }
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val name = it[0]
                    val rate = it[1]

                    var existRates: List<HLServiceProviderServiceModel>? = null
                    adapter?.let { adapter ->
                        existRates = adapter.allData?.filter { service ->
                            service.service.toLowerCase() == name.toLowerCase()
                        }.orEmpty()
                    }

                    editDeleteAdapter?.let { adapter ->
                        existRates = adapter.services.filter { service ->
                            service.service.toLowerCase() == name.toLowerCase()
                        }
                    }

                    val isEnabled = when {
                        name.isEmpty() -> {
                            nameEditText.error = ResourceUtil.getString(R.string.msg_err_required)
                            false
                        }
                        selectedPosition == -1 && existRates?.isNotEmpty() == true -> {
                            nameEditText.error = "Exist service"
                            false
                        }
                        rate.isEmpty() -> {
                            rateEditText.error = ResourceUtil.getString(R.string.msg_err_required)
                            false
                        }
                        else -> true
                    }
                    addButton.isEnabled = isEnabled
                    addButton.alpha = if (isEnabled) 1.0f else 0.2f
                }
        )
    }
}
