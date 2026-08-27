package com.stealthx.data.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StealthXApiTlsTest {
    @Test
    fun `shared client builder installs all api pins`() {
        val client = StealthXApiTls.newClientBuilder().build()

        assertEquals(StealthXApiTls.certificatePinner.pins, client.certificatePinner.pins)
        assertEquals(3, client.certificatePinner.pins.size)
        assertEquals(
            setOf("api.stealthx.tech"),
            client.certificatePinner.pins.map { it.pattern }.toSet(),
        )
    }
}
