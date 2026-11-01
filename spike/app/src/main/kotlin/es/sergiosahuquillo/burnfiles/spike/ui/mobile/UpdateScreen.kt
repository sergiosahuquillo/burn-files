package es.sergiosahuquillo.burnfiles.spike.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import es.sergiosahuquillo.burnfiles.spike.ui.common.MobileConfirmDialog
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.updateLastStep
import es.sergiosahuquillo.burnfiles.spike.ui.common.updateStatus
import es.sergiosahuquillo.burnfiles.spike.update.UpdatePhase
import es.sergiosahuquillo.burnfiles.spike.update.UpdateViewModel

@Composable
fun MobileUpdateScreen() {
    val viewModel = spikeViewModel { UpdateViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    BackHandler(enabled = state.phase == UpdatePhase.DOWNLOADING) { viewModel.requestCancel() }
    if (state.confirmCancel) {
        MobileConfirmDialog(
            title = stringResource(R.string.cancel_operation_title),
            body = stringResource(R.string.update_cancel_body),
            confirmLabel = stringResource(R.string.action_stop),
            onConfirm = viewModel::cancelDownload,
            onDismiss = viewModel::dismissCancel
        )
    }

    MobileScreenColumn {
        MobileSection {
            MobileInfoRow(
                stringResource(R.string.update_installed),
                stringResource(R.string.update_version_value, state.installedVersionName, state.installedVersionCode)
            )
            Text(updateStatus(state), style = MaterialTheme.typography.bodyMedium)
            if (state.phase == UpdatePhase.DOWNLOADING && state.total > 0) {
                LinearProgressIndicator(
                    progress = { (state.downloaded.toFloat() / state.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            updateLastStep(state)?.let { MobileInfoRow(stringResource(R.string.update_last_step), it) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::check, enabled = !state.busy) { Text(stringResource(R.string.update_check)) }
            OutlinedButton(onClick = viewModel::download, enabled = state.phase == UpdatePhase.FOUND) {
                Text(stringResource(R.string.update_download))
            }
            OutlinedButton(
                onClick = { activity?.let(viewModel::install) },
                enabled = state.phase == UpdatePhase.VERIFIED || state.phase == UpdatePhase.NEEDS_PERMISSION
            ) { Text(stringResource(R.string.update_install)) }
            if (state.phase == UpdatePhase.DOWNLOADING) {
                OutlinedButton(onClick = viewModel::requestCancel) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    }
}
