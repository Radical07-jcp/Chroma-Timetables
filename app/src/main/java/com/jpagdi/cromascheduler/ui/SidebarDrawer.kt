package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.designsystem.CromaColors
import com.jpagdi.cromascheduler.designsystem.ThemeMode

data class SidebarActions(
    val onHome: () -> Unit,
    val onNewTimetable: () -> Unit,
    val onRepair: () -> Unit,
    val onSettings: () -> Unit,
    val onAbout: () -> Unit,
)

@Composable
fun SidebarDrawer(
    currentThemeMode: ThemeMode?,
    onThemeModeChange: (ThemeMode) -> Unit,
    actions: SidebarActions,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxHeight()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BrandWordmark()
                    CromaWorkflowTags(modifier = Modifier.padding(top = 2.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SidebarSectionHeader("WORKSPACE")
                SidebarRow("Timetables", Icons.Filled.Home, actions.onHome)
                SidebarRow("New timetable", Icons.Filled.Add, actions.onNewTimetable)
                SidebarRow("Repair a schedule", Icons.Filled.Build, actions.onRepair)

                SidebarSectionHeader("APPEARANCE")
                ThemeSelector(currentThemeMode, onThemeModeChange)

                SidebarSectionHeader("APP")
                SidebarRow("Settings", Icons.Filled.Settings, actions.onSettings)
                SidebarRow("About", Icons.Filled.Info, actions.onAbout)
            }

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "Chroma Engine",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "v1.0.4  •  On-device scheduling",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SidebarSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun SidebarRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun ThemeSelector(
    currentThemeMode: ThemeMode?,
    onChange: (ThemeMode) -> Unit,
) {
    val selected = currentThemeMode ?: ThemeMode.LIGHT
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ThemeChoice(
            "Light",
            Icons.Filled.LightMode,
            selected == ThemeMode.LIGHT,
            onClick = { onChange(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f),
        )
        ThemeChoice(
            "Dark",
            Icons.Filled.DarkMode,
            selected == ThemeMode.DARK,
            onClick = { onChange(ThemeMode.DARK) },
            modifier = Modifier.weight(1f),
        )
        ThemeChoice(
            "Black",
            Icons.Filled.WbSunny,
            selected == ThemeMode.BLACK,
            onClick = { onChange(ThemeMode.BLACK) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeChoice(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = modifier,
    )
}
