package es.sergiosahuquillo.burnfiles.spike.update

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class Sha256VerifierTest {
    // sha256("abc")
    private val abc = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private val name = "burnfiles-spike-0.0.2.apk"

    @TempDir
    lateinit var dir: File

    @Test
    fun `digest matches the reference vector`() {
        assertEquals(abc, Sha256Verifier.digest("abc".toByteArray()))
    }

    @Test
    fun `file digest matches and mismatches`() {
        val file = File(dir, name).apply { writeText("abc") }
        assertTrue(Sha256Verifier.matches(file, abc))
        assertTrue(Sha256Verifier.matches(file, abc.uppercase()))
        assertFalse(Sha256Verifier.matches(file, abc.replaceFirst('b', 'c')))
    }

    @Test
    fun `parses sha256sum text and binary formats`() {
        assertEquals(abc, Sha256Verifier.parseChecksum("$abc  $name\n", name))
        assertEquals(abc, Sha256Verifier.parseChecksum("$abc *$name", name))
        assertEquals(abc, Sha256Verifier.parseChecksum("\n${abc.uppercase()}\n", name))
    }

    @Test
    fun `rejects checksums for another file or malformed content`() {
        assertNull(Sha256Verifier.parseChecksum("$abc  other.apk", name))
        assertNull(Sha256Verifier.parseChecksum("abc  $name", name))
        assertNull(Sha256Verifier.parseChecksum("", name))
        assertNull(Sha256Verifier.parseChecksum("<html>Not Found</html>", name))
    }
}
