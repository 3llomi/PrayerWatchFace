package com.devlomi.shared.analog_watch_face

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withRotation
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.devlomi.shared.constants.ComplicationsIds
import com.devlomi.shared.constants.DefaultWatchFaceColors
import com.devlomi.shared.constants.FontSize
import com.devlomi.shared.config.PrayerConfigState
import com.devlomi.shared.R
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.WatchFacePainter
import com.devlomi.shared.common.dpToPx
import com.devlomi.shared.common.getBounds
import com.devlomi.shared.common.getIshaaTimePreviousDay
import com.devlomi.shared.common.getLocaleStringResource
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.config.offsetWithDaylight
import com.devlomi.shared.common.previousPrayer
import com.devlomi.shared.common.spToPx
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit


class AnalogWatchFacePainter(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase,
) : WatchFacePainter {
    private lateinit var hijriDate: HijrahDate

    private lateinit var timeFormat: SimpleDateFormat
    private var amPmFormat: SimpleDateFormat
    private lateinit var dayMonthFormat: SimpleDateFormat
    private lateinit var dayNameFormat: SimpleDateFormat
    private lateinit var prayerTimes: PrayerTimes


    private lateinit var ambientBackgroundPaint: Paint
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
    private lateinit var progressPaint: Paint

    private var backgroundColor = -1
    private var backgroundColorBottomPart = -1

    private var mainForegroundColor = -1
    private var onBottomForegroundColor = -1
    private var ambientForegroundColor = -1
    private var greyTextColor = -1
    private var progressColor = -1
    private var hourHandColor = -1
    private var secondsHandColor = -1
    private var hourMarkerColor = -1

    private lateinit var hijriDateFormatter: DateTimeFormatter

    private var mAmbient: Boolean = false
    private var currentDate = ""

    private var remainingY = 0f


    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = PrayerConfigState(settingsDataStore, scope).state

    private var coordinates: Coordinates =
        Coordinates(0.0, 0.0)

    private val _changeState = MutableSharedFlow<Unit>()
    override val changeState: SharedFlow<Unit> get() = _changeState


    private var prayerTimesParams: CalculationParameters =
        CalculationMethod.EGYPTIAN.parameters.also {
            it.madhab = Madhab.SHAFI
        }

    private val clockData: WatchFaceData by lazy {
        WatchFaceData()
    }

    private var locale: Locale = Locale.US
    private var wallpaperBitmap: Bitmap? = null


    private lateinit var clockHandPaint: Paint
    private lateinit var secondsHandPaint: Paint

    private lateinit var hourMarkerPaint: Paint


    private lateinit var hourHandFill: Path
    private lateinit var hourHandBorder: Path
    private lateinit var hourHandBorderRect: HandRect
    private lateinit var minuteHandFill: Path
    private lateinit var minuteHandBorder: Path
    private lateinit var secondHand: Path

    // Changed when setting changes cause a change in the minute hand arm (triggered by user in
    // updateUserStyle() via userStyleRepository.addUserStyleListener()).
    private var armLengthChangedRecalculateClockHands: Boolean = false

    // Default size of watch face drawing area, that is, a no size rectangle. Will be replaced with
    // valid dimensions from the system.
    private var currentWatchFaceSize = Rect(0, 0, 0, 0)


    init {

        initializeColors()
        initializePaint()

        initializeTimeFormat()
        amPmFormat = SimpleDateFormat("a", Locale.US)
        initializeDateFormatters()

        scope.launch {
            state.collectLatest {
                prayerTimesParams = it.calculationMethod.parameters.also { calcParams ->
                    calcParams.madhab = it.madhab
                }
                prayerTimesParams.adjustments.dhuhr = it.offsetWithDaylight(Prayer.DHUHR)
                prayerTimesParams.adjustments.asr = it.offsetWithDaylight(Prayer.ASR)
                prayerTimesParams.adjustments.maghrib = it.offsetWithDaylight(Prayer.MAGHRIB)
                prayerTimesParams.adjustments.isha = it.offsetWithDaylight(Prayer.ISHA)
                prayerTimesParams.adjustments.fajr = it.offsetWithDaylight(Prayer.FAJR)
                prayerTimesParams.adjustments.sunrise = it.offsetWithDaylight(Prayer.SUNRISE)
                coordinates = Coordinates(it.lat, it.lng)
                LocaleType.values()
                    .firstOrNull { localeType: LocaleType -> localeType == it.localeType }
                    ?.let { locale ->
                        this@AnalogWatchFacePainter.locale = LocaleHelper.getLocale(locale)
                    }
                wallpaperBitmap = if (it.customWallpaperEnabled) {
                    try {
                        val wallpaperFile =
                            File(File(context.filesDir, "wallpapers_current"), it.wallpaper)
                        if (wallpaperFile.exists()) {
                            BitmapFactory.decodeFile(wallpaperFile.path)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                it.backgroundColor?.let {
                    val color = Color.parseColor(it)
                    backgroundColor = color
                }

                it.backgroundColorBottomPart?.let {
                    val color = Color.parseColor(it)
                    backgroundColorBottomPart = color
                }


                it.foregroundColor?.let {
                    val color = Color.parseColor(it)
                    mainForegroundColor = color
                }

                it.foregroundColorBottomPart?.let {
                    val color = Color.parseColor(it)
                    onBottomForegroundColor = color
                }

                it.progressColor?.let {
                    val color = Color.parseColor(it)
                    progressColor = color
                }

                it.handPrimaryColor?.let {
                    val color = Color.parseColor(it)
                    hourHandColor = color
                }

                it.handSecondaryColor?.let {
                    val color = Color.parseColor(it)
                    secondsHandColor = color
                }

                it.hourMarkerColor?.let {
                    val color = Color.parseColor(it)
                    hourMarkerColor = color
                }



                initHijriDate(Date())
                initPrayerTimes(Date())
                initializePaint()
                initializeTimeFormat()
                initializeDateFormatters()
                updateFontSizes(getFontSizeAccu(it.fontSize))


                _changeState.emit(Unit)
            }
        }
    }


    private fun getFontSizeAccu(fontSizeConfig: Int): Int {
        val fontSizePixels = when (fontSizeConfig) {
            FontSize.MEDIUM -> 2
            FontSize.LARGE -> 4
            FontSize.EXTRA_LARGE -> 6
            else -> 0
        }

        return spToPx(fontSizePixels.toFloat(), context)
    }

    private fun initializeDateFormatters() {
        dayMonthFormat = SimpleDateFormat("dd MMM", locale)
        dayNameFormat = SimpleDateFormat("EEE", locale)
        hijriDateFormatter = DateTimeFormatter.ofPattern("dd MMM", locale).withDecimalStyle(
            DecimalStyle.of(locale)
        )
    }

    private fun initializeTimeFormat() {
        val pattern = if (state.value.twentyFourHours) "HH:mm" else "hh:mm"
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
        progressPaint.color = if (mAmbient) Color.WHITE else progressColor


        bottomSeparatorPaint.color = onBottomColor
        remainingTextPaint.color = onBottomColor
        timeLeftTextPaint.color = onBottomColor
        prayerNameTextPaint.color = onBottomColor
        prayerTimeTextPaint.color = onBottomColor

        dateTextPaint.color = foregroundColor
        hijriTextPaint.color = foregroundColor
        clockHandPaint.style = if (mAmbient) Paint.Style.STROKE else Paint.Style.FILL

        clockHandPaint.color = if (mAmbient) {
            Color.WHITE
        } else {
            hourHandColor
        }
        hourMarkerPaint.color = if (mAmbient) Color.GRAY else hourMarkerColor

    }

    private fun updateFontSizes(accu: Int) {
        dayPaint.textSize = dayPaint.textSize + accu
        timePaint.textSize = timePaint.textSize + accu
        amPmPaint.textSize = amPmPaint.textSize + accu

        dateTextPaint.textSize = dateTextPaint.textSize + accu
        hijriTextPaint.textSize = hijriTextPaint.textSize + accu

        prayerNameTextPaint.textSize = prayerNameTextPaint.textSize + accu
        prayerTimeTextPaint.textSize = prayerTimeTextPaint.textSize + accu
        remainingTextPaint.textSize = remainingTextPaint.textSize + accu
        timeLeftTextPaint.textSize = timeLeftTextPaint.textSize + accu
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
        progressPaint.isAntiAlias = bool
        clockHandPaint.isAntiAlias = bool
        hourMarkerPaint.isAntiAlias = bool
    }

    private var width = 0f
    private var height = 0f


    override fun draw(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean,
        width: Float,
        height: Float,
        drawComplications: (ids: List<Int>) -> Unit
    ) {
        this.width = width
        this.height = height
        mAmbient = isAmbient
        drawBackground(canvas, width.toInt(), height.toInt())


        val date = Date.from(zonedDateTime.toInstant())

        val todayName = dayMonthFormat.format(date)
        if (currentDate != todayName) {
            initPrayerTimes(date)
            initHijriDate(date)
            currentDate = todayName
        }



        if (!isAmbient && !state.value.removeBottomPart) {
            drawBottomArc(canvas)
        }


        val noNextPrayerToday = prayerTimes.nextPrayer() == Prayer.NONE
        val previousPrayer =
            if (noNextPrayerToday) Prayer.ISHA else prayerTimes.previousPrayer()

        if (shouldDrawElapsedTime(date, previousPrayer)) {
            drawElapsedTime(canvas, date, previousPrayer)
            drawCurrentPrayerForElapsedTime(canvas, previousPrayer)
        } else {
            if (noNextPrayerToday) {
                val nextDayDate = Date.from(zonedDateTime.toInstant().plus(1, ChronoUnit.DAYS))
                initPrayerTimes(nextDayDate)
            }
            drawTimeLeftForNextPrayer(canvas, date)
            drawPrayer(canvas)
        }

        drawBottomSeparator(canvas)

        val dayNameY = drawDayName(canvas, date)
        val dateY = drawDate(canvas, date, dayNameY)
        drawHijriDate(canvas, dateY)

        if (prayerTimes.nextPrayer() != Prayer.NONE && state.value.progressEnabled) {
            val previousPrayerTime =
                if (previousPrayer == Prayer.NONE || previousPrayer == Prayer.ISHA) {
                    getIshaaTimePreviousDay(
                        prayerTimes.timeForPrayer(previousPrayer).time,
                        coordinates,
                        prayerTimesParams
                    )
                } else {
                    prayerTimes.timeForPrayer(previousPrayer).time
                }

            drawRemainingCircle(
                canvas,
                previousPrayerTime,
                date.time,
                prayerTimes.timeForPrayer(prayerTimes.nextPrayer()).time
            )
        }

        if (state.value.complicationsEnabled) {
            val ids = arrayListOf<Int>()
            if (state.value.leftComplicationEnabled) {
                ids.add(ComplicationsIds.LEFT_COMPLICATION_ID)
            }
            if (state.value.rightComplicationEnabled) {
                ids.add(ComplicationsIds.RIGHT_COMPLICATION_ID)
            }
            if (ids.isNotEmpty()) {
                drawComplications(ids)
            }
        }


        val bounds = Rect(0, 0, width.toInt(), height.toInt())
        drawClockHands(canvas, bounds, zonedDateTime)
        drawAllHoursMarkers(
            canvas,
            bounds,
            outerCircleStokeWidthFraction = clockData.outerCircleStokeWidthFraction,
            numberStyleOuterCircleRadiusFraction = clockData.numberStyleOuterCircleRadiusFraction,
            gapBetweenOuterCircleAndBorderFraction = clockData.gapBetweenOuterCircleAndBorderFraction
        )

    }

    private fun drawBottomSeparator(canvas: Canvas) {
        val lineHeight = dpToPx(38f, context).toFloat()
        val lineY = remainingY - dpToPx(12f, context)
        canvas.withTranslation(width / 2f, lineY) {
            canvas.drawLine(0f, lineHeight, 0f, 0f, bottomSeparatorPaint)
        }
    }

    private fun drawRemainingCircle(
        canvas: Canvas,
        currentPrayerTime: Long,
        now: Long,
        nextPrayerTime: Long
    ) {


        val width = width.toFloat()
        val height = height.toFloat()

        val elapsed = now - currentPrayerTime
        val total = nextPrayerTime - currentPrayerTime
        val sweepAngle = (elapsed.toFloat() / total) * 360

        canvas.drawArc(0f, 0f, width, height, -90f, sweepAngle, false, progressPaint);


    }


    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        if (mAmbient) {
            canvas.drawRect(Rect(0, 0, width, height), ambientBackgroundPaint)
        } else {
            if (wallpaperBitmap != null) {
                canvas.drawBitmap(
                    wallpaperBitmap!!,
                    null,
                    Rect(0, 0, width, height),
                    null
                )
                mBackgroundPaint.color = Color.BLACK
                mBackgroundPaint.alpha = state.value.wallpaperOpacity
                canvas.drawRect(Rect(0, 0, width, height), mBackgroundPaint)
            } else {
                canvas.drawRect(Rect(0, 0, width, height), mBackgroundPaint)
            }
        }
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

        val width = timeLeftTextPaint.measureText(time)
        val height = time.getBounds(timeLeftTextPaint).height()


        val remainingText =
            context.getLocaleStringResource(locale, R.string.remaining)
        val remainingWidth = remainingTextPaint.measureText(remainingText)
        val remainingHeight = remainingText.getBounds(remainingTextPaint).height()
        val remainingX = this.width / 2 - remainingWidth - dpToPx(8f, context)
        remainingY = this.height - dpToPx(45f, context) - remainingHeight
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


    private fun drawElapsedTime(canvas: Canvas, date: Date, previousPrayer: Prayer) {

        val timeForPrayer = prayerTimes.timeForPrayer(previousPrayer)
        val diff = date.time - timeForPrayer.time

        var minutes = 0L
        if (diff > 0) {
            minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

            if (minutes < 0) {
                minutes = 0
            }

        }

        val time = "+" + String.format(Locale.US, "%2d", minutes)

        val width = timeLeftTextPaint.measureText(time)
        val height = time.getBounds(timeLeftTextPaint).height()

        val remainingText =
            context.getLocaleStringResource(
                locale,
                R.string.elapsed
            )
        val remainingWidth = remainingTextPaint.measureText(remainingText)
        val remainingHeight = remainingText.getBounds(remainingTextPaint).height()
        val remainingX = this.width / 2 - remainingWidth - dpToPx(8f, context)
        remainingY = this.height - dpToPx(50f, context) - remainingHeight
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

    private fun shouldDrawElapsedTime(date: Date, previousPrayer: Prayer): Boolean {
        if (state.value.elapsedTimeEnabled) {
            val timeForPrayer = prayerTimes.timeForPrayer(previousPrayer)

            val diff = date.time - timeForPrayer.time
            if (diff >= 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                if (minutes <= state.value.elapsedTimeMinutes) {
                    return true
                }
            }
        }
        return false
    }

    private fun drawCurrentPrayerForElapsedTime(canvas: Canvas, previousPrayer: Prayer) {
        val prayerName = getPrayerNameByLocaleUseCase.getPrayerNameByLocale(
            previousPrayer,
            locale
        )

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
        drawPrayerTime(previousPrayer, canvas, x, textY, prayerNameWidth)

    }

    private fun drawPrayer(canvas: Canvas) {
        val nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            return
        }

        val prayerName = getPrayerNameByLocaleUseCase.getPrayerNameByLocale(
            nextPrayer,
            locale
        )

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

    private fun drawDate(canvas: Canvas, date: Date, dayNameY: Float): Float {
        val time = dayMonthFormat.format(date)
        dateTextPaint.textAlign = Paint.Align.CENTER
        val textHeight = time.getBounds(dateTextPaint).height()
        val x = (width / 2)


        val y = dayNameY + textHeight.toFloat() + dpToPx(20f, context).toFloat()
        canvas.drawText(
            time,
            x,
            y,
            dateTextPaint
        )
        return y
    }

    private fun drawHijriDate(canvas: Canvas, dateY: Float) {
        val hijriDateStr =
            hijriDate.plus(state.value.hijriOffset.toLong(), ChronoUnit.DAYS)
                .format(hijriDateFormatter)

        hijriTextPaint.textAlign = Paint.Align.CENTER

        val x = width / 2
        val textHeight = hijriDateStr.getBounds(hijriTextPaint).height()
        val y = dateY + textHeight + dpToPx(8f, context).toFloat()

        canvas.drawText(
            hijriDateStr,
            x,
            y,
            hijriTextPaint
        )
    }

    private fun drawBottomArc(canvas: Canvas) {
        val rectHeight = height * 0.34f
        canvas.withTranslation(y = height - rectHeight) {
            val rectangle = RectF(0f, 0f, this@AnalogWatchFacePainter.width, rectHeight)
            canvas.drawRect(rectangle, bottomArcPaint)
        }
    }

    private fun drawDayName(canvas: Canvas, date: Date): Float {
        val date = dayNameFormat.format(date).uppercase()

        val height = date.getBounds(dayPaint).height()


        val centerX = width / 2f
        val width = dayPaint.measureText(date)
        val x = centerX - width / 2


        val y = height + dpToPx(18f, context).toFloat()
        canvas.drawText(
            date,
            x,
            y,
            dayPaint
        )
        return y
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
        backgroundColor = context.getColor(DefaultWatchFaceColors.BACKGROUND_COLOR)
        backgroundColorBottomPart =
            context.getColor(DefaultWatchFaceColors.BACKGROUND_COLOR_BOTTOM_PART)
        mainForegroundColor = context.getColor(DefaultWatchFaceColors.MAIN_FOREGROUND_COLOR)
        onBottomForegroundColor =
            context.getColor(DefaultWatchFaceColors.ON_BOTTOM_FOREGROUND_COLOR)
        ambientForegroundColor = Color.WHITE
        greyTextColor = Color.GRAY
        progressColor = context.getColor(DefaultWatchFaceColors.PROGRESS_COLOR)
        hourHandColor = context.getColor(DefaultWatchFaceColors.PRIMARY_HAND_COLOR)
        secondsHandColor = context.getColor(DefaultWatchFaceColors.SECONDARY_HAND_COLOR)
        hourMarkerColor = context.getColor(DefaultWatchFaceColors.HOUR_MARKER_COLOR)
    }


    private fun initializePaint() {
        mBackgroundPaint = Paint().apply {
            color = backgroundColor
        }

        ambientBackgroundPaint = Paint().apply {
            color = Color.BLACK
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

        progressPaint = Paint().apply {
            color = progressColor
            style = Paint.Style.STROKE
            strokeWidth = 15f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        clockHandPaint = Paint().apply {
            color = hourHandColor
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
            strokeWidth =
                dpToPx(2f, context).toFloat()
        }

        hourMarkerPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
            color = hourMarkerColor
        }

        secondsHandPaint = Paint().apply {
            color = secondsHandColor
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
            strokeWidth =
                dpToPx(2f, context).toFloat()
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
                Typeface.create(
                    ResourcesCompat.getFont(context, R.font.cairo),
                    Typeface.BOLD
                )
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

    override fun onDestroy() {
        scope.cancel("scope clear() request")
    }

    override fun updateAmbient(isAmbient: Boolean) {
        mAmbient = isAmbient
        updateWatchAmbient()
    }


    private fun drawClockHands(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
    ) {
        // Only recalculate bounds (watch face size/surface) has changed or the arm of one of the
        // clock hands has changed (via user input in the settings).
        // NOTE: Watch face surface usually only updates one time (when the size of the device is
        // initially broadcasted).
        if (currentWatchFaceSize != bounds || armLengthChangedRecalculateClockHands) {
            armLengthChangedRecalculateClockHands = false
            currentWatchFaceSize = bounds
            recalculateClockHands(bounds)
        }

        // Retrieve current time to calculate location/rotation of watch arms.
        val secondOfDay = zonedDateTime.toLocalTime().toSecondOfDay()

        // Determine the rotation of the hour and minute hand.

        // Determine how many seconds it takes to make a complete rotation for each hand
        // It takes the hour hand 12 hours to make a complete rotation
        val secondsPerHourHandRotation = Duration.ofHours(12).seconds
        // It takes the minute hand 1 hour to make a complete rotation
        val secondsPerMinuteHandRotation = Duration.ofHours(1).seconds

        // Determine the angle to draw each hand expressed as an angle in degrees from 0 to 360
        // Since each hand does more than one cycle a day, we are only interested in the remainder
        // of the secondOfDay modulo the hand interval
        val hourRotation = secondOfDay.rem(secondsPerHourHandRotation) * 360.0f /
                secondsPerHourHandRotation
        val minuteRotation = secondOfDay.rem(secondsPerMinuteHandRotation) * 360.0f /
                secondsPerMinuteHandRotation

        canvas.withScale(
            x = WATCH_HAND_SCALE,
            y = WATCH_HAND_SCALE,
            pivotX = bounds.exactCenterX(),
            pivotY = bounds.exactCenterY()
        ) {


            // Draw hour hand.
            withRotation(hourRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                drawPath(hourHandBorder, clockHandPaint)
            }


            // Draw minute hand.
            withRotation(minuteRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                drawPath(minuteHandBorder, clockHandPaint)
            }

            // Draw second hand if not in ambient mode
            if (!mAmbient) {

                // Second hand has a different color style (secondary color) and is only drawn in
                // active mode, so we calculate it here (not above with others).
                val secondsPerSecondHandRotation = Duration.ofMinutes(1).seconds
                val secondsRotation = secondOfDay.rem(secondsPerSecondHandRotation) * 360.0f /
                        secondsPerSecondHandRotation
                withRotation(secondsRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                    drawPath(secondHand, secondsHandPaint)
                }
            }
        }
    }

    private fun drawAllHoursMarkers(
        canvas: Canvas,
        bounds: Rect,
        outerCircleStokeWidthFraction: Float,
        numberStyleOuterCircleRadiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float
    ) {

        // Draws dots for the remain hour indicators between the numbers above.
        hourMarkerPaint.strokeWidth = outerCircleStokeWidthFraction * bounds.width()
        canvas.save()
        for (i in 0 until 12) {
            drawHourMarker(
                canvas,
                bounds,
                numberStyleOuterCircleRadiusFraction,
                gapBetweenOuterCircleAndBorderFraction,
                isLonger = i % 3 == 0
            )

            canvas.rotate(360.0f / 12.0f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restore()
    }


    /*
     * Rarely called (only when watch face surface changes; usually only once) from the
     * drawClockHands() method.
     */
    private fun recalculateClockHands(bounds: Rect) {
        hourHandBorder =
            createClockHand(
                bounds,
                clockData.hourHandDimensions.lengthFraction,
                clockData.hourHandDimensions.widthFraction,
                clockData.gapBetweenHandAndCenterFraction,
                clockData.hourHandDimensions.xRadiusRoundedCorners,
                clockData.hourHandDimensions.yRadiusRoundedCorners
            )

        hourHandBorderRect = createClockHandRect(
            bounds,
            clockData.hourHandDimensions.lengthFraction,
            clockData.hourHandDimensions.widthFraction,
            clockData.gapBetweenHandAndCenterFraction,
            clockData.hourHandDimensions.xRadiusRoundedCorners,
            clockData.hourHandDimensions.yRadiusRoundedCorners
        )
        hourHandFill = hourHandBorder

        minuteHandBorder =
            createClockHand(
                bounds,
                clockData.minuteHandDimensions.lengthFraction,
                clockData.minuteHandDimensions.widthFraction,
                clockData.gapBetweenHandAndCenterFraction,
                clockData.minuteHandDimensions.xRadiusRoundedCorners,
                clockData.minuteHandDimensions.yRadiusRoundedCorners
            )
        minuteHandFill = minuteHandBorder

        secondHand =
            createClockHand(
                bounds,
                clockData.secondHandDimensions.lengthFraction,
                clockData.secondHandDimensions.widthFraction,
                clockData.gapBetweenHandAndCenterFraction,
                clockData.secondHandDimensions.xRadiusRoundedCorners,
                clockData.secondHandDimensions.yRadiusRoundedCorners
            )
    }

    /**
     * Returns a round rect clock hand if {@code rx} and {@code ry} equals to 0, otherwise return a
     * rect clock hand.
     *
     * @param bounds The bounds use to determine the coordinate of the clock hand.
     * @param length Clock hand's length, in fraction of {@code bounds.width()}.
     * @param thickness Clock hand's thickness, in fraction of {@code bounds.width()}.
     * @param gapBetweenHandAndCenter Gap between inner side of arm and center.
     * @param roundedCornerXRadius The x-radius of the rounded corners on the round-rectangle.
     * @param roundedCornerYRadius The y-radius of the rounded corners on the round-rectangle.
     */
    private fun createClockHand(
        bounds: Rect,
        length: Float,
        thickness: Float,
        gapBetweenHandAndCenter: Float,
        roundedCornerXRadius: Float,
        roundedCornerYRadius: Float
    ): Path {
        val width = bounds.width()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val left = centerX - thickness / 2 * width
        val top = centerY - (gapBetweenHandAndCenter + length) * width
        val right = centerX + thickness / 2 * width
        val bottom = centerY - gapBetweenHandAndCenter * width

        val path = Path()

        path.addRoundRect(
            left,
            top,
            right,
            bottom,
            roundedCornerXRadius,
            roundedCornerYRadius,
            Path.Direction.CW
        )

        return path
    }

    private fun createClockHandRect(
        bounds: Rect,
        length: Float,
        thickness: Float,
        gapBetweenHandAndCenter: Float,
        roundedCornerXRadius: Float,
        roundedCornerYRadius: Float
    ): HandRect {
        val width = bounds.width()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val left = centerX - thickness / 2 * width
        val top = centerY - (gapBetweenHandAndCenter + length) * width
        val right = centerX + thickness / 2 * width
        val bottom = centerY - gapBetweenHandAndCenter * width

        return HandRect(
            Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()),
            roundedCornerXRadius,
            roundedCornerYRadius
        )
    }

    /** Draws the outer circle on the top middle of the given bounds. */
    private fun drawHourMarker(
        canvas: Canvas,
        bounds: Rect,
        radiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float,
        isLonger: Boolean
    ) {

        // X and Y coordinates of the center of the circle.
        val centerX = 0.5f * bounds.width().toFloat()
        val centerY = bounds.width() * (gapBetweenOuterCircleAndBorderFraction + radiusFraction)

        //draw hour lines
        val lineLength = dpToPx(10f, context).toFloat()
        val lineY = centerY - radiusFraction * bounds.width()
        val lineHeight = if (isLonger) dpToPx(7f, context).toFloat() else 0f
        canvas.drawLine(centerX, lineY + lineHeight, centerX, lineY - lineLength, hourMarkerPaint)
    }

    companion object {
        // Used to canvas.scale() to scale watch hands in proper bounds. This will always be 1.0.
        private const val WATCH_HAND_SCALE = 1.0f
    }


}