package com.jpagdi.cromascheduler.data.export

class CsvScheduleExporter {
    private val header = listOf("Session ID", "Type", "Subject", "Teacher", "Section", "Room", "Day", "Start", "End")

    fun export(rows: List<ScheduleExportRow>): String {
        val sb = StringBuilder()
        sb.appendLine(header.joinToString(",") { csvField(it) })
        rows.forEach { row ->
            sb.appendLine(
                listOf(
                    row.sessionId, row.sessionType, row.subjectName, row.teacherName,
                    row.sectionName, row.roomName, row.dayLabel, row.startTime, row.endTime,
                ).joinToString(",") { csvField(it) },
            )
        }
        return sb.toString()
    }

    private fun csvField(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
