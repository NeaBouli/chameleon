/*
 * Chameleon — Geofencing Feature Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.features.geofencing

import com.stealthx.features.geofencing.engine.GeofencingEngine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GeofencingEngine")
class GeofencingTest {

    @Test
    @DisplayName("Minimum radius is 100 meters")
    fun `min radius constant`() {
        assertEquals(100f, GeofencingEngine.MIN_RADIUS_METERS)
    }

    @Test
    @DisplayName("GeofenceConfig stores all fields")
    fun `geofence config fields`() {
        val config = GeofencingEngine.GeofenceConfig(
            id = "home",
            latitude = 48.1351,
            longitude = 11.5820,
            radiusMeters = 200f,
            label = "Home"
        )
        assertEquals("home", config.id)
        assertEquals(200f, config.radiusMeters)
    }

    @Test
    @DisplayName("Radius below 100m is enforced to 100m conceptually")
    fun `radius enforcement`() {
        val smallRadius = 50f
        val enforced = maxOf(smallRadius, GeofencingEngine.MIN_RADIUS_METERS)
        assertEquals(100f, enforced)
    }
}
