package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Generator + cleaner on a real temporary folder (FR-008, FR-010, SC-007). */
class DatasetLifecycleTest {
    @TempDir
    lateinit var storage: File

    private val small = DatasetProfile.DEFAULT.copy(
        id = "small",
        targetFiles = 500,
        targetFolders = 60,
        topLevelFolders = 5
    )

    private val datasetRoot get() = File(storage, "BurnFilesSpike/dataset")

    @Test
    fun `generates every planned file and marks the dataset complete`() = runBlocking {
        val plan = DatasetPlan.create(profile = small)
        val marker = DatasetGenerator(datasetRoot).generate(plan) {}
        assertEquals(DatasetState.COMPLETE, marker.state)
        assertEquals(plan.fileCount, marker.createdFiles)
        val written = datasetRoot.walkTopDown().filter { it.isFile && it.name != DatasetMarker.FILE_NAME }.toList()
        assertEquals(plan.fileCount, written.size)
        assertEquals(plan.totalBytes, written.sumOf { it.length() })
        assertEquals(DatasetState.COMPLETE, DatasetMarker.read(datasetRoot)?.state)
    }

    @Test
    fun `cleaning removes the whole dataset folder and nothing else`() = runBlocking {
        val neighbour = File(storage, "BurnFilesSpike/keep.txt").apply { parentFile.mkdirs() }
        neighbour.writeText("x")
        DatasetGenerator(datasetRoot).generate(DatasetPlan.create(profile = small)) {}

        DatasetCleaner(datasetRoot).clean {}

        assertFalse(datasetRoot.exists())
        assertTrue(neighbour.exists())
    }

    @Test
    fun `cleaner refuses folders that are not the spike dataset`() {
        val other = File(storage, "Pictures").apply { mkdirs() }
        assertThrows(IllegalStateException::class.java) { runBlocking { DatasetCleaner(other).clean {} } }
    }

    @Test
    fun `cleaner refuses a dataset folder without marker`() {
        datasetRoot.mkdirs()
        File(datasetRoot, "something").writeText("x")
        assertThrows(IllegalStateException::class.java) { runBlocking { DatasetCleaner(datasetRoot).clean {} } }
        assertTrue(File(datasetRoot, "something").exists())
    }

    @Test
    fun `an incomplete dataset is resumed rather than restarted`() = runBlocking {
        val plan = DatasetPlan.create(profile = small)
        DatasetMarker.forPlan(plan, 0, DatasetState.INCOMPLETE).write(datasetRoot)
        val marker = DatasetGenerator(datasetRoot).generate(plan) {}
        assertNotNull(marker)
        assertEquals(DatasetState.COMPLETE, marker.state)
    }

    @Test
    fun `not enough space is reported before writing anything`() {
        val huge = DatasetProfile.DEFAULT.copy(
            id = "huge",
            targetFiles = 20_000,
            largeMaxBytes = Int.MAX_VALUE / 4,
            smallShare = 0.0
        )
        val plan = DatasetPlan.create(profile = huge)
        assertTrue(plan.diskBytes() > storage.usableSpace, "test needs a plan bigger than the disk")
        assertThrows(InsufficientSpaceException::class.java) {
            runBlocking { DatasetGenerator(datasetRoot).generate(plan) {} }
        }
        assertFalse(File(datasetRoot, DatasetMarker.FILE_NAME).exists())
    }
}
