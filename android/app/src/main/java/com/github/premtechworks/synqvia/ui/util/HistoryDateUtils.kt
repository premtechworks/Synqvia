package com.github.premtechworks.synqvia.ui.util

import com.github.premtechworks.synqvia.data.ClipEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class DateGroup(
    val header: String,
    val date: LocalDate,
    val clips: List<ClipEntity>
)

private val DATE_HEADER_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val CLOCK_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/**
 * Returns "Today", "Yesterday", or "MMM d" (e.g. "Sep 28") for the given timestamp.
 */
fun formatGroupDateHeader(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val clipDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()

    val daysBetween = ChronoUnit.DAYS.between(clipDate, today)

    return when (daysBetween) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> clipDate.format(DATE_HEADER_FORMATTER)
    }
}

/**
 * Returns relative time ("just now", "22m ago", "1h ago", "2h ago") if today,
 * or clock time like "10:24 PM" for yesterday and older items.
 */
fun formatHistoryItemTime(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val clipDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()

    val isToday = clipDate == today
    val diffMillis = now - timestamp

    return if (isToday) {
        val seconds = (diffMillis / 1000).coerceAtLeast(0)
        val minutes = seconds / 60
        val hours = minutes / 60

        when {
            seconds < 60 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            else -> "${hours}h ago"
        }
    } else {
        Instant.ofEpochMilli(timestamp).atZone(zoneId).format(CLOCK_TIME_FORMATTER)
    }
}

/**
 * Groups clips by calendar date into DateGroup items, maintaining order.
 */
fun groupClipsByDate(
    clips: List<ClipEntity>,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<DateGroup> {
    if (clips.isEmpty()) return emptyList()

    val grouped = linkedMapOf<LocalDate, MutableList<ClipEntity>>()

    for (clip in clips) {
        val date = Instant.ofEpochMilli(clip.ts).atZone(zoneId).toLocalDate()
        grouped.getOrPut(date) { mutableListOf() }.add(clip)
    }

    return grouped.map { (date, groupClips) ->
        val firstTs = groupClips.first().ts
        DateGroup(
            header = formatGroupDateHeader(firstTs, now, zoneId),
            date = date,
            clips = groupClips
        )
    }
}

private val headerCache = java.util.concurrent.ConcurrentHashMap<LocalDate, String>()

/**
 * Builds immutable HistoryGroupUi items off the main thread with cached date header formatting.
 */
fun buildHistoryGroups(
    clips: List<ClipEntity>,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<com.github.premtechworks.synqvia.ui.screens.HistoryGroupUi> {
    if (clips.isEmpty()) return emptyList()

    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
    val yesterday = today.minusDays(1)

    val grouped = linkedMapOf<LocalDate, MutableList<com.github.premtechworks.synqvia.ui.screens.HistoryItemUi>>()

    for (clip in clips) {
        val date = Instant.ofEpochMilli(clip.ts).atZone(zoneId).toLocalDate()
        val formattedTime = formatHistoryItemTime(clip.ts, now, zoneId)
        val urlHost = UrlUtil.extractHost(clip.text)
        grouped.getOrPut(date) { mutableListOf() }.add(
            com.github.premtechworks.synqvia.ui.screens.HistoryItemUi(
                clip = clip,
                formattedTime = formattedTime,
                urlHost = urlHost
            )
        )
    }

    return grouped.map { (date, items) ->
        val header = when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> headerCache.getOrPut(date) {
                date.format(DATE_HEADER_FORMATTER)
            }
        }
        com.github.premtechworks.synqvia.ui.screens.HistoryGroupUi(
            header = header,
            date = date,
            items = items
        )
    }
}

