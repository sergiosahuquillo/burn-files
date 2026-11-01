package es.sergiosahuquillo.burnfiles.spike.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkPhase
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkUiState
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkViewModel
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetKind
import es.sergiosahuquillo.burnfiles.spike.ui.common.MobileConfirmDialog
import es.sergiosahuquillo.burnfiles.spike.ui.common.benchmarkMessage
import es.sergiosahuquillo.burnfiles.spike.ui.common.benchmarkProgress
import es.sergiosahuquillo.burnfiles.spike.ui.common.datasetStatus
import es.sergiosahuquillo.burnfiles.spike.ui.common.destructiveBody
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.summaryLine

@Composable
fun MobileBenchmarkScreen() {
    val viewModel = spikeViewModel { BenchmarkViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.busy) { viewModel.requestCancel() }
    BenchmarkDialogs(viewModel, state)

    MobileScreenColumn {
        MobileSection(title = stringResource(R.string.benchmark_dataset)) {
            Text(datasetStatus(state), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatasetKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.dataset == kind,
                        onClick = { viewModel.selectDataset(kind) },
                        enabled = !state.busy,
                        label = { Text(stringResource(kind.label)) }
                    )
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.busy) {
                Button(onClick = viewModel::requestCancel) { Text(stringResource(R.string.action_cancel)) }
            } else {
                Button(
                    onClick = viewModel::run,
                    enabled =
                        state.dataset == DatasetKind.REAL_READ_ONLY || state.datasetComplete
                ) {
                    Text(stringResource(R.string.benchmark_run))
                }
                OutlinedButton(onClick = viewModel::generate, enabled = !state.datasetComplete) {
                    Text(stringResource(R.string.benchmark_generate))
                }
                OutlinedButton(onClick = viewModel::requestClean, enabled = state.hasDataset) {
                    Text(stringResource(R.string.benchmark_clean))
                }
            }
        }

        if (state.busy) {
            MobileSection {
                Text(benchmarkProgress(state), style = MaterialTheme.typography.bodyMedium)
                if (state.target > 0 && state.phase != BenchmarkPhase.RUNNING) {
                    LinearProgressIndicator(
                        progress = { (state.processed.toFloat() / state.target).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        state.message?.let { message ->
            val (text, isError) = benchmarkMessage(message)
            MobileMessage(text, isError)
        }

        MobileSection(title = stringResource(R.string.benchmark_results)) {
            if (state.summaries.isEmpty()) {
                Text(stringResource(R.string.benchmark_no_results), style = MaterialTheme.typography.bodyMedium)
            }
            state.summaries.forEach { summary ->
                MobileInfoRow(
                    stringResource(
                        R.string.benchmark_summary_label,
                        stringResource(summary.strategy.label),
                        stringResource(summary.dataset.label)
                    ),
                    summaryLine(summary)
                )
            }
        }
    }
}

@Composable
private fun BenchmarkDialogs(viewModel: BenchmarkViewModel, state: BenchmarkUiState) {
    val marker = state.marker
    if (state.confirmClean && marker != null) {
        MobileConfirmDialog(
            title = stringResource(R.string.benchmark_clean_title),
            body = destructiveBody(marker.createdFiles.toLong(), marker.totalBytes),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::confirmClean,
            onDismiss = viewModel::dismissClean
        )
    }
    if (state.confirmCancel) {
        MobileConfirmDialog(
            title = stringResource(R.string.cancel_operation_title),
            body = stringResource(R.string.cancel_operation_body),
            confirmLabel = stringResource(R.string.action_stop),
            onConfirm = viewModel::cancel,
            onDismiss = viewModel::dismissCancel
        )
    }
}
