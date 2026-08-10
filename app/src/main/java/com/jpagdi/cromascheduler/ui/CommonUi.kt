package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpagdi.cromascheduler.R
import com.jpagdi.cromascheduler.designsystem.LocalHeaderAccent
import com.jpagdi.cromascheduler.designsystem.PressStart2PFamily
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation

/**
 * One accent color across the WHOLE header row (icon + title, background included) — every
 * screen's header goes through this, rather than each screen picking its own title color against
 * a plain background. [accent] defaults to [LocalHeaderAccent] — the reference app's "Top Panel
 * Accent" / GroupA — so every screen matches, and stays live if that accent is ever changed in
 * Settings, unless there's a specific reason for a screen to override it.
 */
@Composable
fun CromaTopBar(title: String, onBack: () -> Unit, accent: Color = LocalHeaderAccent.current) {
    Surface(color = accent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

/**
 * Home's header — hamburger + logo + two-line CHROMA/TIMETABLES brand instead of back+title, but
 * the SAME accent-across-the-whole-row treatment as [CromaTopBar], so Home doesn't look like a
 * different app from every other screen.
 */
@Composable
fun CromaHomeHeader(onOpenDrawer: () -> Unit, accent: Color = LocalHeaderAccent.current) {
    Surface(color = accent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Open menu", tint = Color.White)
            }
            Image(
                painter = painterResource(R.drawable.logo_chroma),
                contentDescription = null,
                modifier = Modifier.size(30.dp).padding(start = 4.dp),
            )
            BrandWordmark(modifier = Modifier.padding(start = 12.dp))
        }
    }
}

/**
 * CHROMA / TIMETABLES, two lines, with the gap between them collapsed to just the line's own
 * leading rather than a full second line-height of empty space. Set in Press Start 2P (the
 * reference app's own 8-bit/arcade display font), scoped to exactly this composable — every other
 * Text in the app stays Montserrat via CromaTypography, same "just these two spots" scope the
 * reference app used this font with. 18sp/9sp matches that app's own header sizing exactly.
 */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier = modifier) {
        Text(
            "CHROMA",
            color = color,
            fontFamily = PressStart2PFamily,
            fontSize = 18.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.6.sp,
        )
        Text(
            "TIMETABLES",
            color = color,
            fontFamily = PressStart2PFamily,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
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
