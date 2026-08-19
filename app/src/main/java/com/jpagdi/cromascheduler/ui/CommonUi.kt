package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.jpagdi.cromascheduler.R
import com.jpagdi.cromascheduler.designsystem.CromaColors
import com.jpagdi.cromascheduler.designsystem.LocalHeaderAccent
import com.jpagdi.cromascheduler.designsystem.PressStart2PFamily
import com.jpagdi.cromascheduler.engine.validation.ConstraintViolation

/**
 * Modern app chrome shared by the whole application. Navigation, state and screen behavior remain
 * outside this component; this is intentionally presentation-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CromaTopBar(title: String, onBack: () -> Unit, accent: Color = LocalHeaderAccent.current) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.Center) {
                BrandWordmark()
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        actions = {
            Surface(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(8.dp),
                shape = MaterialTheme.shapes.small,
                color = accent,
            ) {}
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CromaHomeHeader(
    onOpenDrawer: () -> Unit,
    accent: Color = LocalHeaderAccent.current,
) {
    CenterAlignedTopAppBar(
        title = { BrandWordmark() },
        navigationIcon = {
            FilledIconButton(
                onClick = onOpenDrawer,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
            }
        },
        actions = {
            Surface(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(10.dp),
                shape = MaterialTheme.shapes.small,
                color = accent,
            ) {}
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    // Measure CHROMA's rendered width so TIMETABLES can be stretched to match it via
    // letter-spacing; if the natural TIMETABLES width still doesn't land exactly on
    // that width, the remainder is centered within CHROMA's span rather than left-aligned.
    var chromaWidthPx by remember { mutableStateOf(0) }
    var timetablesBaseWidthPx by remember { mutableStateOf(0) }

    val baseLetterSpacingSp = 1.0f
    val timetablesCharCount = "TIMETABLES".length
    val stretchedLetterSpacingSp = if (chromaWidthPx > 0 && timetablesBaseWidthPx > 0) {
        val deltaPx = chromaWidthPx - timetablesBaseWidthPx
        // px -> sp manually (sp is affected by both density and font scale, unlike dp).
        val deltaSp = deltaPx / (density.density * density.fontScale)
        (baseLetterSpacingSp + deltaSp / timetablesCharCount).coerceAtLeast(0f)
    } else {
        baseLetterSpacingSp
    }

    Column(modifier = modifier) {
        Text(
            "CHROMA",
            color = Color(0xFFFFB511),
            fontFamily = PressStart2PFamily,
            fontSize = 17.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.5.sp,
            onTextLayout = { result -> chromaWidthPx = result.size.width },
        )
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .then(
                    if (chromaWidthPx > 0) {
                        Modifier.width(with(density) { chromaWidthPx.toDp() })
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "TIMETABLES",
                color = CromaColors.WordmarkGreen,
                fontFamily = PressStart2PFamily,
                fontSize = 8.6.sp,
                lineHeight = 9.sp,
                letterSpacing = baseLetterSpacingSp.sp,
                softWrap = false,
                textAlign = TextAlign.Center,
                onTextLayout = { result ->
                    // Only capture the width measured at the base (unstretched) spacing so the
                    // stretch calculation above doesn't feed back into itself.
                    if (timetablesBaseWidthPx == 0) timetablesBaseWidthPx = result.size.width
                },
                modifier = Modifier.alpha(0f),
            )
            Text(
                "TIMETABLES",
                color = CromaColors.WordmarkGreen,
                fontFamily = PressStart2PFamily,
                fontSize = 8.6.sp,
                lineHeight = 9.sp,
                letterSpacing = stretchedLetterSpacingSp.sp,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun CromaWorkflowTags(active: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkflowTag("PLAN", Color(0xFFFFB511), active == "PLAN")
        WorkflowTag("VALIDATE", CromaColors.WordmarkGreen, active == "VALIDATE")
        WorkflowTag("OPTIMIZE", Color(0xFF8AA8FF), active == "OPTIMIZE")
    }
}

@Composable
private fun WorkflowTag(label: String, color: Color, active: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (active) 0.75f else 0.45f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FormalBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Justify,
    )
}

@Composable
fun ViolationList(violations: List<ConstraintViolation>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(violations) { violation ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        violation.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        violation.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
