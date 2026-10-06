package com.gtc.app_finance

import android.view.View
import com.gtc.app_finance.ui.utils.TouchFeedbackManager
import com.gtc.app_finance.ui.utils.TouchHapticType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TouchFeedbackLogicTest {

    @Test
    fun testAllHapticTypesAreDefined() {
        val types = TouchHapticType.values()
        assertEquals(5, types.size)
        assertNotNull(TouchHapticType.valueOf("TICK"))
        assertNotNull(TouchHapticType.valueOf("CLICK"))
        assertNotNull(TouchHapticType.valueOf("HEAVY"))
        assertNotNull(TouchHapticType.valueOf("WARNING"))
        assertNotNull(TouchHapticType.valueOf("SUCCESS"))
    }

    @Test
    fun testPerformHapticFeedbackDispatchesSafely() {
        val mockView = mockk<View>(relaxed = true)
        every { mockView.performHapticFeedback(any()) } returns true

        val manager = TouchFeedbackManager(mockView)

        // Verify executing each type calls the underlying view without uncaught errors
        TouchHapticType.values().forEach { type ->
            manager.perform(type)
        }

        verify(atLeast = 5) { mockView.performHapticFeedback(any()) }
    }

    @Test
    fun testPerformHapticFeedbackHandlesExceptionsGracefully() {
        val mockView = mockk<View>(relaxed = true)
        every { mockView.performHapticFeedback(any()) } throws RuntimeException("Simulated OEM haptic failure")

        val manager = TouchFeedbackManager(mockView)

        // Should not throw or crash the app
        manager.perform(TouchHapticType.CLICK)
        manager.perform(TouchHapticType.TICK)
    }
}
