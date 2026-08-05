package com.jpagdi.cromascheduler.data.csv

/**
 * Minimal RFC-4180-ish CSV parser: handles quoted fields, escaped quotes (""), and
 * commas/newlines inside quotes. Written by hand rather than pulling in a CSV
 * library — the format we need to support is small and fixed (our own 6 file
 * schemas below), so a dependency isn't justified for this.
 *
 * Returns a list of rows, each row a List<String> in column order. Caller is
 * responsible for treating the first row as a header (see parseTable below).
 */
object CsvParser {
    fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        val normalized = text.replace("\r\n", "\n").replace("\r", "\n")

        fun endField() {
            row.add(field.toString())
            field = StringBuilder()
        }

        fun endRow() {
            endField()
            // Skip fully blank rows (common trailing newline at end of file, or blank
            // separator rows some spreadsheet exports leave behind).
            if (row.size > 1 || row.firstOrNull()?.isNotBlank() == true) {
                rows.add(row)
            }
            row = mutableListOf()
        }

        while (i < normalized.length) {
            val c = normalized[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < normalized.length && normalized[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> endField()
                c == '\n' -> endRow()
                else -> field.append(c)
            }
            i++
        }
        // Final field/row if the text didn't end on a newline.
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()

        return rows
    }

    /**
     * Parses [text] as a header + data table and returns each data row as a
     * header-name -> value map, so per-file parsers below can look up columns by
     * name instead of brittle positional indices (CSV exports don't always agree
     * on column order).
     */
    fun parseTable(text: String): List<Map<String, String>> {
        val rows = parseRows(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1).map { row ->
            header.indices.associate { idx -> header[idx] to (row.getOrNull(idx)?.trim().orEmpty()) }
        }
    }
}
