package com.stealthx.crypto

import com.stealthx.shared.model.AccessTier
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EntitlementTokenVerifierTest {
    private val now = 1_800_000_000L
    private val keyPair = ChameleonCrypto.generateSigningKeyPair()

    @Test
    fun `valid token is bound to Chameleon device and product`() {
        val result = EntitlementTokenVerifier.verify(token(), b64(keyPair.first), "chameleon", "sx_device_1", now)
        assertEquals(AccessTier.PRO, result.tier)
        assertEquals("chameleon_pro_lifetime", result.productId)
    }

    @Test
    fun `accepts canonical Node server Chameleon token`() {
        val publicKey = "740mZDWfdYLeK5peW746i9QiLApdp6IoH2KcGrNDtMY"
        val serverToken = "dj0xCmlzcz1zdGVhbHRoeAphdWQ9Y2hhbWVsZW9uCnN1Yj1zeF90ZXN0X2RldmljZV8xCnRpZXI9RUxJVEUKcHJvZHVjdD1jaGFtZWxlb25fZWxpdGVfbGlmZXRpbWUKaWF0PTE3MjAwMDAwMDAKZXhwPTE3MjI1OTIwMDAKb3JkZXI9OGQzNmQ3MWQ2NTZmNGE5OWIwZjZiMmNlNDEzYjdlNTg.u9vw79BYUIZS3wYKcU6fFknHYz9tiMdWaXQqZCQOlVZhVqOHoTXKq1TbUuFaFJ5URKJyesPKSj0vaeDubkUUDg"
        val result = EntitlementTokenVerifier.verify(
            serverToken,
            publicKey,
            "chameleon",
            "sx_test_device_1",
            1_720_000_010L
        )
        assertEquals(AccessTier.ELITE, result.tier)
        assertEquals("chameleon_elite_lifetime", result.productId)
    }

    @Test
    fun `SecureChat copied and expired tokens fail closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(token(product = "securechat_pro_lifetime"), b64(keyPair.first), "chameleon", "sx_device_1", now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(token(), b64(keyPair.first), "chameleon", "sx_other_device", now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(token(expiresAt = now - 1), b64(keyPair.first), "chameleon", "sx_device_1", now)
        }
    }

    @Test
    fun `product and tier mismatch fails closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(
                token(product = "chameleon_pro_lifetime", tier = "ELITE"),
                b64(keyPair.first),
                "chameleon",
                "sx_device_1",
                now
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            EntitlementTokenVerifier.verify(
                token(product = "chameleon_unknown_lifetime", tier = "PRO"),
                b64(keyPair.first),
                "chameleon",
                "sx_device_1",
                now
            )
        }
    }

    private fun token(
        product: String = "chameleon_pro_lifetime",
        tier: String = "PRO",
        expiresAt: Long = now + 2_592_000
    ): String {
        val audience = if (product.startsWith("chameleon_")) "chameleon" else "securechat"
        val payload = listOf(
            "v=1", "iss=stealthx", "aud=$audience", "sub=sx_device_1", "tier=$tier",
            "product=$product", "iat=${now - 10}", "exp=$expiresAt",
            "order=${MessageDigest.getInstance("SHA-256").digest("order".toByteArray()).joinToString("") { "%02x".format(it) }.take(32)}"
        ).joinToString("\n")
        val encoded = b64(payload.toByteArray(StandardCharsets.UTF_8))
        return "$encoded.${b64(ChameleonCrypto.sign(encoded.toByteArray(StandardCharsets.UTF_8), keyPair.second))}"
    }

    private fun b64(value: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value)
}
