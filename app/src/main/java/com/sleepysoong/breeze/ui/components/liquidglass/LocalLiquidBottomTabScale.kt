package com.sleepysoong.breeze.ui.components.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLiquidBottomTabScale = staticCompositionLocalOf<@Composable () -> Float> { { 1f } }
