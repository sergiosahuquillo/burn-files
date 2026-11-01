package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.log.ResultsJson
import es.sergiosahuquillo.burnfiles.spike.log.ResultsViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.entrySummary
import es.sergiosahuquillo.burnfiles.spike.ui.common.exportMessage
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.rememberFormat
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel

/** Initial focus: "Export". The centre button expands an entry to its raw log line. */
@Composable
fun TvResultsScreen() {
    val viewModel = spikeViewModel { ResultsViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val format = rememberFormat()
    val exportFocus = remember { FocusRequester() }
    var expanded by remember { mutableStateOf<Int?>(null) }

    TvScreenColumn {
        TvTitle(stringResource(R.string.screen_results))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { activity?.let(viewModel::share) }, modifier = Modifier.focusRequester(exportFocus)) {
                Text(stringResource(R.string.results_share))
            }
            OutlinedButton(onClick = viewModel::copyToDownloads) { Text(stringResource(R.string.results_copy)) }
        }
        state.message?.let { message ->
            val (text, isError) = exportMessage(message)
            TvMessage(text, isError)
        }
        TvBody(stringResource(R.string.results_adb))
        Text(viewModel.adbCommand, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)

        TvBody(stringResource(R.string.results_summary))
        state.summary.forEach { row ->
            TvInfoRow(stringResource(row.test.label), stringResource(R.string.results_summary_value, row.ok, row.total))
        }

        TvBody(stringResource(R.string.results_entries))
        if (state.entries.isEmpty()) TvBody(stringResource(R.string.results_empty))
        state.entries.take(MAX_ENTRIES).forEachIndexed { index, entry ->
            TvFocusableInfoRow(
                label = stringResource(
                    R.string.results_entry_title,
                    format.dateTime(entry.timestamp),
                    stringResource(entry.test.label)
                ),
                value = entrySummary(entry),
                onClick = { expanded = if (expanded == index) null else index },
                details = if (expanded == index) ResultsJson.encode(entry) else null
            )
        }
    }
    InitialFocus(exportFocus)
}

private const val MAX_ENTRIES = 50
