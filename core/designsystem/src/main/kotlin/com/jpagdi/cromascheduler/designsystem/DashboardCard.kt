package com.jpagdi.cromascheduler.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The Compose equivalent of MCQ's v3.7 rounded-card dashboard tile: icon on the left
 * in an accent-tinted circle, title + subtitle in the middle, chevron on the right.
 * Used for every Home Dashboard entry (Import Data / Generate Schedule / Validate /
 * Repair / Export) so the five entry points read as one consistent family.
 */
@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: AccentColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CromaShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CromaShapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent.onIcon)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

/** The colored "N conflicts" / "Clean" / "Not set up" badge used on Home's timetable list and inside each Timetable workspace — one composable so the three states can never render with mismatched colors in different screens. */
@Composable
fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
    }
}
