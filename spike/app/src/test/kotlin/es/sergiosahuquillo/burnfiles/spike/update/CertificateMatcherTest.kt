package es.sergiosahuquillo.burnfiles.spike.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CertificateMatcherTest {
    private val debugKey = CertificateMatcher.fingerprint(byteArrayOf(1, 2, 3))
    private val otherKey = CertificateMatcher.fingerprint(byteArrayOf(9, 9, 9))

    @Test
    fun `fingerprint is a lowercase SHA-256 hex`() {
        assertEquals(64, debugKey.length)
        assertEquals(debugKey.lowercase(), debugKey)
        assertNotEquals(debugKey, otherKey)
    }

    @Test
    fun `same signer matches`() {
        assertTrue(CertificateMatcher.matches(setOf(debugKey), setOf(debugKey)))
    }

    @Test
    fun `different or extra signers do not match`() {
        assertFalse(CertificateMatcher.matches(setOf(debugKey), setOf(otherKey)))
        assertFalse(CertificateMatcher.matches(setOf(debugKey), setOf(debugKey, otherKey)))
    }

    @Test
    fun `an unsigned side never matches`() {
        assertFalse(CertificateMatcher.matches(emptySet(), emptySet()))
        assertFalse(CertificateMatcher.matches(setOf(debugKey), emptySet()))
    }
}
