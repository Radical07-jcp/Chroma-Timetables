package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpagdi.cromascheduler.data.entity.TeacherEntity
import com.jpagdi.cromascheduler.di.LocalAppContainer
import com.jpagdi.cromascheduler.viewmodel.TeachersViewModel
import com.jpagdi.cromascheduler.viewmodel.ViewModelFactory

@Composable
fun TeachersScreen(onBack: () -> Unit, onSelectTeacher: (TeacherEntity) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TeachersViewModel = viewModel(factory = ViewModelFactory(container))
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(topBar = { CromaTopBar("Teachers", onBack) }) { padding ->
        if (viewModel.teachers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No teachers yet — import your data from the Home screen.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(viewModel.teachers) { teacher ->
                    Card(onClick = { onSelectTeacher(teacher) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(teacher.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (teacher.subjectIds.isEmpty()) "No subjects listed" else teacher.subjectIds.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
