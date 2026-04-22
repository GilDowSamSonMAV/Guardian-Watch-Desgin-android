package com.gltech.guardianwatch.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.R

/**
 * Guardian Watch typography.
 * Fonts are bundled as assets — see /res/font/. In the MVP we fall back to
 * FontFamily.Default + FontFamily.Monospace until fonts are bundled.
 */
object GwTypography {

    // ===== Font families =====
    // NOTE: Bundle IBM Plex Sans, IBM Plex Mono, and Heebo in res/font/ then uncomment:
    // val PlexSans = FontFamily(
    //     Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    //     Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    //     Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    //     Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
    // )
    // val PlexMono = FontFamily(
    //     Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    //     Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    //     Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    // )
    // val Heebo = FontFamily(
    //     Font(R.font.heebo_regular, FontWeight.Normal),
    //     Font(R.font.heebo_medium, FontWeight.Medium),
    //     Font(R.font.heebo_bold, FontWeight.Bold),
    // )

    val PlexSans = FontFamily.SansSerif       // TODO: replace with bundled IBM Plex Sans
    val PlexMono = FontFamily.Monospace       // TODO: replace with bundled IBM Plex Mono
    val Heebo = FontFamily.SansSerif          // TODO: replace with bundled Heebo

    // ===== Display / page title (gw-h1) =====
    val H1 = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 1.1.em,
        letterSpacing = (-0.01).em,
    )

    // ===== Section title — ALL CAPS tracked (gw-h2) =====
    val H2 = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 1.1.em,
        letterSpacing = 0.08.em,
    )

    // ===== Small label / eyebrow — ALL CAPS =====
    val Label = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.08.em,
    )

    // ===== Body =====
    val Body = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 1.45.em,
    )

    // ===== Mono readouts (the workhorse) =====
    val Mono = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.01).em,
    )
    val MonoSm = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = (-0.01).em,
    )
    val MonoLg = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = (-0.01).em,
    )
    val MonoXl = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 1.2.em,
        letterSpacing = (-0.01).em,
    )

    // ===== Audit log rows =====
    val Audit = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 1.5.em,
    )
}

/** Spacing scale (8px base). Use exclusively; do not hardcode dp values. */
object GwSpacing {
    val sp0 = 0
    val sp1 = 4   // xs
    val sp2 = 8
    val sp3 = 12
    val sp4 = 16  // md — default gap
    val sp5 = 24
    val sp6 = 32
    val sp7 = 48  // tap-min
    val sp8 = 64
    val sp9 = 96

    /** 48dp — min tap target for gloves. Enforce on every interactive element. */
    const val tapMin = 48
}

/** Corner radii. */
object GwRadii {
    val r0 = 0
    val r1 = 2   // buttons, inputs, badges
    val r2 = 4   // cards, panels
    // Full = 9999, use via CircleShape
}

/** Motion tokens — linear-only, no cubic-bezier per design system. */
object GwMotion {
    const val durInstant = 0       // CRITICAL alerts
    const val durFast = 80         // press
    const val durBase = 100        // everything else
}
