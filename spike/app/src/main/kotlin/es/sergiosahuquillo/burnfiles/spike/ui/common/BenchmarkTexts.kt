package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkMessage
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkPhase
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkUiState
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetState
import es.sergiosahuquillo.burnfiles.spike.benchmark.StrategySummary

/** Texts of the benchmark screen shared by the mobile and TV layers. */
@Composable
fun datasetStatus(state: BenchmarkUiState): String {
    val format = rememberFormat()
    val marker = state.marker ?: return stringResource(R.string.benchmark_dataset_none)
    val files = marker.createdFiles
    val sizes = format.bytes(marker.totalBytes)
    return when (marker.state) {
        DatasetState.COMPLETE ->
            pluralStringResource(R.plurals.benchmark_dataset_complete, files, format.count(files.toLong()), sizes)

        DatasetState.INCOMPLETE, DatasetState.CLEANING ->
            stringResource(
                R.string.benchmark_dataset_incomplete,
                format.count(files.toLong()),
                format.count(marker.targetFiles.toLong())
            )
    }
}

@Composable
fun benchmarkProgress(state: BenchmarkUiState): String {
    val format = rememberFormat()
    return when (state.phase) {
        BenchmarkPhase.GENERATING ->
            stringResource(
                R.string.benchmark_progress_generating,
                format.count(state.processed),
                format.count(state.target)
            )

        BenchmarkPhase.CLEANING ->
            stringResource(
                R.string.benchmark_progress_cleaning,
                format.count(state.processed),
                format.count(state.target)
            )

        BenchmarkPhase.RUNNING ->
            stringResource(
                R.string.benchmark_progress_running,
                state.currentStrategy?.let { stringResource(it.label) } ?: "",
                state.currentRepetition,
                format.count(state.processed),
                format.seconds(state.elapsedMs)
            )

        BenchmarkPhase.IDLE -> ""
    }
}

/** Text and whether it is an error. */
@Composable
fun benchmarkMessage(message: BenchmarkMessage): Pair<String, Boolean> {
    val format = rememberFormat()
    return when (message) {
        is BenchmarkMessage.InsufficientSpace ->
            stringResource(
                R.string.benchmark_insufficient_space,
                format.bytes(message.requiredBytes),
                format.bytes(message.availableBytes)
            ) to true

        BenchmarkMessage.NeedsFullAccess -> stringResource(R.string.benchmark_needs_full_access) to true

        BenchmarkMessage.Cancelled -> stringResource(R.string.result_cancelled) to true

        BenchmarkMessage.Failed -> stringResource(R.string.result_failed) to true

        BenchmarkMessage.Done -> stringResource(R.string.benchmark_done) to false
    }
}

@Composable
fun summaryLine(summary: StrategySummary): String {
    val format = rememberFormat()
    return stringResource(
        R.string.benchmark_summary_value,
        format.seconds(summary.medianMs),
        format.seconds(summary.worstMs),
        format.count(summary.files),
        format.count(summary.medianFilesPerSecond.toLong()),
        summary.runs
    )
}
