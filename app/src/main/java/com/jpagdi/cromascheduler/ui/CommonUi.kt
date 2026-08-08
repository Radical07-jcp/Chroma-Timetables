package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation

@Composable
fun CromaTopBar(title: String, onBack: () -> Unit) {
    Surface {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Shared by Validate and Repair so a conflict reads identically in both places. */
@Composable
fun ViolationList(violations: List<ConstraintViolation>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(violations) { violation ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(violation.type.name.replace('_', ' '), style = MaterialTheme.typography.titleSmall)
                    Text(violation.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
