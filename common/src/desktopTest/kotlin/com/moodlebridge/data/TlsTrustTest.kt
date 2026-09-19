package com.moodlebridge.data

import java.security.cert.CertificateException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TlsTrustTest {

    @Test
    fun fingerprintOfFormatsHexWithColonsUpperCase() {
        assertEquals(
            "F9:3E:F7:9D:15:D5:54:B4:83:72:F1:1F:C4:F9:2B:2E:76:0A:7D:D7:2E:58:81:A4:72:F2:BC:7F:78:35:55:D2",
            fingerprintOf(byteArrayOf(0x00, 0x0F.toByte(), 0xFF.toByte()))
        )
        assertEquals(
            "E3:B0:C4:42:98:FC:1C:14:9A:FB:F4:C8:99:6F:B9:24:27:AE:41:E4:64:9B:93:4C:A4:95:99:1B:78:52:B8:55",
            fingerprintOf(byteArrayOf())
        )
    }

    @Test
    fun fingerprintIsDeterministic() {
        val bytes = "some certificate bytes".toByteArray()
        assertEquals(fingerprintOf(bytes), fingerprintOf(bytes))
    }

    @Test
    fun resolveTrustAcceptsWhenPinMatches() {
        val fp = "AB:CD:EF"
        val result = resolveTrust("moodle.example.com", fp) { host -> if (host == "moodle.example.com") fp else null }
        assertNull(result)
    }

    @Test
    fun resolveTrustAcceptsPinCaseInsensitively() {
        val result = resolveTrust("host", "AB:CD:EF") { host -> if (host == "host") "ab:cd:ef" else null }
        assertNull(result)
    }

    @Test
    fun resolveTrustRejectsWhenNoPinStored() {
        val untrusted = resolveTrust("moodle.example.com", "AB:CD:EF") { null }
        assertEquals(UntrustedCertificate("moodle.example.com", "AB:CD:EF"), untrusted)
    }

    @Test
    fun resolveTrustRejectsWhenPinDiffers() {
        val untrusted = resolveTrust("moodle.example.com", "AB:CD:EF") { host ->
            if (host == "moodle.example.com") "12:34:56" else null
        }
        assertEquals(UntrustedCertificate("moodle.example.com", "AB:CD:EF"), untrusted)
    }

    @Test
    fun resolveTrustUsesTheRequestedHost() {
        val untrusted = resolveTrust("host", "AB:CD:EF") { null }
        assertEquals("host", untrusted?.host)
        assertEquals("AB:CD:EF", untrusted?.fingerprint)
    }

    @Test
    fun findUntrustedCertFindsDirectException() {
        val expected = UntrustedCertificate("host", "AB:CD:EF")
        val found = findUntrustedCert(UntrustedCertificateException(expected))
        assertEquals(expected, found)
    }

    @Test
    fun findUntrustedCertUnwrapsNestedCauses() {
        val expected = UntrustedCertificate("host", "AB:CD:EF")
        val certException = CertificateException("trust anchor for certification path was not found")
        certException.initCause(UntrustedCertificateException(expected))
        val nested = RuntimeException("outer", certException)
        assertEquals(expected, findUntrustedCert(nested))
    }

    @Test
    fun findUntrustedCertReturnsNullForUnrelatedErrors() {
        assertNull(findUntrustedCert(RuntimeException("boom")))
        assertNull(findUntrustedCert(null))
    }
}