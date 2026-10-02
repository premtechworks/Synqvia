package com.github.premtechworks.synqvia.ui.util

import com.github.premtechworks.synqvia.data.ClipEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryDateUtilsTest {

    private val zoneId = ZoneId.of("UTC")
    // Reference time: 2026-10-02 12:00:00 UTC
    private val nowDateTime = ZonedDateTime.of(2026, 10, 2, 12, 0, 0, 0, zoneId)
    private val nowMillis = nowDateTime.toInstant().toEpochMilli()

    @Test
    fun formatGroupDateHeader_returnsTodayForSameDay() {
        val todayMillis = nowDateTime.minusHours(2).toInstant().toEpochMilli()
        val header = formatGroupDateHeader(todayMillis, nowMillis, zoneId)
        assertEquals("Today", header)
    }

    @Test
    fun formatGroupDateHeader_returnsYesterdayForPreviousDay() {
        val yesterdayMillis = nowDateTime.minusDays(1).toInstant().toEpochMilli()
        val header = formatGroupDateHeader(yesterdayMillis, nowMillis, zoneId)
        assertEquals("Yesterday", header)
    }

    @Test
    fun formatGroupDateHeader_returnsFormattedDateForOlderDay() {
        // 2026-09-28
        val olderDateTime = ZonedDateTime.of(2026, 9, 28, 10, 0, 0, 0, zoneId)
        val olderMillis = olderDateTime.toInstant().toEpochMilli()
        val header = formatGroupDateHeader(olderMillis, nowMillis, zoneId)
        assertEquals("Sep 28", header)
    }

    @Test
    fun formatHistoryItemTime_returnsJustNowForRecentItem() {
        val recentMillis = nowMillis - 10_000 // 10s ago
        val timeStr = formatHistoryItemTime(recentMillis, nowMillis, zoneId)
        assertEquals("just now", timeStr)
    }

    @Test
    fun formatHistoryItemTime_returnsMinutesAgoForSameDayItem() {
        val twentyTwoMinAgo = nowMillis - (22 * 60 * 1000)
        val timeStr = formatHistoryItemTime(twentyTwoMinAgo, nowMillis, zoneId)
        assertEquals("22m ago", timeStr)
    }

    @Test
    fun formatHistoryItemTime_returnsHoursAgoForSameDayItem() {
        val twoHoursAgo = nowMillis - (2 * 60 * 60 * 1000)
        val timeStr = formatHistoryItemTime(twoHoursAgo, nowMillis, zoneId)
        assertEquals("2h ago", timeStr)
    }

    @Test
    fun formatHistoryItemTime_returnsClockTimeForYesterdayItem() {
        val yesterdayNight = ZonedDateTime.of(2026, 10, 1, 22, 24, 0, 0, zoneId).toInstant().toEpochMilli()
        val timeStr = formatHistoryItemTime(yesterdayNight, nowMillis, zoneId)
        assertEquals("10:24 PM", timeStr)
    }

    @Test
    fun groupClipsByDate_groupsCorrectlyMaintainingOrder() {
        val clipToday1 = ClipEntity("1", "text 1", nowMillis - 1000, "prem-pc", "remote")
        val clipToday2 = ClipEntity("2", "text 2", nowMillis - 3600_000, "android", "local")
        val clipYesterday = ClipEntity("3", "text 3", nowDateTime.minusDays(1).toInstant().toEpochMilli(), "prem-pc", "remote")
        val clipOlder = ClipEntity("4", "text 4", ZonedDateTime.of(2026, 9, 28, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli(), "prem-pc", "remote")

        val groups = groupClipsByDate(listOf(clipToday1, clipToday2, clipYesterday, clipOlder), nowMillis, zoneId)

        assertEquals(3, groups.size)
        assertEquals("Today", groups[0].header)
        assertEquals(2, groups[0].clips.size)

        assertEquals("Yesterday", groups[1].header)
        assertEquals(1, groups[1].clips.size)

        assertEquals("Sep 28", groups[2].header)
        assertEquals(1, groups[2].clips.size)
    }
}
