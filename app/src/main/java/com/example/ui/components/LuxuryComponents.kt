package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LuxuryGlassBorder
import com.example.ui.theme.LuxuryGlassSurface
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceOutline

/**
 * iOS-Grade Squircles, Glassmorphism, and Haptic-Enabled Interactive Cards
 */
@Composable
fun LuxuryCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    border: BorderStroke? = LuxuryGlassBorder,
    containerColor: Color = LuxuryGlassSurface,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val baseModifier = if (onClick != null) {
        modifier
            .bounceClick {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    } else {
        modifier
    }

    Card(
        modifier = baseModifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Tactile Spring Scale Bounce effect for primary touch targets (iOS feel)
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(
        targetValue = if (buttonState == ButtonState.Pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    this
        .scale(scale)
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState == ButtonState.Pressed) {
                    waitForUpOrCancellation()
                    ButtonState.Idle
                } else {
                    awaitFirstDown(false)
                    ButtonState.Pressed
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            onClick()
        }
}

private enum class ButtonState {
    Idle, Pressed
}

/**
 * Premium Shimmer Skeleton Box for fluid loading states (No jarring spinners)
 */
@Composable
fun ShimmerSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val shimmerColors = listOf(
        Color(0xFF1E293B).copy(alpha = 0.6f),
        Color(0xFF334155).copy(alpha = 0.85f),
        Color(0xFF1E293B).copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
    )
}
