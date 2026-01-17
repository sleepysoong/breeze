ultrathink.

이 프로젝트에 애플의 리퀴드글래스 디자인을 적용해줘. 적용하기 위해 Backdrop을 사용할꺼야. 가이드라인과 관련 소스 코드를 줄게.


작업이 끝난 후에는 항상 한글로 응답하고 작업이 종료된 후 커밋과 푸쉬를 진행해.
또한 apk를 빌드한다음에 같이 커밋해. 커밋 후에 내가 바로 다운로드 받을 수 있게.

backdrop은 무조건 최신 버전을 사용할거야. 기준이 충족되지 않는 경우 다른 것들을 수정해 (예: 개발 환경의 코틀린 버전이 너무 낮다 -> backdrop 구버전 사용 ㄴㄴ 코틀린 버전 업데이트 ㅇㅇ)

그리고 현재 버전이 몇 인지도 각종 쉘 도구를 사용해서 확인하고 진행하고
현재 프로젝트 구조도 파악하고 to-do list를 잘 짜서 체계적으로 진행해.

일단 이 파일을 읽고 나면 curl로 https://raw.githubusercontent.com/sleepysoong/trash/refs/heads/main/prompts/liquid_glass_code.md를 다운로드하고 다운로드 한 파일을 읽어서 이 가이드와 함께 사용하도록 해.

<catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidButton.kt>
package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
</catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidButton.kt>

<catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidToggle.kt>package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF34C759)
        else Color(0xFF30D158)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (selected()) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(ContinuousCapsule)
                .drawBehind {
                    val fraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, fraction))
                }
                .size(64f.dp, 28f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .semantics {
                    role = Role.Switch
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
</catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidToggle.kt>

<catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidSlider.kt>package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange)
                    )
                }
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { value ->
                    if (dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(ContinuousCapsule)
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue =
                                (if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(ContinuousCapsule)
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}</catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidSlider.kt>

<catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt>package com.kyant.backdrop.catalog.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56f.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}</catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt>

<https://kyant.gitbook.io/backdrop/get-started.md># Get started The **Backdrop** library ([GitHub](https://github.com/Kyant0/AndroidLiquidGlass)) can draw a copy of the **background (backdrop)** to a **foreground** with various effects. You can achieve amazing **liquid glass** effect with the library. <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FAI1e0lbsx4du8U5Drrtm%2FAndroid%20Liquid%20Glass.png?alt=media&#x26;token=9c6338c4-2591-41b0-8da4-fea055cbf20b" alt=""><figcaption></figcaption></figure> ## Installation <div align="left"><figure><img src="https://img.shields.io/maven-central/v/io.github.kyant0/backdrop" alt=""><figcaption></figcaption></figure></div> <pre class="language-kts" data-title="build.gradle.kts"><code class="lang-kts">dependencies { <strong> implementation("io.github.kyant0:backdrop:&#x3C;version>") </strong>} </code></pre> ## Tutorials ### Liquid Glass **🦖 You must read and practise these tutorials before using the library.** * [Glass Bottom Bar](https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-bar) * [Interactive Glass Bottom Bar](https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar) * [Glass Bottom Sheet](https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-sheet) * [Glass Slider](https://kyant.gitbook.io/backdrop/tutorials/glass-slider) * [Smoother rounded corners](https://kyant.gitbook.io/backdrop/tutorials/smoother-rounded-corners) ### Blur * [Progressive blur](https://kyant.gitbook.io/backdrop/tutorials/progressive-blur) ## API * [Backdrops](https://kyant.gitbook.io/backdrop/api/backdrops) * [Backdrop effects](https://kyant.gitbook.io/backdrop/api/backdrop-effects) ## [FAQ](https://kyant.gitbook.io/backdrop/faq)</https://kyant.gitbook.io/backdrop/get-started.md>

<https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-bar># Glass Bottom Bar <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FZHHQFzrhWrlRrNLsNOzS%2FScreenshot_20251024-200814_Catalog.png?alt=media&#x26;token=01987a6e-66d2-4f9a-866d-e9420455f4bf" alt=""><figcaption></figcaption></figure> ## Goals * Create a glass bottom bar over the `MainNavHost`. ```kotlin Box(Modifier.fillMaxSize()) { MainNavHost() } ``` Here is what `MainNavHost` looks like: <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FMyQFsHUhLj2taAoI7uU2%2FScreenshot_20251024-200220_Catalog.png?alt=media&#x26;token=da5b8ed3-c9f4-44b7-a205-69a7f29593e3" alt="" width="188"><figcaption></figcaption></figure> ## What you will learn * Create and draw backdrops * Apply effects to the backdrops * Handle the background drawing correctly * Ensure the readability ## Steps {% stepper %} {% step %} ### Draw backdrop and add lens effect <pre class="language-kotlin"><code class="lang-kotlin">Box(Modifier.fillMaxSize()) { <strong> val backdrop = rememberLayerBackdrop() </strong> MainNavHost( <strong> modifier = Modifier.layerBackdrop(backdrop) </strong> ) Box( Modifier .safeContentPadding() <strong> .drawBackdrop( </strong> backdrop = backdrop, shape = { CircleShape }, effects = { <strong> lens(16f.dp.toPx(), 32f.dp.toPx()) </strong> } ) .height(64f.dp) .fillMaxWidth() .align(Alignment.BottomCenter) ) } </code></pre> <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2F1zXLfrFSBjglOEzw6v3y%2FScreenshot_20251024-200226_Catalog.png?alt=media&#x26;token=07f3eb2a-af72-4fe5-a335-87fdb9369b05" alt=""><figcaption></figcaption></figure> Noooo! The effect is wrong! There are transparent pixels in the bottom bar. Because we only draw the `MainNavHost` , **the background outside of** `MainNavHost` **should be drawn too**. {% endstep %} {% step %} ### Draw the background to the backdrop (optional) <pre class="language-kotlin"><code class="lang-kotlin">val backgroundColor = Color.White val backdrop = rememberLayerBackdrop { <strong> drawRect(backgroundColor) </strong> drawContent() } </code></pre> <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FdPM0CTuTFWCGpxo4d9gZ%2FScreenshot_20251024-200234_Catalog.png?alt=media&#x26;token=3e4b1edb-b6fa-46ac-97dd-3093846f29cc" alt=""><figcaption></figcaption></figure> Nice work! Try to adjust the lens effect and observe what will happen. {% endstep %} {% step %} ### Add blur effect <pre class="language-kotlin"><code class="lang-kotlin">Modifier.drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { <strong> vibrancy() </strong><strong> blur(4f.dp.toPx()) </strong> lens(16f.dp.toPx(), 32f.dp.toPx()) } ) </code></pre> The use of `vibrancy()` enhances the saturation, giving us more visual impact. <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2F1RMlg4IbZiEJ0yDotoq4%2FScreenshot_20251024-203946_Catalog.png?alt=media&#x26;token=4fa94408-b3e1-4b42-a48c-18111cce3649" alt=""><figcaption></figcaption></figure> {% endstep %} {% step %} ### Add surface for readability <pre class="language-kotlin"><code class="lang-kotlin">Modifier.drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, <strong> onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } </strong>) </code></pre> <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FKvf0pFcZhXuGsePRCifg%2FScreenshot_20251024-200434_Catalog.png?alt=media&#x26;token=e7bb3f25-df82-4402-b009-fda2c56da102" alt=""><figcaption></figcaption></figure> The readability has increased. **You must balance between beauty and readability.** {% endstep %} {% step %} ### Final code ```kotlin Box(Modifier.fillMaxSize()) { val backgroundColor = Color.White val backdrop = rememberLayerBackdrop { drawRect(backgroundColor) drawContent() } MainNavHost( modifier = Modifier.layerBackdrop(backdrop) ) Box( Modifier .safeContentPadding() .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) .height(64f.dp) .fillMaxWidth() .align(Alignment.BottomCenter) ) } ``` {% endstep %} {% endstepper %} ## Exercise: Add a tinted glass icon button <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2F8XPa2wHkxJ8JIjgbip2E%2FScreenshot_20251024-200814_Catalog.png?alt=media&#x26;token=b74710df-4e76-46dc-972d-9592338445d1" alt=""><figcaption></figcaption></figure> <details> <summary>Final code</summary> <pre class="language-kotlin"><code class="lang-kotlin">Box(Modifier.fillMaxSize()) { val backgroundColor = Color.White val backdrop = rememberLayerBackdrop { drawRect(backgroundColor) drawContent() } MainNavHost( modifier = Modifier.layerBackdrop(backdrop) ) Row( Modifier .safeContentPadding() .height(64f.dp) .fillMaxWidth() .align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(8f.dp), verticalAlignment = Alignment.CenterVertically ) { Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) .fillMaxHeight() .weight(1f) ) Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { <strong> val tint = Color(0xFF0088FF) </strong><strong> drawRect(tint, blendMode = BlendMode.Hue) </strong><strong> drawRect(tint.copy(alpha = 0.75f)) </strong> } ) .aspectRatio(1f) ) } } </code></pre> </details> It is recommended to use `BlendMode.Hue` , so that the hue of backdrop will adapt to the tint color.</https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-bar>

<https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar># Interactive Glass Bottom Bar <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FrUHiaDwXAId1w3KQ2OFO%2FScreenshot_20251024-212215_Catalog.png?alt=media&#x26;token=52e89db2-a54d-44a0-8295-6b67757111cc" alt=""><figcaption></figcaption></figure> ## Goals * Add "press to scale" animation to the glass bottom bar ## What you will learn * Handle the transformations (scale, rotation) correctly * Make use of `layerBlock` parameter of the `drawBackdrop` modifier ## Steps {% stepper %} {% step %} ### Press to scale Update the code for the glass bottom bar. <pre class="language-kotlin"><code class="lang-kotlin"><strong>val animationScope = rememberCoroutineScope() </strong><strong>val progressAnimation = remember { Animatable(0f) } </strong> Box( Modifier <strong> .graphicsLayer { </strong> val progress = progressAnimation.value val maxScale = (size.width + 16f.dp.toPx()) / size.width val scale = lerp(1f, maxScale, progress) scaleX = scale scaleY = scale } .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) <strong> .clickable {} </strong><strong> .pointerInput(animationScope) { </strong> val animationSpec = spring(0.5f, 300f, 0.001f) awaitEachGesture { // press awaitFirstDown() animationScope.launch { progressAnimation.animateTo(1f, animationSpec) } // release waitForUpOrCancellation() animationScope.launch { progressAnimation.animateTo(0f, animationSpec) } } } .fillMaxHeight() .weight(1f) ) </code></pre> Press the bottom bar. <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FzelkA8KsIEuRLSWOnRn8%2FScreenshot_20251024-212145_Catalog.png?alt=media&#x26;token=ccd57d9f-4969-440b-8f13-a7c34419c935" alt=""><figcaption></figcaption></figure> Oops! The backdrop is misplaced, it will also scale with the bottom bar. **But the backdrop shouldn't scale**. {% endstep %} {% step %} ### Prevent backdrop from scaling Move the code in `graphicsLayer` to `layerBlock` in the `drawBackdrop` modifier. <pre class="language-kotlin"><code class="lang-kotlin">val animationScope = rememberCoroutineScope() val progressAnimation = remember { Animatable(0f) } Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, <strong> layerBlock = { </strong><strong> val progress = progressAnimation.value </strong><strong> val maxScale = (size.width + 16f.dp.toPx()) / size.width </strong><strong> val scale = lerp(1f, maxScale, progress) </strong><strong> scaleX = scale </strong><strong> scaleY = scale </strong><strong> }, </strong> onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) .clickable(interactionSource = null, indication = null) {} .pointerInput(animationScope) { val animationSpec = spring(0.5f, 300f, 0.001f) awaitEachGesture { // press awaitFirstDown() animationScope.launch { progressAnimation.animateTo(1f, animationSpec) } // release waitForUpOrCancellation() animationScope.launch { progressAnimation.animateTo(0f, animationSpec) } } } .fillMaxHeight() .weight(1f) ) </code></pre> Press the bottom bar again. <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FCUEkEDqk1RNZbAWVCQ1G%2FScreenshot_20251024-212215_Catalog.png?alt=media&#x26;token=09498878-6cfa-4dbd-908e-d6a95fdaaa3f" alt=""><figcaption></figcaption></figure> It works correctly! The content will scale but the backdrop won't scale. {% endstep %} {% endstepper %} ## Final code <details> <summary>Final code</summary> ```kotlin Box(Modifier.fillMaxSize()) { val backgroundColor = Color.White val backdrop = rememberLayerBackdrop { drawRect(backgroundColor) drawContent() } MainNavHost( modifier = Modifier.layerBackdrop(backdrop) ) Row( Modifier .safeContentPadding() .height(64f.dp) .fillMaxWidth() .align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(8f.dp), verticalAlignment = Alignment.CenterVertically ) { val animationScope = rememberCoroutineScope() val progressAnimation = remember { Animatable(0f) } Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, layerBlock = { val progress = progressAnimation.value val maxScale = (size.width + 16f.dp.toPx()) / size.width val scale = lerp(1f, maxScale, progress) scaleX = scale scaleY = scale }, onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) .clickable(interactionSource = null, indication = null) {} .pointerInput(animationScope) { val animationSpec = spring(0.5f, 300f, 0.001f) awaitEachGesture { // press awaitFirstDown() animationScope.launch { progressAnimation.animateTo(1f, animationSpec) } // release waitForUpOrCancellation() animationScope.launch { progressAnimation.animateTo(0f, animationSpec) } } } .fillMaxHeight() .weight(1f) ) Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { ContinuousCapsule }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { val tint = Color(0xFF0088FF) drawRect(tint, blendMode = BlendMode.Hue) drawRect(tint.copy(alpha = 0.75f)) } ) .aspectRatio(1f) ) } } ``` </details></https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar>

<https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-sheet># Interactive Glass Bottom Bar <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FrUHiaDwXAId1w3KQ2OFO%2FScreenshot_20251024-212215_Catalog.png?alt=media&#x26;token=52e89db2-a54d-44a0-8295-6b67757111cc" alt=""><figcaption></figcaption></figure> ## Goals * Add "press to scale" animation to the glass bottom bar ## What you will learn * Handle the transformations (scale, rotation) correctly * Make use of `layerBlock` parameter of the `drawBackdrop` modifier ## Steps {% stepper %} {% step %} ### Press to scale Update the code for the glass bottom bar. <pre class="language-kotlin"><code class="lang-kotlin"><strong>val animationScope = rememberCoroutineScope() </strong><strong>val progressAnimation = remember { Animatable(0f) } </strong> Box( Modifier <strong> .graphicsLayer { </strong> val progress = progressAnimation.value val maxScale = (size.width + 16f.dp.toPx()) / size.width val scale = lerp(1f, maxScale, progress) scaleX = scale scaleY = scale } .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) <strong> .clickable {} </strong><strong> .pointerInput(animationScope) { </strong> val animationSpec = spring(0.5f, 300f, 0.001f) awaitEachGesture { // press awaitFirstDown() animationScope.launch { progressAnimation.animateTo(1f, animationSpec) } // release waitForUpOrCancellation() animationScope.launch { progressAnimation.animateTo(0f, animationSpec) } } } .fillMaxHeight() .weight(1f) ) </code></pre> Press the bottom bar. <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FzelkA8KsIEuRLSWOnRn8%2FScreenshot_20251024-212145_Catalog.png?alt=media&#x26;token=ccd57d9f-4969-440b-8f13-a7c34419c935" alt=""><figcaption></figcaption></figure> Oops! The backdrop is misplaced, it will also scale with the bottom bar. **But the backdrop shouldn't scale**. {% endstep %} {% step %} ### Prevent backdrop from scaling Move the code in `graphicsLayer` to `layerBlock` in the `drawBackdrop` modifier. <pre class="language-kotlin"><code class="lang-kotlin">val animationScope = rememberCoroutineScope() val progressAnimation = remember { Animatable(0f) } Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, <strong> layerBlock = { </strong><strong> val progress = progressAnimation.value </strong><strong> val maxScale = (size.width + 16f.dp.toPx()) / size.width </strong><strong> val scale = lerp(1f, maxScale, progress) </strong><strong> scaleX = scale </strong><strong> scaleY = scale </strong><strong> }, </strong> onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) .clickable(interactionSource = null, indication = null) {} .pointerInput(animationScope) { val animationSpec = spring(0.5f, 300f, 0.001f) awaitEachGesture { // press awaitFirstDown() animationScope.launch { progressAnimation.animateTo(1f, animationSpec) } // release waitForUpOrCancellation() animationScope.launch { progressAnimation.animateTo(0f, animationSpec) } } } .fillMaxHeight() .weight(1f) ) </code></pre> Press the bottom bar again. <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FCUEkEDqk1RNZbAWVCQ1G%2FScreenshot_20251024-212215_Catalog.png?alt=media&#x26;token=09498878-6cfa-4dbd-908e-d6a95fdaaa3f" alt=""><figcaption></figcaption></figure> It works correctly! The content will scale but the backdrop won't scale. {% endstep %} {% endstepper %} ## Final code <details> <summary>Final code</summary> ```kotlin Box(Modifier.fillMaxSize()) { val backgroundColor = Color.White val backdrop = rememberLayerBackdrop { drawRect(backgroundColor) drawContent() } MainNavHost( modifier = Modifier.layerBackdrop(backdrop) ) Row( Modifier .safeContentPadding() .height(64f.dp) .fillMaxWidth() .align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(8f.dp), verticalAlignment = Alignment.CenterVertically ) { val animationScope = rememberCoroutineScope() val progressAnimation = remember { Animatable(0f) } Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { CircleShape }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, layerBlock = { val progress = progressAnimation.value val maxScale = (size.width + 16f.dp.toPx()) / size.width val scale = lerp(1f, maxScale, progress) scaleX = scale scaleY = scale }, onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) } ) .clickable(interactionSource = null, indication = null) {} .pointerInput(animationScope) { val animationSpec = spring(0.5f, 300f, 0.001f) awaitEachGesture { // press awaitFirstDown() animationScope.launch { progressAnimation.animateTo(1f, animationSpec) } // release waitForUpOrCancellation() animationScope.launch { progressAnimation.animateTo(0f, animationSpec) } } } .fillMaxHeight() .weight(1f) ) Box( Modifier .drawBackdrop( backdrop = backdrop, shape = { ContinuousCapsule }, effects = { vibrancy() blur(4f.dp.toPx()) lens(16f.dp.toPx(), 32f.dp.toPx()) }, onDrawSurface = { val tint = Color(0xFF0088FF) drawRect(tint, blendMode = BlendMode.Hue) drawRect(tint.copy(alpha = 0.75f)) } ) .aspectRatio(1f) ) } } ``` </details></https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-sheet>

<https://kyant.gitbook.io/backdrop/tutorials/glass-slider># Glass Slider <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FBGTi35muvdJB6rHCB4DB%2FIMG_20251024_231625.png?alt=media&#x26;token=198dc0cf-51b1-42ee-9185-b9f10318ae94" alt=""><figcaption></figcaption></figure> ## Goals * Create a glass slider based on the code: <pre class="language-kotlin"><code class="lang-kotlin">Box(Modifier.fillMaxSize()) { val backgroundColor = Color.White val backdrop = rememberLayerBackdrop { drawRect(backgroundColor) drawContent() } MainNavHost( modifier = Modifier.layerBackdrop(backdrop) ) <strong> GlassSlider(backdrop = backdrop) </strong>} </code></pre> ## What you will learn * Combine multiple backdrops by using the `rememberCombinedBackdrop` function ## Steps {% stepper %} {% step %} ### Create a GlassSlider <pre class="language-kotlin" data-title="GlassSlider.kt" data-line-numbers><code class="lang-kotlin">// your package import androidx.compose.foundation.background import androidx.compose.foundation.layout.Box import androidx.compose.foundation.layout.BoxWithConstraints import androidx.compose.foundation.layout.fillMaxWidth import androidx.compose.foundation.layout.height import androidx.compose.foundation.layout.offset import androidx.compose.foundation.layout.padding import androidx.compose.foundation.layout.size import androidx.compose.foundation.shape.CircleShape import androidx.compose.runtime.Composable import androidx.compose.ui.Alignment import androidx.compose.ui.Modifier import androidx.compose.ui.graphics.Color import androidx.compose.ui.unit.dp import com.kyant.backdrop.Backdrop import com.kyant.backdrop.backdrops.layerBackdrop import com.kyant.backdrop.backdrops.rememberLayerBackdrop import com.kyant.backdrop.drawBackdrop import com.kyant.backdrop.effects.lens @Composable fun GlassSlider( <strong> backdrop: Backdrop, </strong> modifier: Modifier = Modifier ) { BoxWithConstraints( modifier .padding(horizontal = 24f.dp) .fillMaxWidth(), contentAlignment = Alignment.CenterStart ) { <strong> val trackBackdrop = rememberLayerBackdrop() </strong> // track Box( Modifier <strong> .layerBackdrop(trackBackdrop) </strong> .background(Color(0xFF0088FF), CircleShape) .height(6f.dp) .fillMaxWidth() ) // thumb Box( Modifier .offset(x = maxWidth / 2f - 28f.dp) .drawBackdrop( <strong> // We want to draw both of `backdrop` and `trackBackdrop` </strong><strong> backdrop = trackBackdrop, </strong> shape = { CircleShape }, effects = { lens( refractionHeight = 12f.dp.toPx(), refractionAmount = 16f.dp.toPx(), chromaticAberration = true ) } ) .size(56f.dp, 32f.dp) ) } } </code></pre> {% endstep %} {% step %} ### Replace line 51 in GlassSlider.kt with 1. `trackBackdrop` <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2F8YutSRSD4Gtn5wMrzwb5%2FScreenshot_20251024-230428_Catalog.png?alt=media&#x26;token=f6013c02-5a7a-4d63-8c10-4c41c25b2bbb" alt="" width="188"><figcaption></figcaption></figure> 2. `backdrop` <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FphQO4BEDPFGWLYIoRZfB%2FScreenshot_20251024-230416_Catalog.png?alt=media&#x26;token=84cf0a97-d7ee-45fe-ac83-5319f3d50884" alt="" width="188"><figcaption></figcaption></figure> 3. `rememberCombinedBackdrop(backdrop, trackBackdrop)` <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FW1v1VhBoy9UFuPWHKjo7%2FScreenshot_20251024-230449_Catalog.png?alt=media&#x26;token=8871e53d-e5b8-4529-8586-4f8ad0b71936" alt="" width="188"><figcaption></figcaption></figure> Background and track are refracted by thumb simultaneously. {% endstep %} {% endstepper %} Background and track are refracted by thumb simultaneously.</https://kyant.gitbook.io/backdrop/tutorials/glass-slider>

<https://kyant.gitbook.io/backdrop/tutorials/smoother-rounded-corners># Smoother rounded corners The [Capsule](https://github.com/Kyant0/Capsule) library allows you to create beautiful and smooth G2 continuous rounded rectangles. In the previous tutorials, all shapes are created by the library. Can you tell the difference? <figure><img src="https://1718938796-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FWrt18MksRj8JDd4YEdiJ%2Fuploads%2FWJ88IgrTbVXLJoQe2Tog%2FIMG_20251024_232826.png?alt=media&#x26;token=fc1ef0fd-0516-4737-bac9-b35b2e975ef4" alt=""><figcaption></figcaption></figure> If you prefer the right one, the Capsule library is your right choice.</https://kyant.gitbook.io/backdrop/tutorials/smoother-rounded-corners>
