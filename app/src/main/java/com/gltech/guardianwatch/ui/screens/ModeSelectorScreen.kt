package com.gltech.guardianwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.mode.AppMode
import com.gltech.guardianwatch.ui.components.BrandMonogram
import com.gltech.guardianwatch.ui.components.GhostButton
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

@Composable
fun ModeSelectorScreen(
    onModeSelected: (AppMode) -> Unit,
    onPairWatchRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GwColors.bg000)
            .padding(GwSpacing.sp7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMonogram(size = 64)
        Spacer(Modifier.height(GwSpacing.sp5.dp))
        Text(
            text = "GUARDIAN WATCH",
            style = GwTypography.H1.copy(color = GwColors.fg000),
        )
        Text(
            text = "TACTICAL HEALTH MONITORING",
            style = GwTypography.Label.copy(color = GwColors.fg200),
        )
        Spacer(Modifier.height(GwSpacing.sp7.dp))
        Text(
            text = "SELECT OPERATING MODE",
            style = GwTypography.H2.copy(color = GwColors.fg000),
        )
        Spacer(Modifier.height(GwSpacing.sp2.dp))
        Text(
            text = "Mode persists until changed. Tap to select.",
            style = GwTypography.Body.copy(color = GwColors.fg200),
        )
        Spacer(Modifier.height(GwSpacing.sp6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp)) {
            AppMode.entries.forEach { mode ->
                ModeCard(mode = mode, onClick = { onModeSelected(mode) })
            }
        }
        if (onPairWatchRequested != null) {
            Spacer(Modifier.height(GwSpacing.sp6.dp))
            GhostButton(label = "Pair Watch", onClick = onPairWatchRequested)
        }
    }
}

@Composable
private fun ModeCard(mode: AppMode, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r2.dp))
            .clickable { onClick() }
            .padding(GwSpacing.sp5.dp),
    ) {
        Text(
            text = mode.displayName,
            style = GwTypography.H2.copy(color = GwColors.fg000),
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))
        Text(
            text = mode.description,
            style = GwTypography.Body.copy(color = GwColors.fg100),
        )
    }
}
