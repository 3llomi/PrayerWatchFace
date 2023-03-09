package com.devlomi.shared

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withTranslation
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
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalField
import java.util.*

class WatchFacePainter(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private lateinit var hijriDate: HijrahDate

    private lateinit var timeFormat: SimpleDateFormat
    private var amPmFormat: SimpleDateFormat
    private var dayMonthFormat: SimpleDateFormat
    private var dayNameFormat: SimpleDateFormat
    private lateinit var prayerTimes: PrayerTimes


    private lateinit var mBackgroundPaint: Paint
    private lateinit var timePaint: TextPaint
    private lateinit var amPmPaint: TextPaint
    private lateinit var dayPaint: TextPaint
    private lateinit var timeLeftTextPaint: Paint
    private lateinit var remainingTextPaint: TextPaint
    private lateinit var prayerNameTextPaint: TextPaint
    private lateinit var prayerTimeTextPaint: TextPaint
    private lateinit var dateTextPaint: TextPaint
    private lateinit var hijriTextPaint: Paint
    private lateinit var bottomArcPaint: Paint
    private lateinit var bottomSeparatorPaint: Paint

    private var backgroundColor = -1
    private var backgroundColorBottomPart = -1

    private var mainForegroundColor = -1
    private var onBottomForegroundColor = -1
    private var ambientForegroundColor = -1
    private var greyTextColor = -1

    private var hijriDateFormatter: DateTimeFormatter

    private var mAmbient: Boolean = false
    private var currentDate = ""

    private var remainingY = 0f


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

    private var is24Hours = false
    private var hijriOffset = 0


    init {

        listenForBackgroundColor()
        listenForPrayerConfig()
        listenForPrayerOffset()

        initializeTimeFormat(is24Hours)
        amPmFormat = SimpleDateFormat("a", Locale.US)
        dayMonthFormat = SimpleDateFormat("dd MMM", Locale.US)
        dayNameFormat = SimpleDateFormat("EEE", Locale.US)

        hijriDateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.US)


        initializeColors()
        initializePaint()
    }

    private fun listenForPrayerConfig() {
        scope.launch {
            combine(
                settingsDataStore.calculationMethod,
                settingsDataStore.madhab,
                settingsDataStore.lat,
                settingsDataStore.lng,
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

        scope.launch {
            settingsDataStore.is24Hours.collectLatest {
                this@WatchFacePainter.is24Hours = it
                initializeTimeFormat(is24Hours)
                scope.launch {
                    _changeState.emit(Unit)
                }
            }
        }

        scope.launch {
            settingsDataStore.hijriOffset.collectLatest {
                this@WatchFacePainter.hijriOffset = it
                scope.launch {
                    _changeState.emit(Unit)
                }
            }
        }
    }

    private fun listenForBackgroundColor() {
        scope.launch {
            combine(
                settingsDataStore.backgroundColor,
                settingsDataStore.backgroundBottomPart,
                settingsDataStore.foregroundColor,
                settingsDataStore.foregroundBottomPart,
            ) { backgroundColor, backgroundBottomPart, foregroundColor, foregroundBottomPart ->
                return@combine BackgroundColorSettingsItem(
                    backgroundColor,
                    backgroundBottomPart,
                    foregroundColor,
                    foregroundBottomPart
                )
            }.collectLatest { item ->

                item.backgroundColor?.let {
                    val color = Color.parseColor(it)
                    backgroundColor = color
                }

                item.backgroundColorBottomPart?.let {
                    val color = Color.parseColor(it)
                    backgroundColorBottomPart = color
                }


                item.foregroundColor?.let {
                    val color = Color.parseColor(it)
                    mainForegroundColor = color
                }

                item.foregroundColorBottomPart?.let {
                    val color = Color.parseColor(it)
                    onBottomForegroundColor = color
                }

                initializePaint()
                scope.launch {
                    _changeState.emit(Unit)
                }
            }
        }
    }

    private fun listenForPrayerOffset() {
        scope.launch {
            settingsDataStore.fajrOffset.collectLatest {
                prayerTimesParams.adjustments.fajr = it
                scope.launch {
                    initPrayerTimes(Date())
                    _changeState.emit(Unit)
                }
            }
        }

        scope.launch {
            settingsDataStore.shurooqOffset.collectLatest {
                prayerTimesParams.adjustments.sunrise = it
                scope.launch {
                    initPrayerTimes(Date())
                    _changeState.emit(Unit)
                }
            }
        }

        scope.launch {
            settingsDataStore.dhuhrOffset.collectLatest {
                prayerTimesParams.adjustments.dhuhr = it
                scope.launch {
                    initPrayerTimes(Date())
                    _changeState.emit(Unit)
                }
            }
        }

        scope.launch {
            settingsDataStore.asrOffset.collectLatest {
                prayerTimesParams.adjustments.asr = it
                scope.launch {
                    initPrayerTimes(Date())
                    _changeState.emit(Unit)
                }
            }
        }

        scope.launch {
            settingsDataStore.maghribOffset.collectLatest {
                prayerTimesParams.adjustments.maghrib = it
                scope.launch {
                    initPrayerTimes(Date())
                    _changeState.emit(Unit)
                }
            }
        }

        scope.launch {
            settingsDataStore.ishaaOffset.collectLatest {
                prayerTimesParams.adjustments.isha = it
                scope.launch {
                    initPrayerTimes(Date())
                    _changeState.emit(Unit)
                }
            }
        }
    }

    private fun initializeTimeFormat(is24Hours: Boolean) {
        val pattern = if (is24Hours) "HH:mm" else "hh:mm"
        timeFormat = SimpleDateFormat(pattern, Locale.US)
    }


    private fun updateWatchAmbient() {
        if (mAmbient) {

            mBackgroundPaint.color = Color.BLACK
            setAntiAlias(false)


        } else {
            mBackgroundPaint.color = backgroundColor
            setAntiAlias(true)
        }

        val foregroundColor = if (mAmbient) ambientForegroundColor else mainForegroundColor
        val onBottomColor = if (mAmbient) ambientForegroundColor else onBottomForegroundColor
        timePaint.color = foregroundColor
        amPmPaint.color = if (mAmbient) foregroundColor else greyTextColor
        dayPaint.color = foregroundColor

        bottomSeparatorPaint.color = onBottomColor
        remainingTextPaint.color = onBottomColor
        timeLeftTextPaint.color = onBottomColor
        prayerNameTextPaint.color = onBottomColor
        prayerTimeTextPaint.color = onBottomColor

        dateTextPaint.color = foregroundColor
        hijriTextPaint.color = foregroundColor

    }

    private fun setAntiAlias(bool: Boolean) {
        timePaint.isAntiAlias = bool
        amPmPaint.isAntiAlias = bool
        dayPaint.isAntiAlias = bool
        timeLeftTextPaint.isAntiAlias = bool
        prayerNameTextPaint.isAntiAlias = bool
        prayerTimeTextPaint.isAntiAlias = bool

        dateTextPaint.isAntiAlias = bool

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

        val todayName = dayMonthFormat.format(date)
        if (currentDate != todayName) {
            initPrayerTimes(date)
            initHijriDate(date)
            currentDate = todayName
        }
        val nextPrayer = prayerTimes.nextPrayer()


        drawCurrentTime(canvas, date)

        if (nextPrayer == Prayer.NONE) {
            val date = Date.from(zonedDateTime.toInstant().plus(1, ChronoUnit.DAYS))
            initPrayerTimes(date)
        }
        if (!isAmbient) {
            drawBottomArc(canvas)
        }
        drawTimeLeftForNextPrayer(canvas, date)
        drawPrayer(canvas)
        drawBottomSeparator(canvas)

        drawDate(canvas, date)
        drawHijriDate(canvas)
        drawDayName(canvas, date)
    }

    private fun drawBottomSeparator(canvas: Canvas) {

        val lineHeight = dpToPx(38f, context).toFloat()
        val lineY = remainingY - dpToPx(12f, context)
        canvas.withTranslation(width / 2f, lineY) {
            canvas.drawLine(0f, lineHeight, 0f, 0f, bottomSeparatorPaint)
        }
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
        val width = timePaint.measureText(time)
        val y = height * 0.33f
        val x = centerX - width / 2
        canvas.drawText(time, x, y, timePaint)
        if (!is24Hours) {
            drawAMPM(canvas, x + width, y, date)
        }

    }

    private fun drawAMPM(canvas: Canvas, x: Float, y: Float, date: Date) {
        val time = amPmFormat.format(date)
        val width = amPmPaint.measureText(time)

        val x = x - width - dpToPx(4f, context)
        canvas.drawText(time, x, y + dpToPx(17f, context), amPmPaint)
    }


    private fun drawTimeLeftForNextPrayer(canvas: Canvas, date: Date) {
        val nextPrayer = prayerTimes.nextPrayer()


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

        val width = timeLeftTextPaint.measureText(time)
        val height = time.getBounds(timeLeftTextPaint).height()

        val remainingText = "REMAINING"
        val remainingWidth = remainingTextPaint.measureText(remainingText)
        val remainingHeight = remainingText.getBounds(remainingTextPaint).height()
        val remainingX = this.width / 2 - remainingWidth - dpToPx(8f, context)
        remainingY = this.height - dpToPx(30f, context) - remainingHeight
        canvas.drawText(remainingText, remainingX, remainingY, remainingTextPaint)


        val y = remainingY + dpToPx(6f, context) + height

        val timeLeftX = remainingX + remainingWidth / 2f - width / 2f
        canvas.withTranslation(x = timeLeftX, y = y) {
            canvas.drawText(
                time,
                0f,
                0f,
                timeLeftTextPaint
            )
        }

    }


    private fun drawPrayer(canvas: Canvas) {
        val nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            return
        }

        val prayerName = nextPrayer.name

        val prayerNameWidth = prayerNameTextPaint.measureText(prayerName)

        val centerX = width / 2f


        val x = centerX + dpToPx(12f, context)
        val textY = remainingY

        canvas.withTranslation(x = x, y = textY) {
            canvas.drawText(
                prayerName,
                0f,
                0f,
                prayerNameTextPaint
            )

        }
        drawPrayerTime(nextPrayer, canvas, x, textY, prayerNameWidth)
    }

    private fun drawPrayerTime(
        prayer: Prayer,
        canvas: Canvas,
        x: Float,
        y: Float,
        prayerNameWidth: Float
    ) {
        val timeForPrayer = prayerTimes.timeForPrayer(prayer)
        val time = timeFormat.format(timeForPrayer)
        val height = time.getBounds(prayerTimeTextPaint).height()
        val width = prayerTimeTextPaint.measureText(time)

        val x = x + prayerNameWidth / 2f - width / 2f
        canvas.withTranslation(x = x, y = y + height + dpToPx(6f, context)) {
            canvas.drawText(
                time,
                0f,
                0f,
                prayerTimeTextPaint
            )
        }
    }

    private fun drawDate(canvas: Canvas, date: Date) {
        val time = dayMonthFormat.format(date)
        val textHeight = time.getBounds(dateTextPaint).height()
        val x = dpToPx(16f, context).toFloat()


        val y = height / 2f + (textHeight / 2f)
        canvas.drawText(
            time,
            x,
            y,
            dateTextPaint
        )
    }

    private fun drawHijriDate(canvas: Canvas) {
        val hijriDateStr =
            hijriDate.plus(hijriOffset.toLong(), ChronoUnit.DAYS).format(hijriDateFormatter)

        val textWidth = hijriTextPaint.measureText(hijriDateStr)
        val x = width - textWidth - dpToPx(5f, context)
        val textHeight = hijriDateStr.getBounds(hijriTextPaint).height()
        val y = height / 2f + (textHeight / 2f)

        canvas.drawText(
            hijriDateStr,
            x,
            y,
            hijriTextPaint
        )
    }

    private fun drawBottomArc(canvas: Canvas) {

        val rectHeight = height * 0.28f
        canvas.withTranslation(y = height - rectHeight) {
            val rectangle = RectF(0f, 0f, this@WatchFacePainter.width, rectHeight)
            canvas.drawRect(rectangle, bottomArcPaint)

        }
    }

    private fun drawDayName(canvas: Canvas, date: Date) {
        val date = dayNameFormat.format(date).uppercase()

        val height = date.getBounds(dayPaint).height()


        val centerX = width / 2f
        val width = dayPaint.measureText(date)
        val x = centerX - width / 2


        val y = height + dpToPx(12f, context).toFloat()
        canvas.drawText(
            date,
            x,
            y,
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
        backgroundColor = context.getColor(R.color.wf_preview)
        backgroundColorBottomPart = context.getColor(R.color.wf_bottom_bg)
        mainForegroundColor = context.getColor(R.color.wf_fg)
        onBottomForegroundColor = context.getColor(R.color.wf_bottom_fg)
        ambientForegroundColor = Color.WHITE
        greyTextColor = Color.GRAY
    }

    private fun initializePaint() {
        mBackgroundPaint = Paint().apply {
            color = backgroundColor
        }

        bottomArcPaint = Paint().apply {
            isAntiAlias = true
            color = backgroundColorBottomPart
            style = Paint.Style.FILL
        }

        bottomSeparatorPaint = Paint().apply {
            this.color = onBottomForegroundColor
            style = Paint.Style.STROKE
        }

        timePaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.rubik)
            textSize = spToPx(45f, context).toFloat()
            color = mainForegroundColor
            isAntiAlias = true
        }

        amPmPaint = TextPaint().apply {
            textSize = spToPx(16f, context).toFloat()
            color = greyTextColor
            isAntiAlias = true
        }

        dayPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.righteous)
            textSize = spToPx(12f, context).toFloat()
            color = mainForegroundColor
            isAntiAlias = true
        }

        dateTextPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.alata)
            textSize = spToPx(15f, context).toFloat()
            color = mainForegroundColor
            isAntiAlias = true
        }


        hijriTextPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.alata)
            textSize = spToPx(13f, context).toFloat()
            color = mainForegroundColor
            isAntiAlias = true
        }



        prayerNameTextPaint = TextPaint().apply {
            textSize = spToPx(12f, context).toFloat()
            typeface =
                Typeface.create(ResourcesCompat.getFont(context, R.font.cairo), Typeface.BOLD)
            color = onBottomForegroundColor
            isAntiAlias = true
        }

        prayerTimeTextPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.cairo)
            textSize = spToPx(15f, context).toFloat()
            color = onBottomForegroundColor
            isAntiAlias = true
        }

        timeLeftTextPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.cairo)
            textSize = spToPx(15f, context).toFloat()
            color = onBottomForegroundColor
            isAntiAlias = true
        }

        remainingTextPaint = TextPaint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.cairo)
            textSize = spToPx(10f, context).toFloat()
            color = onBottomForegroundColor
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