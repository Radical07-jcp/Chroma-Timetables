package com.jpagdi.cromascheduler.data.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * An .xlsx file is just a ZIP of a handful of XML parts — small enough that hand-
 * rolling the minimal valid structure avoids adding Apache POI (a large dependency,
 * and one that would need explicit approval per the standing "no new external
 * dependencies without approval" rule) for what is otherwise one flat table with no
 * formulas, styles, or multiple sheets. Cells use inline strings (t="inlineStr")
 * specifically so a sharedStrings.xml part isn't needed either — one less moving
 * piece that has to stay correctly cross-referenced.
 */
class XlsxScheduleExporter {
    private val header = listOf("Session ID", "Type", "Subject", "Teacher", "Section", "Room", "Day", "Start", "End")

    fun export(rows: List<ScheduleExportRow>): ByteArray {
        val sheetXml = buildSheetXml(rows)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            writeEntry(zip, "_rels/.rels", RELS_XML)
            writeEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return baos.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildSheetXml(rows: List<ScheduleExportRow>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        sb.append(rowXml(1, header))
        rows.forEachIndexed { index, row ->
            sb.append(
                rowXml(
                    index + 2,
                    listOf(row.sessionId, row.sessionType, row.subjectName, row.teacherName, row.sectionName, row.roomName, row.dayLabel, row.startTime, row.endTime),
                ),
            )
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun rowXml(rowIndex: Int, values: List<String>): String {
        val cells = values.mapIndexed { colIndex, value -> cellXml(colIndex, rowIndex, value) }.joinToString("")
        return "<row r=\"$rowIndex\">$cells</row>"
    }

    private fun cellXml(colIndex: Int, rowIndex: Int, value: String): String {
        val ref = "${columnLetter(colIndex)}$rowIndex"
        return "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXml(value)}</t></is></c>"
    }

    /** 0-based column index -> spreadsheet letters (0->A, 25->Z, 26->AA, ...). */
    private fun columnLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
            if (i < 0) break
        }
        return sb.toString()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    companion object {
        private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        private const val RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Schedule" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

        private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
    }
}
