package com.devlomi.shared.usecase

import androidx.core.graphics.withTranslation
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.devlomi.shared.R
import com.devlomi.shared.common.dpToPx
import com.devlomi.shared.common.getBounds
import com.devlomi.shared.common.getLocaleStringResource
import java.util.Date
import java.util.Locale

class GetTimeLeftForNextPrayerUseCase {
    //TODO DELETE IF NOT NEEDED
    fun getTimeLeftForNextPrayer(prayerTimes: PrayerTimes, date: Date): String? {
        val nextPrayer = prayerTimes.nextPrayer()
        if (nextPrayer == Prayer.NONE) {
            return null
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

        return String.format(Locale.US, "%2d:%02d", hours, minutes)

    }
}