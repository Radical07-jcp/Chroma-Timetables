package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jpagdi.cromascheduler.designsystem.AccentPrefs

/**
 * One screen, reused for both GroupA ("Top Panel Accent") and GroupB ("Button Accent") — same
 * 16-swatch preset grid and lightness slider as the reference app's AccentColorPickerDialog,
 * as a full screen instead of a dialog (a dedicated back-stack destination fits this app's
 * navigation better than a heavyweight custom-View dialog did in the reference app).
 */
@Composable
fun AccentColorPickerScreen(
    title: String,
    current: Color,
    onBack: () -> Unit,
    onPick: (Color) -> Unit,
    onReset: () -> Unit,
) {
    var selectedBase by remember(current) { mutableStateOf(AccentPrefs.nearestPreset(current)) }
    var lightness by remember(current) { mutableStateOf(AccentPrefs.lightnessDeltaFrom(selectedBase, current)) }
    val previewColor = AccentPrefs.adjustLightness(selectedBase, lightness)

    Scaffold(topBar = { CromaTopBar(title, onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Live preview — a mini header bar so the picked color reads exactly the way it will
            // once applied, not just as an isolated swatch.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewColor),
                contentAlignment = Alignment.Center,
            ) {
                Text("Preview", color = AccentPrefs.textColorFor(previewColor), style = MaterialTheme.typography.titleMedium)
            }

            Text("Presets", style = MaterialTheme.typography.titleSmall)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(180.dp),
            ) {
                items(AccentPrefs.PRESET_SWATCHES) { swatch ->
                    val isSelected = swatch == selectedBase
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.Black.copy(alpha = 0.15f), CircleShape)
                            .clickable { selectedBase = swatch; lightness = 0f },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = AccentPrefs.textColorFor(swatch))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lightness", style = MaterialTheme.typography.titleSmall)
                Slider(value = lightness, onValueChange = { lightness = it }, valueRange = -0.5f..0.5f)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Revert to Default") }
                Button(onClick = { onPick(previewColor) }, modifier = Modifier.weight(1f)) { Text("Apply") }
            }
        }
    }
}
