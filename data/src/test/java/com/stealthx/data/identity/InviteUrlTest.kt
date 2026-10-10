/*
 * Chameleon — shared invite URL producer tests (CHA-14). Synthetic data only.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.identity

import java.net.URI
import java.net.URLDecoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StealthXIdentity — shared invite URL (fragment)")
class InviteUrlTest {

    private val x = "x".repeat(43)
    private val e = "e".repeat(43)
    private val s = "s".repeat(86)
    private val privateLink = "stealthx://add/sx_AAAAAAAAA?x=$x&e=$e&s=$s&c=1700000000000"
    private val publicLink = "$privateLink&h=%40demo_user"

    private fun decodedFragmentLink(url: String): String {
        val fragment = URI(url).rawFragment
        val params = fragment.split('&').map { it.substringBefore('=') to it.substringAfter('=') }
        assertEquals(listOf("app", "link"), params.map { it.first })
        assertEquals("chameleon", params[0].second)
        return URLDecoder.decode(params[1].second, "UTF-8")
    }

    @Test
    @DisplayName("bundle lives only in the fragment of /invite.html, never in path or query")
    fun `bundle not in server request url`() {
        for (link in listOf(privateLink, publicLink)) {
            val uri = URI(StealthXIdentity.buildInviteUrl(link))
            assertEquals("https", uri.scheme)
            assertEquals("stealthx.tech", uri.host)
            assertEquals("/invite.html", uri.path)
            assertNull(uri.rawQuery)
            val requestPart = uri.toString().substringBefore('#')
            assertEquals("https://stealthx.tech/invite.html", requestPart)
            assertFalse(requestPart.contains("sx_") || requestPart.contains("stealthx%3A") || requestPart.contains(x))
        }
    }

    @Test
    @DisplayName("canonical /invite/ path is not used")
    fun `does not use exact securecall path`() {
        assertFalse(URI(StealthXIdentity.buildInviteUrl(privateLink)).path.startsWith("/invite/"))
    }

    @Test
    @DisplayName("signed bytes survive encoding/decoding exactly (private and public handle paths)")
    fun `round trip is byte exact`() {
        assertEquals(privateLink, decodedFragmentLink(StealthXIdentity.buildInviteUrl(privateLink)))
        assertEquals(publicLink, decodedFragmentLink(StealthXIdentity.buildInviteUrl(publicLink)))
    }

    @Test
    @DisplayName("fragment encodes the link exactly once")
    fun `single encoding`() {
        val url = StealthXIdentity.buildInviteUrl(publicLink)
        assertTrue(url.contains("link=stealthx%3A%2F%2Fadd%2Fsx_AAAAAAAAA%3Fx%3D"))
        assertTrue(url.contains("%26h%3D%2540demo_user"))
    }

    @Test
    @DisplayName("malformed, pending, duplicate or unexpected input fails closed")
    fun `invalid input rejected`() {
        val bad = listOf(
            "",
            "stealthx://add/sx_AAAAAAAAA",
            "stealthx://add/sx_AAAAAAAAA?",
            "https://example.com/?x=$x&e=$e&s=$s&c=1",
            "stealthx://add/sx_AAAAAAAA0?x=$x&e=$e&s=$s&c=1",
            "stealthx://add/sx_AAAAAAAAA?x=$x&e=$e&c=1",
            "$privateLink&x=$x",
            "$privateLink&evil=1",
            "$privateLink#frag",
            "$privateLink bad",
            "$privateLink\n",
            "$privateLink&h=%40" + "a".repeat(600)
        )
        for (link in bad) {
            assertThrows(IllegalArgumentException::class.java, { StealthXIdentity.buildInviteUrl(link) }, link.take(40))
        }
    }
}
