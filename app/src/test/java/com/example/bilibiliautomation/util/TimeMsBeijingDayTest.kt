package com.example.bilibiliautomation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TimeMsBeijingDayTest {
    @Test
    fun beijingDayStartsAtMidnightAndRollsAtNextMidnight() {
        val beforeMidnight = parse("2026-09-19 23:59:59.999")
        val atMidnight = parse("2026-09-20 00:00:00.000")
        val morning = parse("2026-09-20 00:00:00.001")

        val day19 = TimeMs.beijingDayStart(beforeMidnight)
        val day20 = TimeMs.beijingDayStart(atMidnight)

        assertEquals(parse("2026-09-19 00:00:00.000"), day19)
        assertEquals(parse("2026-09-20 00:00:00.000"), day20)
        assertEquals(day20, TimeMs.beijingDayStart(morning))
        assertEquals(day20, TimeMs.nextBeijingDayStart(beforeMidnight))
        assertTrue(day20 > day19)
        assertEquals(1L, TimeMs.millisUntilNextBeijingMidnight(beforeMidnight))
    }

    @Test
    fun sameBeijingCalendarDaySharesTheSameStart() {
        val evening = parse("2026-09-19 22:34:00.000")
        val early = parse("2026-09-19 00:00:00.000")
        assertEquals(TimeMs.beijingDayStart(early), TimeMs.beijingDayStart(evening))
        assertEquals("2026-09-19", TimeMs.formatBeijing(TimeMs.beijingDayStart(evening), "yyyy-MM-dd"))
    }

    private fun parse(text: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)
        fmt.timeZone = TimeMs.BEIJING
        return fmt.parse(text)!!.time
    }
}
