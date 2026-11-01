package es.sergiosahuquillo.burnfiles.spike.log

import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetPlan
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetProfile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** FR-019 / constitution VI: the exported log never contains paths, file names or contents. */
class ResultsPrivacyTest {
    private val forbidden = listOf("/", "\\", "storage", "emulated", "BurnFilesSpike", "dataset/", "probe-")

    @Test
    fun `serialized entries of every test contain no path-like text`() {
        val lines = Fixtures.payloads.map { ResultsJson.encode(Fixtures.result(it)) }
        lines.forEach { line ->
            // The timestamp and device fields are the only strings; neither may look like a path.
            forbidden.forEach { token -> assertFalse(line.contains(token), "'$token' leaked into: $line") }
        }
    }

    @Test
    fun `no synthetic dataset name ends up in the log`() {
        val plan = DatasetPlan.create(profile = DatasetProfile.DEFAULT.copy(targetFiles = 200, targetFolders = 20))
        val names = (0 until plan.fileCount).map { plan.filePath(it).substringAfterLast('/') }
        val log = Fixtures.payloads.joinToString("\n") { ResultsJson.encode(Fixtures.result(it)) }
        names.forEach { assertFalse(log.contains(it)) }
    }
}
