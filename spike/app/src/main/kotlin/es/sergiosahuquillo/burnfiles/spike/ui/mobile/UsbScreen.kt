package es.sergiosahuquillo.burnfiles.spike.ui.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.ui.common.UsbTreePickerEffect
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.volumeLine
import es.sergiosahuquillo.burnfiles.spike.usb.UsbProbeResult
import es.sergiosahuquillo.burnfiles.spike.usb.UsbViewModel

@Composable
fun MobileUsbScreen() {
    val viewModel = spikeViewModel { UsbViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    UsbTreePickerEffect(viewModel, state)

    MobileScreenColumn {
        MobileSection(title = stringResource(R.string.usb_volumes)) {
            if (!state.loading && state.volumes.isEmpty()) {
                Text(stringResource(R.string.usb_none), style = MaterialTheme.typography.bodyMedium)
            }
            state.volumes.forEach { volume ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state.selected == volume.index, onClick = { viewModel.select(volume.index) })
                    Text(volumeLine(volume), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::run, enabled = state.selectedVolume != null && !state.running) {
                Text(stringResource(R.string.usb_run))
            }
            OutlinedButton(onClick = viewModel::refresh, enabled = !state.running) {
                Text(stringResource(R.string.usb_rescan))
            }
        }
        listOfNotNull(state.direct, state.tree).forEach { MobileProbeResult(it) }
    }
}

@Composable
private fun MobileProbeResult(result: UsbProbeResult) {
    MobileSection(title = stringResource(result.probe.label)) {
        MobileInfoRow(stringResource(R.string.usb_op_detect), stringResource(result.detect.label))
        MobileInfoRow(stringResource(R.string.usb_op_write), stringResource(result.write.label))
        MobileInfoRow(stringResource(R.string.usb_op_read_verify), stringResource(result.readVerify.label))
        MobileInfoRow(stringResource(R.string.usb_op_delete), stringResource(result.delete.label))
        result.errorKind?.let { MobileInfoRow(stringResource(R.string.usb_error_kind), it.name) }
    }
}
