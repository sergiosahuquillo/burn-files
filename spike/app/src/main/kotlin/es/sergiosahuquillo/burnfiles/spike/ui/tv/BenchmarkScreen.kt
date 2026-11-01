package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkViewModel
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetKind
import es.sergiosahuquillo.burnfiles.spike.ui.common.TvConfirmDialog
import es.sergiosahuquillo.burnfiles.spike.ui.common.benchmarkMessage
import es.sergiosahuquillo.burnfiles.spike.ui.common.benchmarkProgress
import es.sergiosahuquillo.burnfiles.spike.ui.common.datasetStatus
import es.sergiosahuquillo.burnfiles.spike.ui.common.destructiveBody
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.summaryLine

/**
 * Initial focus: "Run benchmark", or "Generate data" when there is no complete dataset.
 * While running, focus jumps to "Cancel"; "Back" asks before cancelling.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBenchmarkScreen() {
    val viewModel = spikeViewModel { BenchmarkViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val runFocus = remember { FocusRequester() }
    val generateFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    BackHandler(enabled = state.busy) { viewModel.requestCancel() }

    val marker = state.marker
    if (state.confirmClean && marker != null) {
        TvConfirmDialog(
            title = stringResource(R.string.benchmark_clean_title),
            body = destructiveBody(marker.createdFiles.toLong(), marker.totalBytes),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::confirmClean,
            onDismiss = viewModel::dismissClean
        )
    }
    if (state.confirmCancel) {
        TvConfirmDialog(
            title = stringResource(R.string.cancel_operation_title),
            body = stringResource(R.string.cancel_operation_body),
            confirmLabel = stringResource(R.string.action_stop),
            onConfirm = viewModel::cancel,
            onDismiss = viewModel::dismissCancel
        )
    }

    val canRun = state.dataset == DatasetKind.REAL_READ_ONLY || state.datasetComplete
    TvScreenColumn {
        TvTitle(stringResource(R.string.screen_benchmark))
        TvBody(datasetStatus(state))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.busy) {
                Button(onClick = viewModel::requestCancel, modifier = Modifier.focusRequester(cancelFocus)) {
                    Text(stringResource(R.string.action_cancel))
                }
            } else {
                Button(onClick = viewModel::run, enabled = canRun, modifier = Modifier.focusRequester(runFocus)) {
                    Text(stringResource(R.string.benchmark_run))
                }
                OutlinedButton(
                    onClick = viewModel::generate,
                    enabled = !state.datasetComplete,
                    modifier = Modifier.focusRequester(generateFocus)
                ) { Text(stringResource(R.string.benchmark_generate)) }
                OutlinedButton(onClick = viewModel::requestClean, enabled = state.hasDataset) {
                    Text(stringResource(R.string.benchmark_clean))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DatasetKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.dataset == kind,
                    onClick = { viewModel.selectDataset(kind) },
                    enabled = !state.busy
                ) { Text(stringResource(kind.label)) }
            }
        }
        if (state.busy) TvBody(benchmarkProgress(state))
        state.message?.let { message ->
            val (text, isError) = benchmarkMessage(message)
            TvMessage(text, isError)
        }

        TvBody(stringResource(R.string.benchmark_results))
        if (state.summaries.isEmpty()) TvBody(stringResource(R.string.benchmark_no_results))
        state.summaries.forEach { summary ->
            TvFocusableInfoRow(
                stringResource(
                    R.string.benchmark_summary_label,
                    stringResource(summary.strategy.label),
                    stringResource(summary.dataset.label)
                ),
                summaryLine(summary)
            )
        }
    }

    val target =
        when {
            state.busy -> cancelFocus
            canRun -> runFocus
            !state.datasetComplete -> generateFocus
            else -> runFocus
        }
    InitialFocus(target, key = state.busy)
}
