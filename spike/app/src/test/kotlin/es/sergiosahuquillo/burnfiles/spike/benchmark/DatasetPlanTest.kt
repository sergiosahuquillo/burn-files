package es.sergiosahuquillo.burnfiles.spike.benchmark

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DatasetPlanTest {
    private val plan = DatasetPlan.create()

    @Test
    fun `same seed and profile give the same tree`() {
        assertTrue(plan.sameAs(DatasetPlan.create(DatasetPlan.DEFAULT_SEED, DatasetProfile.DEFAULT)))
        assertEquals(plan.filePath(12_345), DatasetPlan.create().filePath(12_345))
    }

    @Test
    fun `another seed gives another tree`() {
        assertFalse(plan.sameAs(DatasetPlan.create(seed = 42)))
    }

    @Test
    fun `default seed is the documented one`() {
        assertEquals(0xB0FF11E5L, DatasetPlan.DEFAULT_SEED)
    }

    @Test
    fun `at least 100k files in about 4k folders`() {
        assertTrue(plan.fileCount >= 100_000)
        assertTrue(plan.folderCount in 3_500..4_000, "folders = ${plan.folderCount}")
    }

    @Test
    fun `depth goes from 1 to 6`() {
        val depths = (0 until plan.folderCount).map(plan::folderDepth)
        assertEquals(1, depths.min())
        assertTrue(depths.max() <= 6)
        assertTrue(depths.max() >= 4, "the tree must actually be deep")
    }

    @Test
    fun `97 percent small files and 3 percent up to 64 KiB`() {
        val sizes = (0 until plan.fileCount).map(plan::fileSize)
        val small = sizes.count { it in 1..2048 }.toDouble() / sizes.size
        assertEquals(0.97, small, 0.005)
        assertTrue(sizes.all { it in 1..65_536 })
        val totalMiB = plan.totalBytes / (1024.0 * 1024.0)
        assertTrue(totalMiB in 150.0..250.0, "total = $totalMiB MiB")
    }

    @Test
    fun `paths are relative, unique and nested under their folder`() {
        val paths = (0 until plan.fileCount).map(plan::filePath)
        assertEquals(paths.size, paths.toSet().size)
        assertTrue(paths.none { it.startsWith("/") })
        assertTrue(paths.all { it.substringBefore('/').startsWith("d") })
    }

    @Test
    fun `disk usage accounts for one block per file at least`() {
        assertTrue(plan.diskBytes() >= plan.fileCount.toLong() * 4096)
    }
}
