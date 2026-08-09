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
import com.jpagdi.cromascheduler.designsystem.CromaColors
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation

/**
 * One accent color across the WHOLE header row (icon + title, background included) — every
 * screen's header goes through this, rather than each screen picking its own title color against
 * a plain background. [accent] defaults to the app's navy so every screen matches unless there's a
 * specific reason to differ.
 */
@Composable
fun CromaTopBar(title: String, onBack: () -> Unit, accent: Color = CromaColors.Navy) {
    Surface(color = accent) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
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
fun CromaHomeHeader(onOpenDrawer: () -> Unit, accent: Color = CromaColors.Navy) {
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
 * leading rather than a full second line-height of empty space — that extra gap is what "remove
 * line/row spacing (paragraph spacing)" was about. Setting `lineHeight` equal to `fontSize` on each
 * Text removes Material's default extra leading; a small explicit `padding(top=)` on the second
 * line replaces it with an intentional, much smaller gap instead of relying on default line-height.
 */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier = modifier) {
        Text(
            "CHROMA",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.6.sp,
        )
        Text(
            "TIMETABLES",
            color = color,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 2.dp),
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
