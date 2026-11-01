package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.log.ExportMessage

/** Text and whether it is an error. */
@Composable
fun exportMessage(message: ExportMessage): Pair<String, Boolean> = when (message) {
    is ExportMessage.Copied -> stringResource(R.string.results_copied, message.fileName) to false
    ExportMessage.CopyFailed -> stringResource(R.string.results_copy_failed) to true
    ExportMessage.ShareUnavailable -> stringResource(R.string.results_share_unavailable) to true
}
