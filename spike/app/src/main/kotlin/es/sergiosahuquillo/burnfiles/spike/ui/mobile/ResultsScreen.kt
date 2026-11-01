package es.sergiosahuquillo.burnfiles.spike.ui.mobile

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.log.ResultsViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.entrySummary
import es.sergiosahuquillo.burnfiles.spike.ui.common.exportMessage
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.rememberFormat
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel

@Composable
fun MobileResultsScreen() {
    val viewModel = spikeViewModel { ResultsViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val format = rememberFormat()

    MobileScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { activity?.let(viewModel::share) }) { Text(stringResource(R.string.results_share)) }
            OutlinedButton(onClick = viewModel::copyToDownloads) { Text(stringResource(R.string.results_copy)) }
        }
        state.message?.let { message ->
            val (text, isError) = exportMessage(message)
            MobileMessage(text, isError)
        }
        MobileSection(title = stringResource(R.string.results_adb)) {
            Text(viewModel.adbCommand, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
        MobileSection(title = stringResource(R.string.results_summary)) {
            state.summary.forEach { row ->
                MobileInfoRow(
                    stringResource(row.test.label),
                    stringResource(R.string.results_summary_value, row.ok, row.total)
                )
            }
        }
        MobileSection(title = stringResource(R.string.results_entries)) {
            if (state.entries.isEmpty()) Text(stringResource(R.string.results_empty))
            state.entries.take(MAX_ENTRIES).forEach { entry ->
                MobileInfoRow(
                    stringResource(
                        R.string.results_entry_title,
                        format.dateTime(entry.timestamp),
                        stringResource(entry.test.label)
                    ),
                    entrySummary(entry)
                )
            }
        }
    }
}

private const val MAX_ENTRIES = 50
