package com.jpagdi.cromascheduler.data.csv

import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Reads a ZIP archive (e.g. all six CSVs bundled together) and returns each .csv
 * entry's text content keyed by its filename (path stripped, so "data/teachers.csv"
 * and "teachers.csv" both key to "teachers.csv" — some phone zip tools nest files in
 * a folder without the user realizing it).
 */
object ZipCsvReader {
    fun readCsvEntries(input: InputStream): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.substringAfterLast('/').endsWith(".csv", ignoreCase = true)) {
                    val fileName = entry.name.substringAfterLast('/')
                    result[fileName] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }
}
