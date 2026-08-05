package com.jpagdi.cromascheduler.data.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a simple paginated table using android.graphics.pdf.PdfDocument — part of
 * the Android framework, not a library dependency. Also what "print-friendly
 * timetable" (spec, Export section) reuses: the same PDF gets handed to
 * PrintManager by the :app layer's print flow rather than building a second,
 * separate print renderer.
 */
class PdfScheduleExporter {
    private val header = listOf("Session", "Type", "Subject", "Teacher", "Section", "Room", "Day", "Start", "End")
    private val pageWidth = 842 // A4 landscape @ 72dpi
    private val pageHeight = 595
    private val margin = 24f
    private val rowHeight = 20f

    fun export(title: String, rows: List<ScheduleExportRow>, outputFile: File): File {
        val document = PdfDocument()
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
        val cellPaint = Paint().apply { textSize = 9f }
        val titlePaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val columnWidths = computeColumnWidths()

        var rowIndex = 0
        var pageNumber = 1
        do {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var y = margin

            canvas.drawText(if (pageNumber == 1) title else "$title (cont.)", margin, y + 12f, titlePaint)
            y += 28f

            var x = margin
            header.forEachIndexed { i, h -> canvas.drawText(h, x, y, headerPaint); x += columnWidths[i] }
            y += rowHeight

            while (rowIndex < rows.size && y < pageHeight - margin) {
                val row = rows[rowIndex]
                val values = listOf(
                    row.sessionId, row.sessionType, row.subjectName, row.teacherName,
                    row.sectionName, row.roomName, row.dayLabel, row.startTime, row.endTime,
                )
                x = margin
                values.forEachIndexed { i, v -> canvas.drawText(truncate(v, columnWidths[i], cellPaint), x, y, cellPaint); x += columnWidths[i] }
                y += rowHeight
                rowIndex++
            }

            document.finishPage(page)
            pageNumber++
        } while (rowIndex < rows.size)

        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        return outputFile
    }

    private fun computeColumnWidths(): List<Float> {
        val usable = pageWidth - margin * 2
        val weights = listOf(1.2f, 0.8f, 1.2f, 1.2f, 1.0f, 0.8f, 1.0f, 0.7f, 0.7f)
        val totalWeight = weights.sum()
        return weights.map { usable * (it / totalWeight) }
    }

    private fun truncate(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
