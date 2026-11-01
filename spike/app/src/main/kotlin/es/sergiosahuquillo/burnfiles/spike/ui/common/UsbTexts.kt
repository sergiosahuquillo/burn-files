package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.usb.UsbVolume

@Composable
fun volumeLine(volume: UsbVolume): String {
    val format = rememberFormat()
    val line =
        stringResource(
            R.string.usb_volume_line,
            volume.description,
            format.bytes(volume.freeBytes),
            format.bytes(volume.capacityBytes)
        )
    return if (volume.readOnly) stringResource(R.string.usb_volume_read_only, line) else line
}
