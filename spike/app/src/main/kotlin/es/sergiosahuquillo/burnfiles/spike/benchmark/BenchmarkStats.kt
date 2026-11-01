package es.sergiosahuquillo.burnfiles.spike.benchmark

data class StrategySummary(
    val strategy: Strategy,
    val dataset: DatasetKind,
    val runs: Int,
    val files: Long,
    val medianMs: Long,
    val worstMs: Long,
    val medianFilesPerSecond: Double
)

object BenchmarkStats {
    fun median(values: List<Long>): Long {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }

    fun medianDouble(values: List<Double>): Double {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }

    /** Median and worst per strategy and dataset, over successful runs only. */
    fun summarize(runs: List<BenchmarkRun>): List<StrategySummary> = runs
        .filter { it.outcome == RunOutcome.OK }
        .groupBy { it.strategy to it.dataset }
        .map { (key, group) ->
            StrategySummary(
                strategy = key.first,
                dataset = key.second,
                runs = group.size,
                files = group.maxOf { it.counts.files },
                medianMs = median(group.map { it.durationMs }),
                worstMs = group.maxOf { it.durationMs },
                medianFilesPerSecond = medianDouble(group.map { it.filesPerSecond })
            )
        }.sortedWith(compareBy({ it.dataset }, { it.strategy }))
}
