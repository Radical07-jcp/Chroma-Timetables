package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.data.entity.SessionTypeEntity
import com.jpagdi.cromascheduler.designsystem.CromaAccents
import com.jpagdi.cromascheduler.designsystem.DashboardCard
import com.jpagdi.cromascheduler.viewmodel.CreateTimetableViewModel

@Composable
fun ChooseTimetableTypeScreen(viewModel: CreateTimetableViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    Scaffold(topBar = { CromaTopBar("New Timetable", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("What kind of schedule is this?", style = MaterialTheme.typography.titleMedium)
            Text(
                "This decides which imported sessions this timetable can use, and can't be changed after it's generated.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )

            Spacer(Modifier.height(4.dp))

            DashboardCard(
                title = SessionTypeEntity.CLASS.label(),
                subtitle = "Regular subject periods for sections",
                icon = Icons.Filled.MenuBook,
                accent = CromaAccents.Blue,
                onClick = { viewModel.setSessionType(SessionTypeEntity.CLASS); onNext() },
            )
            DashboardCard(
                title = SessionTypeEntity.EXAM.label(),
                subtitle = "Longer blocks, proctor-focused",
                icon = Icons.Filled.Event,
                accent = CromaAccents.Gold,
                onClick = { viewModel.setSessionType(SessionTypeEntity.EXAM); onNext() },
            )
            DashboardCard(
                title = SessionTypeEntity.LAB.label(),
                subtitle = "Requires a lab-type room",
                icon = Icons.Filled.Science,
                accent = CromaAccents.Mint,
                onClick = { viewModel.setSessionType(SessionTypeEntity.LAB); onNext() },
            )
        }
    }
}
