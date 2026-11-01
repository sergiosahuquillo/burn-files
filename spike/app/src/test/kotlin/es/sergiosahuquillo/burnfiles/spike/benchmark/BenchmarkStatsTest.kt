package es.sergiosahuquillo.burnfiles.spike.benchmark

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BenchmarkStatsTest {
    private fun run(
        strategy: Strategy,
        ms: Long,
        files: Long = 100_000,
        outcome: RunOutcome = RunOutcome.OK,
        dataset: DatasetKind = DatasetKind.SYNTHETIC
    ) = BenchmarkRun(strategy, dataset, 1, CacheState.WARM, ms, Counts(files = files), outcome)

    @Test
    fun `median of odd and even counts`() {
        assertEquals(20, BenchmarkStats.median(listOf(30, 10, 20)))
        assertEquals(15, BenchmarkStats.median(listOf(10, 20)))
        assertThrows(IllegalArgumentException::class.java) { BenchmarkStats.median(emptyList()) }
    }

    @Test
    fun `files per second is derived from duration`() {
        assertEquals(50_000.0, run(Strategy.NIO_P1, 2_000).filesPerSecond)
        assertEquals(0.0, run(Strategy.NIO_P1, 0).filesPerSecond)
    }

    @Test
    fun `summary uses successful runs only, grouped by strategy and dataset`() {
        val runs =
            listOf(
                run(Strategy.NIO_P4, 9_000),
                run(Strategy.NIO_P4, 10_000),
                run(Strategy.NIO_P4, 30_000),
                run(Strategy.NIO_P4, 1, outcome = RunOutcome.CANCELLED),
                run(Strategy.IO_P1, 20_000),
                run(Strategy.IO_P1, 20_000, dataset = DatasetKind.REAL_READ_ONLY)
            )
        val summaries = BenchmarkStats.summarize(runs)
        assertEquals(3, summaries.size)
        val nio = summaries.single { it.strategy == Strategy.NIO_P4 }
        assertEquals(3, nio.runs)
        assertEquals(10_000, nio.medianMs)
        assertEquals(30_000, nio.worstMs)
        assertEquals(10_000.0, nio.medianFilesPerSecond)
    }
}
