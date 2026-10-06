package com.gtc.app_finance.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Reusable modifier applying tactile squash-and-stretch elasticity on press.
 * Runs strictly on the GPU RenderNode layer via [graphicsLayer] to avoid recomposition
 * and layout overhead, preserving smooth 60/120 FPS performance.
 *
 * @param minScale Target compression scale when pressed (e.g. 0.98f for cards, 0.94f for small buttons).
 * @param interactionSource Interaction source to observe press events.
 */
fun Modifier.bouncePress(
    minScale: Float = 0.96f,
    interactionSource: MutableInteractionSource
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) minScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncePressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin.Center
    }
}

/**
 * All-in-one clickable modifier with tactile spring bounce and native haptic feedback.
 *
 * @param minScale Target compression scale when pressed.
 * @param hapticType Haptic vibration to perform on click.
 * @param enabled Whether interaction is enabled.
 * @param onClick Callback triggered on click release.
 */
fun Modifier.bounceClickable(
    minScale: Float = 0.96f,
    hapticType: TouchHapticType? = TouchHapticType.CLICK,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val feedback = rememberTouchFeedback()

    this
        .bouncePress(minScale = minScale, interactionSource = interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // The tactile spring bounce provides the physical visual feedback
            enabled = enabled,
            onClick = {
                if (hapticType != null) {
                    feedback.perform(hapticType)
                }
                onClick()
            }
        )
}
