package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.designsystem.CromaColors
import com.jpagdi.cromascheduler.designsystem.LocalHeaderAccent
import com.jpagdi.cromascheduler.designsystem.ThemeMode

data class SidebarActions(
    val onHome: () -> Unit,
    val onRepair: () -> Unit,
    val onSettings: () -> Unit,
    val onAbout: () -> Unit,
)

/**
 * Drawer content only — ModalNavigationDrawer in MainActivity supplies the sliding/gesture
 * behavior and opens from the START edge (left, in an LTR layout) by default.
 *
 * No "Define Periods" row anymore — periods are per-timetable now, chosen during creation, not a
 * standalone global setting there'd be anything to edit here. No "Teachers" row either — teacher
 * data now lives inside each timetable's own bottom nav (via TimetableDetailScreen), since a
 * global unscoped Teachers screen couldn't show a teacher's time slots for any specific schedule.
 * Settings and About are two separate rows: Settings is meant to grow (default algorithm today,
 * more later), About is meant to stay a static page — folding them together would mean either an
 * ever-growing About page or an oddly-named Settings page with one static paragraph in it.
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
                    .background(LocalHeaderAccent.current)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandWordmark()
                Text(
                    "PLAN • VALIDATE • OPTIMIZE",
                    color = CromaColors.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                SidebarSectionHeader("SCHEDULING")
                SidebarRow("Timetables", Icons.Filled.Home, actions.onHome)
                SidebarRow("Repair a Schedule", Icons.Filled.Build, actions.onRepair)

                SidebarSectionHeader("PREFERENCES")
                ThemePill(currentThemeMode, onThemeModeChange)
                SidebarRow("Settings", Icons.Filled.Settings, actions.onSettings)

                SidebarSectionHeader("SUPPORT")
                SidebarRow("About", Icons.Filled.Info, actions.onAbout)
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Chroma Engine v0.1.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("Developed by Sir_JPagdi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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

/**
 * One pill: three small dots (light/dark/black previews) + the current theme's name — tapping
 * anywhere on the pill cycles Light -> Dark -> Black -> Light, replacing the earlier three-segment
 * selector. The dots are fixed reference colors (white/navy/black), not the app's live color
 * scheme, so they work as a legend regardless of which theme is currently active.
 */
@Composable
private fun ThemePill(current: ThemeMode?, onChange: (ThemeMode) -> Unit) {
    val mode = current ?: ThemeMode.LIGHT
    val next = when (mode) {
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.BLACK
        ThemeMode.BLACK -> ThemeMode.LIGHT
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onChange(next) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        ThemeDot(Color.White)
        Spacer(Modifier.width(4.dp))
        ThemeDot(CromaColors.SurfaceDark)
        Spacer(Modifier.width(4.dp))
        ThemeDot(Color.Black)
        Spacer(Modifier.width(10.dp))
        Text(
            mode.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeDot(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), CircleShape),
    )
}
