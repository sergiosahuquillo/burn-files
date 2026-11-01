package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep
import es.sergiosahuquillo.burnfiles.spike.access.AccessViewModel
import es.sergiosahuquillo.burnfiles.spike.ui.common.AccessPermissionEffect
import es.sergiosahuquillo.burnfiles.spike.ui.common.action
import es.sergiosahuquillo.burnfiles.spike.ui.common.copyToClipboard
import es.sergiosahuquillo.burnfiles.spike.ui.common.help
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.rememberFormat
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel

/** Initial focus: "Start diagnosis" (then the same button runs each step). */
@Composable
fun TvAccessScreen() {
    val viewModel = spikeViewModel { AccessViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val format = rememberFormat()
    val primary = remember { FocusRequester() }
    AccessPermissionEffect(viewModel, state)

    TvScreenColumn {
        TvTitle(stringResource(R.string.screen_access))
        TvInfoRow(stringResource(R.string.access_current_level), stringResource(state.level.label))

        val step = state.step
        if (state.running && step != null) {
            TvBody(stringResource(R.string.access_current_step, stringResource(step.label)))
            TvBody(stringResource(step.help))
            if (step == AccessStep.ADB) {
                // One line, monospace, large enough for 3 m.
                Text(
                    viewModel.adbCommand,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.running && step != null) {
                Button(onClick = { activity?.let(viewModel::runStep) }, modifier = Modifier.focusRequester(primary)) {
                    Text(stringResource(step.action))
                }
                if (step == AccessStep.ADB) {
                    OutlinedButton(onClick = { context.copyToClipboard(viewModel.adbCommand) }) {
                        Text(stringResource(R.string.action_copy))
                    }
                    OutlinedButton(onClick = viewModel::skipAdb) {
                        Text(stringResource(R.string.action_continue_limited))
                    }
                }
            } else {
                Button(onClick = viewModel::start, modifier = Modifier.focusRequester(primary)) {
                    Text(
                        stringResource(
                            if (state.finalLevel ==
                                null
                            ) {
                                R.string.action_start_diagnosis
                            } else {
                                R.string.action_repeat_diagnosis
                            }
                        )
                    )
                }
            }
        }

        state.finalLevel?.let { final ->
            TvInfoRow(stringResource(R.string.access_final_level), stringResource(final.label))
            state.coverage?.let { coverage ->
                TvInfoRow(stringResource(R.string.coverage_images), format.count(coverage.images.toLong()))
                TvInfoRow(stringResource(R.string.coverage_video), format.count(coverage.video.toLong()))
                TvInfoRow(stringResource(R.string.coverage_audio), format.count(coverage.audio.toLong()))
                TvInfoRow(stringResource(R.string.coverage_downloads), format.count(coverage.downloads.toLong()))
            }
        }

        if (state.attempts.isNotEmpty()) {
            TvBody(stringResource(R.string.access_attempts))
            state.attempts.forEach { attempt ->
                TvFocusableInfoRow(
                    stringResource(attempt.step.label),
                    stringResource(
                        R.string.access_attempt_value,
                        stringResource(attempt.outcome.label),
                        stringResource(attempt.levelAfter.label)
                    )
                )
            }
        }
    }
    // Keep the primary action focused when the step (and so its label) changes.
    InitialFocus(primary, key = state.step to state.running)
}
