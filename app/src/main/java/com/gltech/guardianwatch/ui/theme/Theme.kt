package com.gltech.guardianwatch.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box

/**
 * Guardian Watch theme.
 *
 * DESIGN LOCK:
 * - Dark only. No light mode. Military UI must never wash out under direct sunlight shift.
 * - No dynamic color (Material You). Brand chrome is non-negotiable.
 * - Use [GwColors], [GwTypography], [GwSpacing], [GwRadii] — never hardcode.
 */
private val GwDarkColorScheme = darkColorScheme(
    primary = GwColors.chromeOlive500,
    onPrimary = GwColors.fg000,
    secondary = GwColors.chromeEarth500,
    onSecondary = GwColors.fg000,
    tertiary = GwColors.stateLive,
    background = GwColors.bg000,
    onBackground = GwColors.fg000,
    surface = GwColors.bg100,
    onSurface = GwColors.fg100,
    surfaceVariant = GwColors.bg200,
    onSurfaceVariant = GwColors.fg200,
    error = GwColors.critRed,
    onError = GwColors.fg000,
    outline = GwColors.strokeDefault,
    outlineVariant = GwColors.strokeHairline,
)

@Composable
fun GuardianWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GwDarkColorScheme,
        typography = androidx.compose.material3.Typography(
            // Map Material 3 roles to our tokens
            displayLarge = GwTypography.H1,
            headlineLarge = GwTypography.H1,
            headlineMedium = GwTypography.H2,
            titleLarge = GwTypography.H2,
            titleMedium = GwTypography.Label,
            bodyLarge = GwTypography.Body,
            bodyMedium = GwTypography.Body,
            labelLarge = GwTypography.Label,
            labelSmall = GwTypography.MonoSm,
        ),
        content = {
            // Always paint the field background first so we don't flash
            // Material's default surface color during composition.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GwColors.bg000)
            ) {
                content()
            }
        }
    )
}
