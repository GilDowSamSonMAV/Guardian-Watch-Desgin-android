package com.gltech.guardianwatch.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Guardian Watch design tokens — colors.
 * Generated from design_system/colors_and_type.css (oklch → sRGB sRGB).
 * DO NOT hardcode colors anywhere else in the app. Reference from [GwColors].
 */
object GwColors {
    // ===== Base (warm near-black, NEVER pure #000) =====
    val bg000 = Color(0xFF0C0806)  // field / page
    val bg100 = Color(0xFF13100D)  // panel
    val bg200 = Color(0xFF1A1613)  // card
    val bg300 = Color(0xFF231E1A)  // raised / dropdown
    val bg400 = Color(0xFF2E2924)  // modal / confirm

    // ===== Chrome (muted olive / earth) =====
    val chromeOlive900 = Color(0xFF191C12)
    val chromeOlive700 = Color(0xFF373A29)
    val chromeOlive500 = Color(0xFF5E6049)  // primary chrome accent
    val chromeOlive300 = Color(0xFF909274)
    val chromeEarth700 = Color(0xFF3B3127)
    val chromeEarth500 = Color(0xFF615244)
    val chromeEarth300 = Color(0xFF97826F)

    // ===== Foreground (warm-tinted neutrals, AAA contrast) =====
    val fg000 = Color(0xFFF6F5F2)  // primary text / numerals
    val fg100 = Color(0xFFD0CDC9)  // body
    val fg200 = Color(0xFF9B9893)  // secondary / labels
    val fg300 = Color(0xFF66635D)  // tertiary / hints
    val fg400 = Color(0xFF46423D)  // disabled / dividers in use

    // ===== Strokes =====
    val strokeHairline = Color(0xFF2C2824)
    val strokeDefault = Color(0xFF47413C)
    val strokeStrong = Color(0xFF797065)

    // ===== Triage (NATO/TCCC). MUST be paired with shape glyph for colorblind safety. =====
    val triageMinor = Color(0xFF61BD67)       // green
    val triageMinorDim = Color(0xFF235B28)
    val triageDelayed = Color(0xFFEABF3A)     // yellow
    val triageDelayedDim = Color(0xFF816500)
    val triageImmediate = Color(0xFFEE343B)   // red
    val triageImmediateDim = Color(0xFF800613)
    val triageExpectant = Color(0xFF353230)   // black
    val triageExpectantFg = Color(0xFFA8A3A0)

    // ===== Clinical alerts (reserved — NEVER for chrome/links/brand) =====
    val critRed = Color(0xFFEE0B2A)
    val critRedBg = Color(0xFF4F0A0D)
    val warnAmber = Color(0xFFEF9E00)
    val warnAmberBg = Color(0xFF3F2100)
    val cautionYellow = Color(0xFFE1C34B)
    val infoCyan = Color(0xFF43AFC1)

    // ===== Signal / state =====
    val stateLive = Color(0xFF4BC98F)
    val stateStale = warnAmber
    val stateLost = fg300
    val stateOffline = critRed
}
