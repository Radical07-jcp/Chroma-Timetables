package com.jpagdi.cromascheduler.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.export.CsvScheduleExporter
import com.jpagdi.cromascheduler.data.export.PdfScheduleExporter
import com.jpagdi.cromascheduler.data.export.XlsxScheduleExporter
import com.jpagdi.cromascheduler.data.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ExportFormat(val label: String, val extension: String) {
    CSV("CSV", "csv"),
    EXCEL("Excel", "xlsx"),
    PDF("PDF", "pdf"),
    PRINT("Print-friendly", "pdf"), // print reuses the PDF renderer — see PdfScheduleExporter's doc comment
}

class ExportViewModel(
    private val repository: ScheduleRepository,
    private val csvExporter: CsvScheduleExporter,
    private val xlsxExporter: XlsxScheduleExporter,
    private val pdfExporter: PdfScheduleExporter,
) : ViewModel() {
    var exportedFile by mutableStateOf<File?>(null)
        private set
    var isExporting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun export(context: Context, runId: String, runName: String, format: ExportFormat) {
        isExporting = true
        errorMessage = null
        exportedFile = null
        viewModelScope.launch {
            runCatching {
                val rows = repository.buildExportRows(runId)
                withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                    val safeName = runName.ifBlank { "schedule" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val outFile = File(dir, "$safeName.${format.extension}")
                    when (format) {
                        ExportFormat.CSV -> outFile.writeText(csvExporter.export(rows))
                        ExportFormat.EXCEL -> outFile.writeBytes(xlsxExporter.export(rows))
                        ExportFormat.PDF, ExportFormat.PRINT -> pdfExporter.export(runName, rows, outFile)
                    }
                    outFile
                }
            }.onSuccess { file -> exportedFile = file; isExporting = false }
                .onFailure { e -> errorMessage = e.message ?: "Export failed"; isExporting = false }
        }
    }

    fun clearResult() {
        exportedFile = null
        errorMessage = null
    }
}
