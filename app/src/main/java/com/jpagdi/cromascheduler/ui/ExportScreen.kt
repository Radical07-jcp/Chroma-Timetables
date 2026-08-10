package com.jpagdi.cromascheduler.ui

import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.DashboardCard
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.export.PdfPrintDocumentAdapter
import com.jpagdi.cromascheduler.viewmodel.ExportFormat
import com.jpagdi.cromascheduler.viewmodel.ExportViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

@Composable
fun ExportScreen(runId: String, runName: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ExportViewModel = viewModel(factory = ViewModelFactory(container))
    val context = LocalContext.current

    LaunchedEffect(runId) { viewModel.clearResult() }

    Scaffold(topBar = { CromaTopBar("Export Schedule", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Export \"$runName\" as:", style = MaterialTheme.typography.titleMedium)

            ExportFormat.entries.forEach { format ->
                DashboardCard(
                    title = format.label,
                    subtitle = subtitleFor(format),
                    icon = iconFor(format),
                    accent = accentFor(format),
                    onClick = { viewModel.export(context, runId, runName, format) },
                )
            }

            if (viewModel.isExporting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preparing file…")
                }
            }

            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            viewModel.exportedFile?.let { file ->
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                val isPdf = file.extension == "pdf"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ready: ${file.name}", style = MaterialTheme.typography.titleSmall)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = mimeTypeFor(file.extension)
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share schedule"))
                            }) { Text("Share") }

                            if (isPdf) {
                                OutlinedButton(onClick = {
                                    val printManager = context.getSystemService(PrintManager::class.java)
                                    val adapter = PdfPrintDocumentAdapter(context, file, runName)
                                    printManager?.print(runName, adapter, PrintAttributes.Builder().build())
                                }) { Text("Print") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun subtitleFor(format: ExportFormat): String = when (format) {
    ExportFormat.CSV -> "Plain text, opens in any spreadsheet app"
    ExportFormat.EXCEL -> "Formatted .xlsx workbook"
    ExportFormat.PDF -> "Paginated document, ready to share"
    ExportFormat.PRINT -> "Same PDF, sent straight to a printer"
}

private fun iconFor(format: ExportFormat): ImageVector = when (format) {
    ExportFormat.CSV -> Icons.Filled.Description
    ExportFormat.EXCEL -> Icons.Filled.TableChart
    ExportFormat.PDF -> Icons.Filled.Article
    ExportFormat.PRINT -> Icons.Filled.Print
}

private fun accentFor(format: ExportFormat) = when (format) {
    ExportFormat.CSV -> CromaAccents.GoldLight
    ExportFormat.EXCEL -> CromaAccents.MaroonLight
    ExportFormat.PDF -> CromaAccents.Gold
    ExportFormat.PRINT -> CromaAccents.Maroon
}

private fun mimeTypeFor(extension: String): String = when (extension) {
    "csv" -> "text/csv"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "pdf" -> "application/pdf"
    else -> "application/octet-stream"
}
