package es.sergiosahuquillo.burnfiles.spike.benchmark

enum class Strategy(val parallelism: Int) {
    NIO_P1(1),
    NIO_P4(4),
    IO_P1(1),
    IO_P4(4),
    MEDIASTORE(1)
}

enum class DatasetKind { SYNTHETIC, REAL_READ_ONLY }

enum class CacheState { COLD, WARM }

enum class RunOutcome { OK, CANCELLED, ACCESS_LOST, ERROR }

data class Counts(val files: Long = 0, val folders: Long = 0, val bytes: Long = 0)

data class BenchmarkRun(
    val strategy: Strategy,
    val dataset: DatasetKind,
    val repetition: Int,
    val cache: CacheState,
    val durationMs: Long,
    val counts: Counts,
    val outcome: RunOutcome
) {
    val filesPerSecond: Double
        get() = if (durationMs <= 0) 0.0 else counts.files * MILLIS_PER_SECOND / durationMs

    private companion object {
        const val MILLIS_PER_SECOND = 1000.0
    }
}
