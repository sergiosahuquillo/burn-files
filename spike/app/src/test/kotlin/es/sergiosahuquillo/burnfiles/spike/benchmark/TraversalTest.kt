package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TraversalTest {
    @TempDir
    lateinit var root: File

    private lateinit var excluded: List<File>
    private lateinit var expected: Counts

    @BeforeEach
    fun tree() {
        // root/a.txt (3)  root/b/c.bin (5)  root/b/d/e.bin (7)  root/f/ (empty)
        // root/Android/media/m.bin (11)  root/Android/data/x/secret (13)  root/Android/obb/o (17)
        file("a.txt", 3)
        file("b/c.bin", 5)
        file("b/d/e.bin", 7)
        File(root, "f").mkdirs()
        file("Android/media/m.bin", 11)
        file("Android/data/x/secret", 13)
        file("Android/obb/o", 17)
        excluded = listOf(File(root, "Android/data"), File(root, "Android/obb"))
        // Folders: b, b/d, f, Android, Android/media → 5. Files: a, c, e, m → 4.
        expected = Counts(files = 4, folders = 5, bytes = 3L + 5 + 7 + 11)
    }

    @ParameterizedTest
    @EnumSource(Strategy::class, names = ["NIO_P1", "NIO_P4", "IO_P1", "IO_P4"])
    fun `every file strategy agrees and skips Android data and obb`(strategy: Strategy) = runBlocking {
        assertEquals(expected, FileTraversal(strategy, excluded).run(root))
    }

    @Test
    fun `progress is reported and ends with the final counts`() = runBlocking {
        val seen = mutableListOf<Counts>()
        val result = FileTraversal(Strategy.NIO_P4, excluded).run(root, progressIntervalMs = 1) {
            synchronized(seen) {
                seen +=
                    it
            }
        }
        assertTrue(seen.isNotEmpty())
        assertEquals(result, seen.last())
    }

    @Test
    fun `the media index strategy is not a file traversal`() {
        assertThrows(IllegalArgumentException::class.java) { FileTraversal(Strategy.MEDIASTORE, excluded) }
    }

    @ParameterizedTest
    @EnumSource(Strategy::class, names = ["NIO_P4", "IO_P4"])
    fun `cancellation stops a long traversal quickly`(strategy: Strategy) {
        val big = File(root, "big")
        repeat(40) { top -> repeat(50) { sub -> File(big, "t$top/s$sub").mkdirs() } }
        runBlocking {
            val job = async(Dispatchers.Default) { FileTraversal(strategy, excluded).run(big) }
            delay(1)
            val start = System.nanoTime()
            job.cancel()
            job.join()
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            assertTrue(job.isCancelled)
            // SC-004: effective in well under 2 s.
            assertTrue(elapsedMs < 2_000, "took $elapsedMs ms")
        }
    }

    private fun file(path: String, size: Int) {
        File(root, path).apply {
            parentFile.mkdirs()
            writeBytes(ByteArray(size))
        }
    }
}
