package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.ui.common.TvConfirmDialog
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.updateLastStep
import es.sergiosahuquillo.burnfiles.spike.ui.common.updateStatus
import es.sergiosahuquillo.burnfiles.spike.update.UpdatePhase
import es.sergiosahuquillo.burnfiles.spike.update.UpdateViewModel

/** Initial focus: "Check test release"; focus then follows the flow check → download → install. */
@Composable
fun TvUpdateScreen() {
    val viewModel = spikeViewModel { UpdateViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val checkFocus = remember { FocusRequester() }
    val downloadFocus = remember { FocusRequester() }
    val installFocus = remember { FocusRequester() }
    BackHandler(enabled = state.phase == UpdatePhase.DOWNLOADING) { viewModel.requestCancel() }
    if (state.confirmCancel) {
        TvConfirmDialog(
            title = stringResource(R.string.cancel_operation_title),
            body = stringResource(R.string.update_cancel_body),
            confirmLabel = stringResource(R.string.action_stop),
            onConfirm = viewModel::cancelDownload,
            onDismiss = viewModel::dismissCancel
        )
    }

    val canInstall = state.phase == UpdatePhase.VERIFIED || state.phase == UpdatePhase.NEEDS_PERMISSION
    TvScreenColumn {
        TvTitle(stringResource(R.string.screen_update))
        TvInfoRow(
            stringResource(R.string.update_installed),
            stringResource(R.string.update_version_value, state.installedVersionName, state.installedVersionCode)
        )
        TvBody(updateStatus(state))
        updateLastStep(state)?.let { TvInfoRow(stringResource(R.string.update_last_step), it) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = viewModel::check, enabled = !state.busy, modifier = Modifier.focusRequester(checkFocus)) {
                Text(stringResource(R.string.update_check))
            }
            OutlinedButton(
                onClick = viewModel::download,
                enabled = state.phase == UpdatePhase.FOUND,
                modifier = Modifier.focusRequester(downloadFocus)
            ) { Text(stringResource(R.string.update_download)) }
            OutlinedButton(
                onClick = { activity?.let(viewModel::install) },
                enabled = canInstall,
                modifier = Modifier.focusRequester(installFocus)
            ) { Text(stringResource(R.string.update_install)) }
            if (state.phase == UpdatePhase.DOWNLOADING) {
                OutlinedButton(onClick = viewModel::requestCancel) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    }

    val target =
        when {
            state.phase == UpdatePhase.FOUND -> downloadFocus
            canInstall -> installFocus
            else -> checkFocus
        }
    InitialFocus(target, key = state.phase)
}
