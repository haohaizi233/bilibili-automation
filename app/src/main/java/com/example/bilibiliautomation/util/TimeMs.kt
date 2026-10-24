package com.example.bilibiliautomation.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeMs {
    val BEIJING: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")
    private const val MINUTE_MS = 60_000L

    fun now(): Long {
        return try {
            android.os.SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.nanoTime() / 1_000_000L
        }
    }

    fun wallClock(): Long = System.currentTimeMillis()

    fun formatBeijing(epochMs: Long = wallClock(), pattern: String): String {
        val fmt = SimpleDateFormat(pattern, Locale.CHINA)
        fmt.timeZone = BEIJING
        return fmt.format(Date(epochMs))
    }

    fun beijingDayStart(epochMs: Long = wallClock()): Long {
        val cal = Calendar.getInstance(BEIJING, Locale.CHINA)
        cal.timeInMillis = epochMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun nextBeijingDayStart(epochMs: Long = wallClock()): Long {
        val cal = Calendar.getInstance(BEIJING, Locale.CHINA)
        cal.timeInMillis = beijingDayStart(epochMs)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    fun millisUntilNextBeijingMidnight(epochMs: Long = wallClock()): Long {
        return (nextBeijingDayStart(epochMs) - epochMs).coerceAtLeast(0L)
    }

    fun beijingDayPollIntervalMs(epochMs: Long = wallClock()): Long {
        return minOf(MINUTE_MS, millisUntilNextBeijingMidnight(epochMs).coerceAtLeast(1_000L))
    }
}
