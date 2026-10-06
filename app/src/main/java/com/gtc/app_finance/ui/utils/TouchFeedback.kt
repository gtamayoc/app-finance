package com.gtc.app_finance.ui.utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Semantic touch haptic feedback types following mobile design principles.
 * Mapped to native Android HapticFeedbackConstants for low latency and zero permission overhead.
 */
enum class TouchHapticType {
    /** Subtle tick for selection, tab switching, and segmented controls (EFFECT_TICK) */
    TICK,
    /** Standard click for button presses and card taps (EFFECT_CLICK) */
    CLICK,
    /** Heavy feedback for deletions, destructive actions, and threshold crossing (EFFECT_HEAVY_CLICK) */
    HEAVY,
    /** Double pulse / warning feedback (EFFECT_DOUBLE_CLICK) */
    WARNING,
    /** Confirmation feedback for successful operations like saving (CONFIRM) */
    SUCCESS
}

/**
 * Controller responsible for dispatching optimized haptic feedback to the hosting Android View.
 */
class TouchFeedbackManager(private val view: View) {

    fun perform(type: TouchHapticType) {
        val constant = when (type) {
            TouchHapticType.TICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.SEGMENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
            }
            TouchHapticType.CLICK -> HapticFeedbackConstants.KEYBOARD_TAP
            TouchHapticType.HEAVY -> HapticFeedbackConstants.LONG_PRESS
            TouchHapticType.WARNING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
            TouchHapticType.SUCCESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                }
            }
        }

        try {
            view.performHapticFeedback(constant)
        } catch (_: Exception) {
            // Graceful fallback for devices without advanced haptic actuators
        }
    }
}

/**
 * Remember an instance of [TouchFeedbackManager] bound to the current Compose hierarchy View.
 */
@Composable
fun rememberTouchFeedback(): TouchFeedbackManager {
    val view = LocalView.current
    return remember(view) { TouchFeedbackManager(view) }
}
