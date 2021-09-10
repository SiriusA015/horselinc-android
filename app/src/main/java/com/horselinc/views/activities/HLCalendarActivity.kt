package com.horselinc.views.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.horselinc.*
import com.horselinc.utils.ResourceUtil
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.previous
import kotlinx.android.synthetic.main.activity_calendar.*
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class HLCalendarActivity : AppCompatActivity() {

    private var startDate: Long = 0
    private var endDate: Long = 0
    private var selectedDate: Long = 0

    private var curSelectedDate: LocalDate? = null

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMM")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // set action bar
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val title = SpannableString(getString(R.string.select_date))
        title.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        supportActionBar?.title = title

        // set extra
        startDate = intent.getLongExtra(IntentExtraKey.CALENDAR_START_DATE, 0)
        endDate = intent.getLongExtra(IntentExtraKey.CALENDAR_END_DATE, 0)
        selectedDate = intent.getLongExtra(IntentExtraKey.CALENDAR_SELECTED_DATE, 0)

        // event handler
        okButton.setOnClickListener { onClickOK () }
        cancelButton.setOnClickListener { onClickCancel () }
        prevMonthImageView.setOnClickListener { onClickPrev () }
        nextMonthImageView.setOnClickListener { onClickNext () }

        // calendar view
        initCalendar ()
    }

    /**
     *  Event Handler
     */
    private fun onClickOK () {
        if (curSelectedDate != null) {
            val resIntent = Intent()
            resIntent.putExtra(IntentExtraKey.CALENDAR_RETURN_DATE, curSelectedDate?.atStartOfDay(ZoneOffset.systemDefault())?.toInstant()?.toEpochMilli())
            setResult(RESULT_OK, resIntent)
            finish()
        } else {
            showErrorMessage("Please select date")
        }
    }

    private fun onClickCancel () {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun onClickPrev () {
        calendarView.findFirstVisibleMonth()?.let {
            calendarView.smoothScrollToMonth(it.yearMonth.previous)
        }
    }

    private fun onClickNext () {
        calendarView.findFirstVisibleMonth()?.let {
            calendarView.smoothScrollToMonth(it.yearMonth.next)
        }
    }

    /**
     *  Initialize Handler
     */
    private fun initCalendar () {

        val startMonth = if (startDate == 0L) {
            YearMonth.now().minusMonths(12)
        }  else {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDate
            YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }

        val endMonth = if (endDate == 0L) {
            YearMonth.now().plusMonths(12)
        }  else {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = endDate
            YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }

        val selectedMonth = if (selectedDate == 0L) {
            YearMonth.now()
        } else {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }

        curSelectedDate = if (selectedDate == 0L) {
            if (startDate == 0L) {
                LocalDate.now()
            } else {
                val calendar = startDate.calendar
                LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
            }
        } else {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        }

        calendarView.setup(startMonth, endMonth, daysOfWeekFromLocale().first())
        calendarView.scrollToMonth(selectedMonth)

        calendarView.dayBinder = object : DayBinder<DayViewContainer> {

            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {

                container.day = day
                val textView = container.textView
                textView.text = day.date.dayOfMonth.toString()

                if (day.owner == DayOwner.THIS_MONTH) {
                    when (day.date) {
                        curSelectedDate -> {
                            val textColor = if (day.date == LocalDate.now()) {
                                ResourceUtil.getColor(R.color.colorPink)
                            } else {
                                Color.WHITE
                            }
                            textView.setTextColor(textColor)
                            textView.setBackgroundResource(R.drawable.calendar_selected_day_background)
                        }
                        LocalDate.now() -> {
                            textView.setTextColor(ResourceUtil.getColor(R.color.colorPink))
                            textView.background = null
                        }
                        else -> {
                            textView.setTextColor(Color.BLACK)
                            textView.background = null
                        }
                    }
                } else {
                    textView.setTextColor(Color.GRAY)
                    textView.background = null
                }
            }
        }

        calendarView.monthScrollListener = { month ->
            val title = "${monthTitleFormatter.format(month.yearMonth)} ${month.yearMonth.year}"
            monthTextView.text = title
        }

    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay

        val textView = with(view) {
            setOnClickListener {
                if (day.owner == DayOwner.THIS_MONTH) {
                    if (curSelectedDate == day.date) {
                        curSelectedDate = null
                        calendarView.notifyDayChanged(day)
                    } else {
                        val calendar = startDate.calendar
                        val start = LocalDate.of(calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH))

                        if (day.date.isAfter(start) || day.date == start) {
                            val oldDate = curSelectedDate
                            curSelectedDate = day.date
                            calendarView.notifyDateChanged(day.date)
                            oldDate?.let { calendarView.notifyDateChanged(oldDate) }
                        }
                    }
                }
            }
            return@with this as TextView
        }
    }
}
