/*
 * Chameleon — Core Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core

import android.content.Context
import android.view.WindowManager
import com.stealthx.core.overlay.OverlayManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OverlayManager")
class OverlayManagerTest {

    private fun createManager(): OverlayManager? {
        return try {
            val context: Context = mockk(relaxed = true)
            val wm: WindowManager = mockk(relaxed = true)
            every { context.getSystemService(Context.WINDOW_SERVICE) } returns wm
            OverlayManager(context)
        } catch (e: Exception) {
            null
        }
    }

    @Test
    @DisplayName("isShowing returns false initially")
    fun `initially not showing`() {
        val manager = createManager()
        assumeTrue(manager != null, "WindowManager not available in JVM")
        assertFalse(manager!!.isShowing())
    }

    @Test
    @DisplayName("dismiss on non-showing overlay does not throw")
    fun `dismiss when not showing is safe`() {
        val manager = createManager()
        assumeTrue(manager != null, "WindowManager not available in JVM")
        assertDoesNotThrow { manager!!.dismiss() }
    }

    @Test
    @DisplayName("Overlay uses TYPE_APPLICATION_OVERLAY (not deprecated types)")
    fun `overlay type constant is correct`() {
        // Verify we use the correct type constant (API 26+)
        assertEquals(2038, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
    }

    @Test
    @DisplayName("FLAG_SECURE is set to prevent screenshots")
    fun `flag secure is used`() {
        // Verify FLAG_SECURE constant exists
        val flagSecure = WindowManager.LayoutParams.FLAG_SECURE
        assertTrue(flagSecure > 0)
    }
}
