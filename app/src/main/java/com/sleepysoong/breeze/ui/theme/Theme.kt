package com.sleepysoong.breeze.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val DarkColorScheme = darkColorScheme(
    primary = BreezeColors().primary,
    onPrimary = BreezeColors().textPrimary,
    secondary = BreezeColors().primaryLight,
    onSecondary = BreezeColors().textPrimary,
    background = BreezeColors().background,
    onBackground = BreezeColors().textPrimary,
    surface = BreezeColors().surface,
    onSurface = BreezeColors().textPrimary,
    error = BreezeColors().error,
    onError = BreezeColors().textPrimary
)

@Composable
fun BreezeTheme(
    content: @Composable () -> Unit
) {
    val breezeColors = BreezeColors()
    val breezeTypography = BreezeTypography()

    CompositionLocalProvider(
        LocalBreezeColors provides breezeColors,
        LocalBreezeTypography provides breezeTypography
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}

object BreezeTheme {
    val colors: BreezeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBreezeColors.current

    val typography: BreezeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalBreezeTypography.current
}
