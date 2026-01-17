package com.sleepysoong.breeze.ui.components.liquidglass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import androidx.compose.ui.graphics.isSpecified

@Composable
fun LiquidCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isInteractive: Boolean = false,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val defaultSurfaceColor = if (isLightTheme) {
        Color.White.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val blurRadiusPx = with(density) { 8.dp.toPx() }
    val lensInnerPx = with(density) { 16.dp.toPx() }
    val lensOuterPx = with(density) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(blurRadiusPx)
                    lens(lensInnerPx, lensOuterPx)
                },
                highlight = {
                    if (isInteractive) {
                        val progress = interactiveHighlight.pressProgress
                        Highlight.Ambient.copy(alpha = progress)
                    } else {
                        Highlight.Ambient.copy(alpha = 0.3f)
                    }
                },
                shadow = {
                    Shadow(
                        radius = 8.dp,
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1.02f, progress)
                        scaleX = scale
                        scaleY = scale
                    }
                } else null,
                onDrawSurface = {
                    drawRect(if (surfaceColor.isSpecified) surfaceColor else defaultSurfaceColor)
                }
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun LiquidSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 4.dp,
    surfaceAlpha: Float = 0.1f,
    content: @Composable BoxScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val surfaceColor = if (isLightTheme) {
        Color.White.copy(alpha = surfaceAlpha * 5f)
    } else {
        Color.White.copy(alpha = surfaceAlpha)
    }
    
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { blurRadius.toPx() }
    val lensInnerPx = with(density) { 8.dp.toPx() }
    val lensOuterPx = with(density) { 16.dp.toPx() }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(blurRadiusPx)
                    lens(lensInnerPx, lensOuterPx)
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                }
            )
    ) {
        content()
    }
}
