package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.R
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.designsystem.CromaShapes
import com.jpagdi.cromascheduler.designsystem.CromaStatus
import com.jpagdi.cromascheduler.designsystem.StatusPill
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.HomeViewModel
import com.jpagdi.cromascheduler.viewmodel.TimetableSummary
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Three cards, one per schedule type — Import/Generate/Validate/Repair/Optimize/Export all live
 * one tap further in, inside [TimetableWorkspaceScreen], scoped to whichever card was tapped. That's
 * the actual structural fix behind "generate and import buttons must be within the timetable
 * details": there's no home-screen action button that isn't already about one specific timetable.
 */
@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, onOpenWorkspace: (SessionTypeEntity) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container.scheduleRepository))
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                    Image(
                        painter = painterResource(R.drawable.logo_chroma),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).padding(start = 4.dp),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("CHROMA", fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 0.6.sp)
                        Text("TIMETABLES", fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
        },
    ) { padding ->
        if (viewModel.summaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(viewModel.summaries) { summary ->
                    TimetableCard(summary, onClick = { onOpenWorkspace(summary.sessionType) })
                }
            }
        }
    }
}

@Composable
private fun TimetableCard(summary: TimetableSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = CromaShapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.sessionType.label(), style = MaterialTheme.typography.titleMedium)
                val latest = summary.latestRun
                Text(
                    if (latest == null) {
                        "Not set up yet"
                    } else {
                        "${latest.algorithmUsed} • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(latest.createdAtEpochMillis))}" +
                            if (summary.runCount > 1) " • ${summary.runCount} runs" else ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            val latest = summary.latestRun
            when {
                latest == null -> StatusPill("NOT SET UP", CromaStatus.Pending)
                summary.conflictCount > 0 -> StatusPill("${summary.conflictCount} CONFLICT${if (summary.conflictCount == 1) "" else "S"}", CromaStatus.Conflicts)
                else -> StatusPill("CLEAN", CromaStatus.Clean)
            }
        }
    }
}
