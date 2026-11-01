package es.sergiosahuquillo.burnfiles.spike.benchmark

import android.content.ContentResolver
import es.sergiosahuquillo.burnfiles.spike.access.AccessMonitor
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import es.sergiosahuquillo.burnfiles.spike.log.ResultSink
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException

sealed interface BenchmarkEvent {
    data class Progress(val strategy: Strategy, val repetition: Int, val counts: Counts, val elapsedMs: Long) :
        BenchmarkEvent

    data class Finished(val run: BenchmarkRun) : BenchmarkEvent
}

/** Runs every available strategy [REPETITIONS] times and records one BENCHMARK entry per run. */
class BenchmarkRunner(
    private val storageRoot: File,
    private val datasetRoot: File,
    private val resolver: ContentResolver,
    private val access: AccessMonitor,
    private val sink: ResultSink,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var coldPending = true

    /** The next run starts with cold caches (after app start or after generating the dataset). */
    fun markCold() {
        coldPending = true
    }

    suspend fun run(dataset: DatasetKind, onEvent: (BenchmarkEvent) -> Unit): List<BenchmarkRun> {
        val fullAccess = access.peek().isFull
        val strategies = if (fullAccess) Strategy.entries else listOf(Strategy.MEDIASTORE)
        val results = mutableListOf<BenchmarkRun>()
        for (strategy in strategies) {
            for (repetition in 1..REPETITIONS) {
                val run = runOnce(strategy, dataset, repetition, onEvent)
                results += run
                sink.record(Payload.Benchmark(run))
                onEvent(BenchmarkEvent.Finished(run))
                if (run.outcome == RunOutcome.ACCESS_LOST) return results
            }
        }
        return results
    }

    private suspend fun runOnce(
        strategy: Strategy,
        dataset: DatasetKind,
        repetition: Int,
        onEvent: (BenchmarkEvent) -> Unit
    ): BenchmarkRun {
        val cache = if (coldPending) CacheState.COLD else CacheState.WARM
        coldPending = false
        val start = clock()
        var last = Counts()
        val progress = { counts: Counts ->
            last = counts
            onEvent(BenchmarkEvent.Progress(strategy, repetition, counts, clock() - start))
        }
        fun result(outcome: RunOutcome) =
            BenchmarkRun(strategy, dataset, repetition, cache, clock() - start, last, outcome)

        if (strategy != Strategy.MEDIASTORE && !access.peek().isFull) return result(RunOutcome.ACCESS_LOST)
        return try {
            last =
                when (strategy) {
                    Strategy.MEDIASTORE ->
                        MediaStoreTraversal(resolver).run(scopeFor(dataset), storageRoot, progress)

                    else -> FileTraversal(strategy, excludedDirs()).run(rootFor(dataset), onProgress = progress)
                }
            result(RunOutcome.OK)
        } catch (cancelled: CancellationException) {
            sink.record(Payload.Benchmark(result(RunOutcome.CANCELLED)))
            throw cancelled
        } catch (_: SecurityException) {
            result(RunOutcome.ACCESS_LOST)
        } catch (_: IOException) {
            result(if (access.peek().isFull) RunOutcome.ERROR else RunOutcome.ACCESS_LOST)
        }
    }

    private fun rootFor(dataset: DatasetKind) = if (dataset == DatasetKind.SYNTHETIC) datasetRoot else storageRoot

    private fun scopeFor(dataset: DatasetKind) = if (dataset == DatasetKind.SYNTHETIC) datasetRoot else null

    private fun excludedDirs() = listOf(File(storageRoot, "Android/data"), File(storageRoot, "Android/obb"))

    companion object {
        const val REPETITIONS = 3
    }
}
