package com.devlomi.shared

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*

class WatchFacePainter(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private lateinit var hijriDate: HijrahDate

    private var timeFormat: SimpleDateFormat
    private var amPmFormat: SimpleDateFormat
    private var dayFormat: SimpleDateFormat
    private var dayNameFormat: SimpleDateFormat
    private lateinit var prayerTimes: PrayerTimes


    private lateinit var mBackgroundPaint: Paint
    private lateinit var timePaint: TextPaint
    private lateinit var amPmPaint: TextPaint
    private lateinit var dayPaint: TextPaint
    private lateinit var timeLeftCirclePaint: Paint
    private lateinit var timeLeftTextPaint: Paint
    private lateinit var prayerTimeCirclePaint: Paint
    private lateinit var prayerNameTextPaint: TextPaint
    private lateinit var prayerTimeTextPaint: TextPaint
    private lateinit var dateCirclePaint: Paint
    private lateinit var dateTextPaint: TextPaint
    private lateinit var hijriCirclePaint: Paint
    private lateinit var hijriTextPaint: Paint

    private var backgroundColor = -1
    private var prayerCircleColor = -1
    private var dateCircleColor = -1

    private var whiteTextColor = -1
    private var greyTextColor = -1

    private var hijriDateFormatter: DateTimeFormatter

    private var mAmbient: Boolean = false
    private var currentDate = ""


    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var coordinates: Coordinates =
        Coordinates(0.0, 0.0)

    private val _changeState = MutableSharedFlow<Unit>()
    val changeState: SharedFlow<Unit> get() = _changeState


    private var prayerTimesParams: CalculationParameters =
        CalculationMethod.EGYPTIAN.parameters.also {
            it.madhab = Madhab.SHAFI
        }
    private var madhab = Madhab.SHAFI


    init {

        scope.launch {
            combine(
                settingsDataStore.calculationMethod,
                settingsDataStore.madhab,
                settingsDataStore.lat,
                settingsDataStore.lng
            ) { calculationMethod, madhab, lat, lng ->
                return@combine PrayerConfigItem(calculationMethod, madhab, lat, lng)
            }.collectLatest {
                madhab = Madhab.valueOf(it.madhab ?: Madhab.SHAFI.name)
                if (it.lat != null && it.lng != null) {
                    coordinates = Coordinates(it.lat, it.lng)
                }
                it.calculationMethod?.let { calcMethod ->
                    CalculationMethod.valueOf(calcMethod)?.let { foundCalcMethod ->
                        prayerTimesParams = foundCalcMethod.parameters.also { calcParams ->
                            calcParams.madhab = madhab
                        }

                        initPrayerTimes(Date())
                        scope.launch {
                            _changeState.emit(Unit)
                        }
                    }
                }
            }
        }



        timeFormat = SimpleDateFormat("hh:mm")
        amPmFormat = SimpleDateFormat("a")
        dayFormat = SimpleDateFormat("dd")
        dayNameFormat = SimpleDateFormat("EEE")

        hijriDateFormatter = DateTimeFormatter.ofPattern("dd")


        initializeColors()
        initializePaint()
    }


    private fun updateWatchAmbient() {
        if (mAmbient) {

            mBackgroundPaint.color = Color.BLACK
            timePaint.color = Color.WHITE
            amPmPaint.color = Color.WHITE
            dayPaint.color = Color.WHITE

            timeLeftCirclePaint.color = Color.WHITE
            timeLeftTextPaint.color = Color.WHITE

            prayerTimeCirclePaint.color = Color.WHITE

            prayerNameTextPaint.color = Color.WHITE
            prayerTimeTextPaint.color = Color.WHITE

            dateCirclePaint.color = Color.WHITE
            dateTextPaint.color = Color.WHITE

            hijriCirclePaint.color = Color.WHITE
            hijriTextPaint.color = Color.WHITE

            prayerTimeCirclePaint.style = Paint.Style.STROKE
            timeLeftCirclePaint.style = Paint.Style.STROKE
            dateCirclePaint.style = Paint.Style.STROKE
            hijriCirclePaint.style = Paint.Style.STROKE

            setAntiAlias(false)


        } else {
            mBackgroundPaint.color = backgroundColor
            timePaint.color = whiteTextColor
            amPmPaint.color = greyTextColor
            dayPaint.color = whiteTextColor

            timeLeftCirclePaint.color = prayerCircleColor
            timeLeftTextPaint.color = whiteTextColor

            prayerTimeCirclePaint.color = prayerCircleColor

            prayerNameTextPaint.color = whiteTextColor
            prayerTimeTextPaint.color = whiteTextColor

            dateCirclePaint.color = dateCircleColor
            dateTextPaint.color = whiteTextColor

            hijriCirclePaint.color = dateCircleColor
            hijriTextPaint.color = whiteTextColor

            prayerTimeCirclePaint.style = Paint.Style.FILL
            timeLeftCirclePaint.style = Paint.Style.FILL
            dateCirclePaint.style = Paint.Style.FILL
            hijriCirclePaint.style = Paint.Style.FILL



            setAntiAlias(true)

        }
    }

    private fun setAntiAlias(bool: Boolean) {
        timePaint.isAntiAlias = bool
        amPmPaint.isAntiAlias = bool
        dayPaint.isAntiAlias = bool
        timeLeftCirclePaint.isAntiAlias = bool
        timeLeftTextPaint.isAntiAlias = bool

        prayerTimeCirclePaint.isAntiAlias = bool
        prayerNameTextPaint.isAntiAlias = bool
        prayerTimeTextPaint.isAntiAlias = bool

        dateCirclePaint.isAntiAlias = bool
        dateTextPaint.isAntiAlias = bool

        hijriCirclePaint.isAntiAlias = bool
        hijriTextPaint.isAntiAlias = bool
    }

    private var width = 0f
    private var height = 0f
    fun draw(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean,
        width: Float,
        height: Float,
    ) {
        this.width = width
        this.height = height
        mAmbient = isAmbient


        val date = Date.from(zonedDateTime.toInstant())

        val todayName = dayFormat.format(date)
        if (currentDate != todayName) {
            initPrayerTimes(date)
            initHijriDate(date)
            currentDate = todayName
        }


        drawCurrentTime(canvas, date)
        drawTimeLeftForNextPrayer(canvas, date)
        drawPrayer(canvas, date)
        drawDate(canvas, date)
        drawHijriDate(canvas, date)
        drawDayName(canvas, date)
    }


    fun drawBackground(canvas: Canvas) {
        if (mAmbient) {
            canvas.drawColor(Color.BLACK)
        } else {
            canvas.drawColor(mBackgroundPaint.color)
        }
    }


    private fun drawCurrentTime(canvas: Canvas, date: Date) {
        val time = timeFormat.format(date)
        val centerX = width / 2f
        val centerY = height / 2f
        val width = timePaint.measureText(time)
        val y = centerY + dpToPx(15f, context).toFloat()

        canvas.drawText(time, centerX - width / 2, y, timePaint)
        drawAMPM(canvas, centerX, y, date)

    }

    private fun drawAMPM(canvas: Canvas, x: Float, y: Float, date: Date) {
        val time = amPmFormat.format(date)
        val width = amPmPaint.measureText(time)

        val x = x + width / 1.5f
        canvas.drawText(time, x, y + dpToPx(19f, context), amPmPaint)
    }


    private fun drawTimeLeftForNextPrayer(canvas: Canvas, date: Date) {
        val nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            return
        }

        val timeForPrayer = prayerTimes.timeForPrayer(nextPrayer)
        val diff = timeForPrayer.time - date.time

        var minutes = 0L
        var hours = 0L
        if (diff > 0) {
            minutes = (diff / (1000 * 60)) % 60;
            hours = diff / (1000 * 60 * 60);
            if (hours < 0) {
                hours = 0
            }
            if (minutes < 0) {
                minutes = 0
            }

        }

        val time = String.format(Locale.US, "%2d:%02d", hours, minutes)
        val centerX = width / 2f
        val centerY = height / 2f

        val y = centerY + width / 5 + dpToPx(5f, context)
        val width = timeLeftTextPaint.measureText(time)
        val height = time.getBounds(timeLeftTextPaint).height()

        val radius = dpToPx(17f, context).toFloat()

        canvas.drawCircle(centerX, y, radius, timeLeftCirclePaint)

        canvas.drawText(
            time,
            centerX - dpToPx(2f, context) - width / 2f,
            y + height / 2f - dpToPx(2f, context),
            timeLeftTextPaint
        )

    }


    private fun drawPrayer(canvas: Canvas, date: Date) {
        val nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            return
        }

        val prayerName = nextPrayer.name

        val prayerNameWidth = prayerNameTextPaint.measureText(prayerName)
        val prayerNameHeight = prayerName.getBounds(prayerNameTextPaint).height()

        val centerX = width / 2f
        val centerY = height / 2f

        val y = centerY - width / 5 + dpToPx(5f, context)

        val radius = dpToPx(17f, context).toFloat()

        val circlePaint = prayerTimeCirclePaint
        canvas.drawCircle(centerX, y, radius, circlePaint)

        val textY = y - prayerNameHeight
        canvas.drawText(
            prayerName,
            centerX - prayerNameWidth / 2f,
            textY,
            prayerNameTextPaint
        )

        drawPrayerTime(nextPrayer, canvas, centerX, textY)
    }

    private fun drawPrayerTime(prayer: Prayer, canvas: Canvas, x: Float, y: Float) {
        val timeForPrayer = prayerTimes.timeForPrayer(prayer)
        val time = timeFormat.format(timeForPrayer)
        val height = time.getBounds(prayerTimeTextPaint).height()
        val width = prayerTimeTextPaint.measureText(time)

        canvas.drawText(
            time,
            x - width / 2f,
            y + height + dpToPx(3f, context),
            prayerTimeTextPaint
        )
    }

    private fun drawDate(canvas: Canvas, date: Date) {
        val time = dayFormat.format(date)

        val radius = dpToPx(10f, context).toFloat()
        val x = width.toFloat() - (width / 5f) + dpToPx(5f, context)
        canvas.drawCircle(
            x + dpToPx(4f, context),
            width / 2f,
            radius,
            dateCirclePaint
        )
        canvas.drawText(
            time,
            x - dpToPx(3f, context),
            width / 2f + dpToPx(4f, context),
            dateTextPaint
        )
    }

    private fun drawHijriDate(canvas: Canvas, date: Date) {
        val hijriDateStr = hijriDate.format(hijriDateFormatter)

        val radius = dpToPx(10f, context).toFloat()

        val x = width.toFloat() - (width / 5f) + dpToPx(25f, context)
        canvas.drawCircle(
            x + dpToPx(4f, context),
            width / 2f,
            radius,
            dateCirclePaint
        )
        canvas.drawText(
            hijriDateStr,
            x - dpToPx(3f, context),
            width / 2f + dpToPx(4f, context),
            hijriTextPaint
        )
    }

    private fun drawDayName(canvas: Canvas, date: Date) {
        val date = dayNameFormat.format(date).uppercase()

        val height = date.getBounds(dayPaint).height()

        val x = width.toFloat() - (width / 5f)

        canvas.drawText(
            date,
            x - dpToPx(3f, context),
            width / 2f + height + dpToPx(4f, context) + height,
            dayPaint
        )
    }


    private fun initPrayerTimes(date: Date) {
        val dateComponents = DateComponents.from(date)
        prayerTimes = PrayerTimes(coordinates, dateComponents, prayerTimesParams)
    }

    private fun initHijriDate(date: Date) {
        val mCalendar = Calendar.getInstance()
        mCalendar.time = date
        hijriDate = HijrahChronology.INSTANCE.date(
            LocalDate.of(
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH) + 1,
                mCalendar.get(Calendar.DATE)
            )
        )
    }

    private fun initializeColors() {
        backgroundColor = Color.parseColor("#2B324B")
        dateCircleColor = Color.parseColor("#E32E2E")
        prayerCircleColor = Color.parseColor("#163B72")
        whiteTextColor = Color.WHITE
        greyTextColor = Color.GRAY
    }

    private fun initializePaint() {
        mBackgroundPaint = Paint().apply {
            color = backgroundColor
        }

        timePaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.rubik)
            textSize = spToPx(45f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }

        amPmPaint = TextPaint().apply {
            textSize = spToPx(20f, context).toFloat()
            color = greyTextColor
            isAntiAlias = true
        }

        dayPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.righteous)
            textSize = spToPx(12f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }

        dateCirclePaint = Paint().apply {
            color = dateCircleColor
            isAntiAlias = true
        }

        dateTextPaint = TextPaint().apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = spToPx(13f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }

        hijriCirclePaint = Paint().apply {
            color = dateCircleColor
            isAntiAlias = true
        }

        hijriTextPaint = TextPaint().apply {
            textSize = spToPx(13f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }



        prayerNameTextPaint = TextPaint().apply {
            textSize = spToPx(6f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }

        prayerTimeTextPaint = TextPaint().apply {
            textSize = spToPx(12f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }

        prayerTimeCirclePaint = Paint().apply {
            color = prayerCircleColor
            isAntiAlias = true
        }



        timeLeftCirclePaint = Paint().apply {
            color = prayerCircleColor
            isAntiAlias = true
        }

        timeLeftTextPaint = TextPaint().apply {
            textSize = spToPx(14f, context).toFloat()
            color = whiteTextColor
            isAntiAlias = true
        }


    }

    fun onDestroy() {
        scope.cancel("scope clear() request")
    }

    fun updateAmbient(ambient: Boolean) {
        mAmbient = ambient
        updateWatchAmbient()
    }


}