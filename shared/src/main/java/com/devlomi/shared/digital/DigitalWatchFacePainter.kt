package com.devlomi.shared.digital

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withTranslation
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.devlomi.shared.R
import com.devlomi.shared.WatchFacePainter
import com.devlomi.shared.config.PrayerConfigState
import com.devlomi.shared.config.SettingsDataStore
import com.devlomi.shared.config.offsetWithDaylight
import com.devlomi.shared.constants.ComplicationsIds
import com.devlomi.shared.constants.DefaultWatchFaceColors
import com.devlomi.shared.constants.FontSize
import com.devlomi.shared.common.dpToPx
import com.devlomi.shared.common.getBounds
import com.devlomi.shared.common.getIshaaTimePreviousDay
import com.devlomi.shared.common.getLocaleStringResource
import com.devlomi.shared.locale.GetPrayerNameByLocaleUseCase
import com.devlomi.shared.locale.LocaleHelper
import com.devlomi.shared.locale.LocaleType
import com.devlomi.shared.common.previousPrayer
import com.devlomi.shared.common.spToPx
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit


class DigitalWatchFacePainter(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val getPrayerNameByLocaleUseCase: GetPrayerNameByLocaleUseCase
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
    private var progressColor = -1

    private var mainForegroundColor = -1
    private var onBottomForegroundColor = -1
    private var ambientForegroundColor = -1
    private var greyTextColor = -1

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
    private var locale: Locale = Locale.US
    private var wallpaperBitmap: Bitmap? = null

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
                        this@DigitalWatchFacePainter.locale = LocaleHelper.getLocale(locale)
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

    private fun initializeDateFormatters() {
        dayMonthFormat = SimpleDateFormat("dd MMM", locale)
        dayNameFormat = SimpleDateFormat("EEE", locale)
        hijriDateFormatter = DateTimeFormatter.ofPattern("dd MMM", locale).withDecimalStyle(
            DecimalStyle.of(locale)
        )
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

        bottomSeparatorPaint.color = onBottomColor
        remainingTextPaint.color = onBottomColor
        timeLeftTextPaint.color = onBottomColor
        prayerNameTextPaint.color = onBottomColor
        prayerTimeTextPaint.color = onBottomColor

        dateTextPaint.color = foregroundColor
        hijriTextPaint.color = foregroundColor
        progressPaint.color = if (mAmbient) Color.WHITE else progressColor


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


        drawCurrentTime(canvas, date)

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

        drawDate(canvas, date)
        drawHijriDate(canvas)

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

        drawDayName(canvas, date)

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


    private fun drawCurrentTime(canvas: Canvas, date: Date) {
        val time = timeFormat.format(date)
        val centerX = width / 2f
        val width = timePaint.measureText(time)
        val percentage = if (state.value.complicationsEnabled) 0.30f else 0.33f
        val y = height * percentage
        val x = centerX - width / 2
        canvas.drawText(time, x, y, timePaint)
        if (!state.value.twentyFourHours) {
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


        val remainingText = context.getLocaleStringResource(locale, R.string.remaining)
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

    private fun drawDate(canvas: Canvas, date: Date) {
        val time = dayMonthFormat.format(date)
        val textHeight = time.getBounds(dateTextPaint).height()
        val x = dpToPx(16f, context).toFloat()

        val percentage = if (state.value.complicationsEnabled) 2.3f else 2f

        val y = height / percentage + (textHeight / 2f)
        canvas.drawText(
            time,
            x,
            y,
            dateTextPaint
        )
    }

    private fun drawHijriDate(canvas: Canvas) {
        val hijriDateStr =
            hijriDate.plus(state.value.hijriOffset.toLong(), ChronoUnit.DAYS)
                .format(hijriDateFormatter)

        val textWidth = hijriTextPaint.measureText(hijriDateStr)
        val x = width - textWidth - dpToPx(5f, context)
        val textHeight = hijriDateStr.getBounds(hijriTextPaint).height()
        val percentage = if (state.value.complicationsEnabled) 2.3f else 2f
        val y = height / percentage + (textHeight / 2f)

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
            val rectangle = RectF(0f, 0f, this@DigitalWatchFacePainter.width, rectHeight)
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
        backgroundColor = context.getColor(DefaultWatchFaceColors.BACKGROUND_COLOR)
        backgroundColorBottomPart =
            context.getColor(DefaultWatchFaceColors.BACKGROUND_COLOR_BOTTOM_PART)
        mainForegroundColor = context.getColor(DefaultWatchFaceColors.MAIN_FOREGROUND_COLOR)
        onBottomForegroundColor =
            context.getColor(DefaultWatchFaceColors.ON_BOTTOM_FOREGROUND_COLOR)
        ambientForegroundColor = Color.WHITE
        greyTextColor = Color.GRAY
        progressColor = context.getColor(DefaultWatchFaceColors.PROGRESS_COLOR)
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

    override fun onDestroy() {
        scope.cancel("scope clear() request")
    }

    override fun updateAmbient(isAmbient: Boolean) {
        mAmbient = isAmbient
        updateWatchAmbient()
    }


}