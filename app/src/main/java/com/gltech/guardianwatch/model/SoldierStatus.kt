package com.gltech.guardianwatch.model

import androidx.compose.ui.graphics.Color

/** Health/comms status for a soldier — drives card color and map dot color. */
enum class SoldierStatus(val label: String, val color: Color) {
    OK("תקין",           Color(0xFF3DDC84)),   // green
    CAUTION("אזהרה", Color(0xFFFFB020)),   // amber
    HIGH("גבוה",       Color(0xFFFF6B1A)),   // orange
    CRITICAL("קריטי",   Color(0xFFFF2D38)),   // red
    OFFLINE("מנותק",     Color(0xFF66635D)),   // grey
}
