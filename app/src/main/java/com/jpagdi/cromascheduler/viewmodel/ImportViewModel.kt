package com.jpagdi.cromascheduler.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpagdi.cromascheduler.data.repository.CsvImportService
import com.jpagdi.cromascheduler.data.repository.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Loading : ImportUiState()
    data class Done(val result: ImportResult) : ImportUiState()
    data class Failed(val message: String) : ImportUiState()
}

class ImportViewModel(private val csvImportService: CsvImportService) : ViewModel() {
    var uiState by mutableStateOf<ImportUiState>(ImportUiState.Idle)
        private set

    /** Single zip containing all six CSVs — the spec's "Optionally support importing a ZIP archive" path. */
    fun importZip(context: Context, uri: Uri) {
        uiState = ImportUiState.Loading
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { csvImportService.importFromZip(it) }
                        ?: error("Could not open the selected file")
                }
            }.onSuccess { result -> uiState = ImportUiState.Done(result) }
                .onFailure { e -> uiState = ImportUiState.Failed(e.message ?: "Import failed") }
        }
    }

    /** Multiple individually-picked CSVs — filename (per the six expected names) decides which parser handles each. */
    fun importCsvFiles(context: Context, uris: List<Uri>) {
        uiState = ImportUiState.Loading
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val files = uris.mapNotNull { uri ->
                        val name = queryDisplayName(context, uri) ?: return@mapNotNull null
                        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                            ?: return@mapNotNull null
                        name to text
                    }.toMap()
                    csvImportService.importFromFiles(files)
                }
            }.onSuccess { result -> uiState = ImportUiState.Done(result) }
                .onFailure { e -> uiState = ImportUiState.Failed(e.message ?: "Import failed") }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    fun reset() {
        uiState = ImportUiState.Idle
    }
}
