package com.gltech.guardianwatch.model

import androidx.compose.ui.graphics.Color

/** Health/comms status for a soldier — drives card color and map dot color. */
enum class SoldierStatus(val label: String, val color: Color) {
    OK("OK",           Color(0xFF3DDC84)),   // green
    CAUTION("CAUTION", Color(0xFFFFB020)),   // amber
    HIGH("HIGH",       Color(0xFFFF6B1A)),   // orange
    CRITICAL("CRIT",   Color(0xFFFF2D38)),   // red
    OFFLINE("OFF",     Color(0xFF66635D)),   // grey
}
