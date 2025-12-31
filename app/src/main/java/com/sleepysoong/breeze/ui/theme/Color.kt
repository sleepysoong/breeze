package com.sleepysoong.breeze.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BreezeColors(
    val background: Color = Color(0xFF0D0D0D),
    val surface: Color = Color(0xFF1A1A1A),
    val cardBackground: Color = Color(0x14FFFFFF),
    val cardBorder: Color = Color(0x1AFFFFFF),
    val primary: Color = Color(0xFFFF3B30),
    val primaryLight: Color = Color(0xFFFF6B5E),
    val primaryDark: Color = Color(0xFFCC2F26),
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xFF999999),
    val textTertiary: Color = Color(0xFF666666),
    val success: Color = Color(0xFF34C759),
    val warning: Color = Color(0xFFFFCC00),
    val error: Color = Color(0xFFFF3B30)
)

val LocalBreezeColors = staticCompositionLocalOf { BreezeColors() }
