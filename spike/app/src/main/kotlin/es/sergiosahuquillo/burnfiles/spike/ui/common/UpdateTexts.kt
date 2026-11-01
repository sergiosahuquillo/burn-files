package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.update.UpdatePhase
import es.sergiosahuquillo.burnfiles.spike.update.UpdateUiState
import java.time.Instant

/** Texts of the update screen shared by the mobile and TV layers. */
@Composable
fun updateStatus(state: UpdateUiState): String {
    val format = rememberFormat()
    val release = state.release
    return when (state.phase) {
        UpdatePhase.IDLE -> stringResource(R.string.update_status_idle)

        UpdatePhase.CHECKING -> stringResource(R.string.update_status_checking)

        UpdatePhase.FOUND ->
            stringResource(
                R.string.update_status_found,
                release?.versionName.orEmpty(),
                format.bytes(release?.apk?.size ?: 0)
            )

        UpdatePhase.UP_TO_DATE -> stringResource(R.string.update_status_up_to_date)

        UpdatePhase.DOWNLOADING ->
            stringResource(
                R.string.update_status_downloading,
                format.bytes(state.downloaded),
                format.bytes(state.total)
            )

        UpdatePhase.VERIFIED -> stringResource(R.string.update_status_verified)

        UpdatePhase.NEEDS_PERMISSION -> stringResource(R.string.update_status_needs_permission)

        UpdatePhase.INSTALLING -> stringResource(R.string.update_status_installing)

        UpdatePhase.FAILED -> {
            val reset = state.rateLimitResetEpochSeconds
            if (reset != null) {
                stringResource(R.string.update_status_rate_limited, format.dateTime(Instant.ofEpochSecond(reset)))
            } else {
                stringResource(R.string.update_status_failed)
            }
        }
    }
}

@Composable
fun updateLastStep(state: UpdateUiState): String? {
    val step = state.lastStep ?: return null
    val outcome = state.lastOutcome ?: return null
    return stringResource(R.string.results_entry_pair, stringResource(step.label), stringResource(outcome.label))
}
