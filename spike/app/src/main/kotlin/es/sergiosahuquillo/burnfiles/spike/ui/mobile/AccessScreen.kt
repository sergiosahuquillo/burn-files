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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun MobileAccessScreen() {
    val viewModel = spikeViewModel { AccessViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val format = rememberFormat()
    AccessPermissionEffect(viewModel, state)

    MobileScreenColumn {
        MobileSection {
            MobileInfoRow(stringResource(R.string.access_current_level), stringResource(state.level.label))
        }

        val step = state.step
        if (state.running && step != null) {
            MobileSection(title = stringResource(R.string.access_current_step, stringResource(step.label))) {
                Text(stringResource(step.help), style = MaterialTheme.typography.bodyMedium)
                if (step == AccessStep.ADB) {
                    Text(
                        viewModel.adbCommand,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { activity?.let(viewModel::runStep) }) { Text(stringResource(step.action)) }
                    if (step == AccessStep.ADB) {
                        OutlinedButton(onClick = { context.copyToClipboard(viewModel.adbCommand) }) {
                            Text(stringResource(R.string.action_copy))
                        }
                    }
                }
                if (step == AccessStep.ADB) {
                    OutlinedButton(onClick = viewModel::skipAdb) {
                        Text(stringResource(R.string.action_continue_limited))
                    }
                }
            }
        } else {
            Button(onClick = viewModel::start) {
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

        state.finalLevel?.let { final ->
            MobileSection(title = stringResource(R.string.access_result)) {
                MobileInfoRow(stringResource(R.string.access_final_level), stringResource(final.label))
                state.coverage?.let { coverage ->
                    MobileInfoRow(stringResource(R.string.coverage_images), format.count(coverage.images.toLong()))
                    MobileInfoRow(stringResource(R.string.coverage_video), format.count(coverage.video.toLong()))
                    MobileInfoRow(stringResource(R.string.coverage_audio), format.count(coverage.audio.toLong()))
                    MobileInfoRow(
                        stringResource(R.string.coverage_downloads),
                        format.count(coverage.downloads.toLong())
                    )
                }
            }
        }

        if (state.attempts.isNotEmpty()) {
            MobileSection(title = stringResource(R.string.access_attempts)) {
                state.attempts.forEach { attempt ->
                    MobileInfoRow(
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
    }
}
