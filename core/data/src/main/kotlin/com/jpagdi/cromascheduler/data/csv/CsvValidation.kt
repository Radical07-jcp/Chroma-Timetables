package com.jpagdi.cromascheduler.data.csv

data class CsvValidationError(
    val fileName: String,
    val rowNumber: Int, // 1-based, counting the header as row 0 so this matches what a user sees in a spreadsheet minus header
    val message: String,
)

data class ParsedFile<T>(
    val records: List<T>,
    val errors: List<CsvValidationError>,
)

/** Small helpers shared by every per-file parser below, to keep error messages consistent. */
internal object CsvValidation {
    fun requireField(
        row: Map<String, String>,
        column: String,
        fileName: String,
        rowNumber: Int,
        errors: MutableList<CsvValidationError>,
    ): String? {
        val value = row[column]
        if (value.isNullOrBlank()) {
            errors.add(CsvValidationError(fileName, rowNumber, "Missing required value for column \"$column\""))
            return null
        }
        return value
    }

    fun requireInt(
        row: Map<String, String>,
        column: String,
        fileName: String,
        rowNumber: Int,
        errors: MutableList<CsvValidationError>,
    ): Int? {
        val raw = requireField(row, column, fileName, rowNumber, errors) ?: return null
        val parsed = raw.toIntOrNull()
        if (parsed == null) {
            errors.add(CsvValidationError(fileName, rowNumber, "Column \"$column\" must be a whole number, got \"$raw\""))
        }
        return parsed
    }

    fun optionalInt(
        row: Map<String, String>,
        column: String,
        fileName: String,
        rowNumber: Int,
        errors: MutableList<CsvValidationError>,
    ): Int? {
        val raw = row[column]
        if (raw.isNullOrBlank()) return null
        val parsed = raw.toIntOrNull()
        if (parsed == null) {
            errors.add(CsvValidationError(fileName, rowNumber, "Column \"$column\" must be a whole number if present, got \"$raw\""))
        }
        return parsed
    }
}
