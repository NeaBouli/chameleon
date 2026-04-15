/*
 * Chameleon — Core Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core

import android.os.Parcel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProcessTextResult — AIDL Parcelable")
class ProcessTextResultTest {

    private var parcelAvailable = false

    @BeforeEach
    fun checkParcel() {
        parcelAvailable = try {
            Parcel.obtain().recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    @Test
    @DisplayName("shouldReplace=false with null text is valid passthrough")
    fun `passthrough result has correct defaults`() {
        val result = ProcessTextResult(shouldReplace = false, text = null)
        assertFalse(result.shouldReplace)
        assertNull(result.text)
    }

    @Test
    @DisplayName("shouldReplace=true with text is valid replacement")
    fun `replacement result carries text`() {
        val result = ProcessTextResult(shouldReplace = true, text = "encrypted_payload_xyz")
        assertTrue(result.shouldReplace)
        assertEquals("encrypted_payload_xyz", result.text)
    }

    @Test
    @DisplayName("Parcel round-trip preserves shouldReplace=true")
    fun `parcel roundtrip with replacement`() {
        assumeTrue(parcelAvailable, "Parcel not available in JVM")
        val original = ProcessTextResult(shouldReplace = true, text = "Hello Chameleon")
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = ProcessTextResult(parcel)
        parcel.recycle()

        assertEquals(original.shouldReplace, restored.shouldReplace)
        assertEquals(original.text, restored.text)
    }

    @Test
    @DisplayName("Parcel round-trip preserves shouldReplace=false with null text")
    fun `parcel roundtrip with passthrough`() {
        assumeTrue(parcelAvailable, "Parcel not available in JVM")
        val original = ProcessTextResult(shouldReplace = false, text = null)
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = ProcessTextResult(parcel)
        parcel.recycle()

        assertFalse(restored.shouldReplace)
        assertNull(restored.text)
    }

    @Test
    @DisplayName("describeContents returns 0")
    fun `describeContents is zero`() {
        assertEquals(0, ProcessTextResult(false, null).describeContents())
    }

    @Test
    @DisplayName("CREATOR.newArray creates correct size")
    fun `creator newArray allocates correctly`() {
        val array = ProcessTextResult.CREATOR.newArray(5)
        assertEquals(5, array.size)
        assertTrue(array.all { it == null })
    }

    @Test
    @DisplayName("data class equality works")
    fun `equality check`() {
        val a = ProcessTextResult(true, "test")
        val b = ProcessTextResult(true, "test")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("data class inequality on different text")
    fun `inequality on text`() {
        val a = ProcessTextResult(true, "alpha")
        val b = ProcessTextResult(true, "beta")
        assertNotEquals(a, b)
    }
}
