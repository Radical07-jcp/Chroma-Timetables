package com.jpagdi.cromascheduler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
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
 * Modern app chrome shared by the whole application. Navigation, state and screen behavior remain
 * outside this component; this is intentionally presentation-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CromaTopBar(title: String, onBack: () -> Unit, accent: Color = LocalHeaderAccent.current) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
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
        title = { BrandWordmark(color = MaterialTheme.colorScheme.onBackground) },
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
fun BrandWordmark(modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier = modifier) {
        Text(
            "CHROMA",
            color = color,
            fontFamily = PressStart2PFamily,
            fontSize = 17.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.5.sp,
        )
        Text(
            "TIMETABLES",
            color = color,
            fontFamily = PressStart2PFamily,
            fontSize = 8.sp,
            lineHeight = 8.sp,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
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
