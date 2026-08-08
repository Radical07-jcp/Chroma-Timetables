package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpagdi.cromascheduler.R
import com.jpagdi.cromascheduler.designsystem.CromaColors
import com.jpagdi.cromascheduler.designsystem.ThemeMode

data class SidebarActions(
    val onHome: () -> Unit,
    val onTeachers: () -> Unit,
    val onDefinePeriods: () -> Unit,
    val onRepair: () -> Unit,
    val onSettings: () -> Unit,
)

/**
 * Drawer content only — ModalNavigationDrawer in MainActivity supplies the sliding/gesture
 * behavior and, crucially, opens from the START edge (left, in an LTR layout) by default, which is
 * what "sidebar should appear from the left" actually needed: Compose's drawer is left-aligned out
 * of the box, so this file just had to not fight that default.
 */
@Composable
fun SidebarDrawer(
    currentThemeMode: ThemeMode?,
    onThemeModeChange: (ThemeMode) -> Unit,
    actions: SidebarActions,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CromaColors.Navy)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_chroma),
                    contentDescription = "Chroma Timetables",
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("CHROMA", color = CromaColors.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                Text("TIMETABLES", color = CromaColors.White, fontSize = 9.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 2.dp))
                Text(
                    "PLAN • VALIDATE • OPTIMIZE",
                    color = CromaColors.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                SidebarSectionHeader("SCHEDULING")
                SidebarRow("Timetables", Icons.Filled.Home, actions.onHome)
                SidebarRow("Define Periods", Icons.Filled.CalendarMonth, actions.onDefinePeriods)
                SidebarRow("Repair a Schedule", Icons.Filled.Build, actions.onRepair)

                SidebarSectionHeader("DATA")
                SidebarRow("Teachers", Icons.Filled.Groups, actions.onTeachers)

                SidebarSectionHeader("PREFERENCES")
                ThemePicker(currentThemeMode, onThemeModeChange)

                SidebarSectionHeader("SUPPORT")
                SidebarRow("About / No AI, on-device only", Icons.Filled.Info, actions.onSettings)
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("v0.1.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(
                    "Deterministic graph-coloring engine • no AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun SidebarSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun SidebarRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun ThemePicker(current: ThemeMode?, onChange: (ThemeMode) -> Unit) {
    val selected = current ?: ThemeMode.LIGHT
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onChange(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
