package com.jpagdi.cromascheduler.ui

import com.jpagdi.cromascheduler.data.entity.PeriodBlock

private fun formatTime(minutesSinceMidnight: Int): String {
    val h24 = (minutesSinceMidnight / 60) % 24
    val m = minutesSinceMidnight % 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    val suffix = if (h24 < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, m, suffix)
}

/** "7:30 AM • 60min × 8" for one block, "3 blocks" for more than one — used anywhere a run's period setup needs to fit on one line (Home's list, Timetable Detail's header). */
fun List<PeriodBlock>.summary(): String {
    if (isEmpty()) return "No periods defined"
    if (size == 1) {
        val b = single()
        return "${formatTime(b.startMinutesSinceMidnight)} • ${b.periodDurationMinutes}min × ${b.periodCount}"
    }
    return "$size blocks (${joinToString(", ") { it.label }})"
}
