package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.ui.common.UsbTreePickerEffect
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.volumeLine
import es.sergiosahuquillo.burnfiles.spike.usb.UsbProbeResult
import es.sergiosahuquillo.burnfiles.spike.usb.UsbViewModel

/** Initial focus: first volume, or "Scan again" when none is connected. */
@Composable
fun TvUsbScreen() {
    val viewModel = spikeViewModel { UsbViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val firstVolume = remember { FocusRequester() }
    val rescan = remember { FocusRequester() }
    val runTest = remember { FocusRequester() }
    UsbTreePickerEffect(viewModel, state)

    TvScreenColumn {
        TvTitle(stringResource(R.string.screen_usb))
        if (!state.loading && state.volumes.isEmpty()) TvBody(stringResource(R.string.usb_none))
        state.volumes.forEachIndexed { position, volume ->
            // The list is full width, so ↓ from its last entry must go to "Run test" explicitly
            // (geometric search would pick the button nearest to the centre).
            val focus = if (position == 0) Modifier.focusRequester(firstVolume) else Modifier
            val last = position == state.volumes.lastIndex
            val toRun = if (last) Modifier.focusProperties { down = runTest } else Modifier
            ListItem(
                selected = state.selected == volume.index,
                onClick = { viewModel.select(volume.index) },
                modifier = focus.then(toRun),
                leadingContent = { RadioButton(selected = state.selected == volume.index, onClick = null) },
                headlineContent = { Text(volumeLine(volume)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Only the lack of a volume makes "Run test" unfocusable: while a test runs it keeps focus.
            Button(
                onClick = viewModel::run,
                enabled = state.selectedVolume != null && !state.running,
                modifier = Modifier.focusRequester(runTest).focusableWhen(state.selectedVolume != null)
            ) {
                Text(stringResource(R.string.usb_run))
            }
            OutlinedButton(
                onClick = viewModel::refresh,
                enabled = !state.running,
                modifier = Modifier.focusRequester(rescan)
            ) {
                Text(stringResource(R.string.usb_rescan))
            }
        }
        listOfNotNull(state.direct, state.tree).forEach { TvProbeResult(it) }
    }
    InitialFocus(if (state.volumes.isEmpty()) rescan else firstVolume, key = state.loading to state.volumes.size)
}

@Composable
private fun TvProbeResult(result: UsbProbeResult) {
    TvBody(stringResource(result.probe.label))
    TvFocusableInfoRow(stringResource(R.string.usb_op_detect), stringResource(result.detect.label))
    TvFocusableInfoRow(stringResource(R.string.usb_op_write), stringResource(result.write.label))
    TvFocusableInfoRow(stringResource(R.string.usb_op_read_verify), stringResource(result.readVerify.label))
    TvFocusableInfoRow(stringResource(R.string.usb_op_delete), stringResource(result.delete.label))
    result.errorKind?.let { TvFocusableInfoRow(stringResource(R.string.usb_error_kind), it.name) }
}
