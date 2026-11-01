package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep
import es.sergiosahuquillo.burnfiles.spike.access.AttemptOutcome
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetKind
import es.sergiosahuquillo.burnfiles.spike.benchmark.RunOutcome
import es.sergiosahuquillo.burnfiles.spike.benchmark.Strategy
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import es.sergiosahuquillo.burnfiles.spike.log.TestKind
import es.sergiosahuquillo.burnfiles.spike.log.TestResult
import es.sergiosahuquillo.burnfiles.spike.update.UpdateOutcome
import es.sergiosahuquillo.burnfiles.spike.update.UpdateStep
import es.sergiosahuquillo.burnfiles.spike.usb.ProbeKind
import es.sergiosahuquillo.burnfiles.spike.usb.UsbOp

@get:StringRes
val AccessLevel.label: Int
    get() =
        when (this) {
            AccessLevel.FULL -> R.string.level_full
            AccessLevel.LEGACY_FULL -> R.string.level_legacy_full
            AccessLevel.MEDIA_ONLY -> R.string.level_media_only
            AccessLevel.NONE -> R.string.level_none
        }

@get:StringRes
val AccessStep.label: Int
    get() =
        when (this) {
            AccessStep.APP_SETTINGS -> R.string.step_app_settings
            AccessStep.GENERIC_SETTINGS -> R.string.step_generic_settings
            AccessStep.ADB -> R.string.step_adb
            AccessStep.RUNTIME_DIALOG -> R.string.step_runtime_dialog
            AccessStep.MEDIA_ONLY -> R.string.step_media_only
        }

/** What the developer is asked to do on each step. */
@get:StringRes
val AccessStep.help: Int
    get() =
        when (this) {
            AccessStep.APP_SETTINGS -> R.string.step_app_settings_help
            AccessStep.GENERIC_SETTINGS -> R.string.step_generic_settings_help
            AccessStep.ADB -> R.string.step_adb_help
            AccessStep.RUNTIME_DIALOG -> R.string.step_runtime_dialog_help
            AccessStep.MEDIA_ONLY -> R.string.step_media_only_help
        }

@get:StringRes
val AccessStep.action: Int
    get() =
        when (this) {
            AccessStep.APP_SETTINGS, AccessStep.GENERIC_SETTINGS -> R.string.action_open_settings
            AccessStep.ADB -> R.string.action_check_again
            AccessStep.RUNTIME_DIALOG, AccessStep.MEDIA_ONLY -> R.string.action_request_permission
        }

@get:StringRes
val AttemptOutcome.label: Int
    get() =
        when (this) {
            AttemptOutcome.OPENED -> R.string.outcome_opened
            AttemptOutcome.ACTIVITY_NOT_FOUND -> R.string.outcome_activity_not_found
            AttemptOutcome.SECURITY_EXCEPTION -> R.string.outcome_security_exception
            AttemptOutcome.GRANTED -> R.string.outcome_granted
            AttemptOutcome.DENIED -> R.string.outcome_denied
            AttemptOutcome.NO_EFFECT -> R.string.outcome_no_effect
            AttemptOutcome.SKIPPED -> R.string.outcome_skipped
        }

@get:StringRes
val Strategy.label: Int
    get() =
        when (this) {
            Strategy.NIO_P1 -> R.string.strategy_nio_p1
            Strategy.NIO_P4 -> R.string.strategy_nio_p4
            Strategy.IO_P1 -> R.string.strategy_io_p1
            Strategy.IO_P4 -> R.string.strategy_io_p4
            Strategy.MEDIASTORE -> R.string.strategy_mediastore
        }

@get:StringRes
val DatasetKind.label: Int
    get() =
        when (this) {
            DatasetKind.SYNTHETIC -> R.string.dataset_synthetic
            DatasetKind.REAL_READ_ONLY -> R.string.dataset_real
        }

@get:StringRes
val RunOutcome.label: Int
    get() =
        when (this) {
            RunOutcome.OK -> R.string.result_ok
            RunOutcome.CANCELLED -> R.string.result_cancelled
            RunOutcome.ACCESS_LOST -> R.string.result_access_lost
            RunOutcome.ERROR -> R.string.result_error
        }

@get:StringRes
val UpdateStep.label: Int
    get() =
        when (this) {
            UpdateStep.QUERY -> R.string.update_step_query
            UpdateStep.DOWNLOAD -> R.string.update_step_download
            UpdateStep.VERIFY_SHA256 -> R.string.update_step_sha256
            UpdateStep.VERIFY_CERT -> R.string.update_step_cert
            UpdateStep.INSTALL_PERMISSION -> R.string.update_step_permission
            UpdateStep.INSTALL -> R.string.update_step_install
        }

@get:StringRes
val UpdateOutcome.label: Int
    get() =
        when (this) {
            UpdateOutcome.OK -> R.string.result_ok
            UpdateOutcome.MISMATCH -> R.string.update_outcome_mismatch
            UpdateOutcome.NETWORK_ERROR -> R.string.update_outcome_network
            UpdateOutcome.RATE_LIMITED -> R.string.update_outcome_rate_limited
            UpdateOutcome.ACTIVITY_NOT_FOUND -> R.string.outcome_activity_not_found
            UpdateOutcome.USER_CANCELLED -> R.string.result_cancelled
            UpdateOutcome.FAILED -> R.string.result_failed
        }

@get:StringRes
val UsbOp.label: Int
    get() =
        when (this) {
            UsbOp.OK -> R.string.result_ok
            UsbOp.FAILED -> R.string.result_failed
            UsbOp.READ_ONLY -> R.string.usb_op_read_only
            UsbOp.VOLUME_REMOVED -> R.string.usb_op_removed
            UsbOp.ACTIVITY_NOT_FOUND -> R.string.outcome_activity_not_found
            UsbOp.NOT_RUN -> R.string.usb_op_not_run
        }

@get:StringRes
val ProbeKind.label: Int
    get() =
        when (this) {
            ProbeKind.DIRECT -> R.string.usb_probe_direct
            ProbeKind.DOCUMENT_TREE -> R.string.usb_probe_tree
        }

@get:StringRes
val TestKind.label: Int
    get() =
        when (this) {
            TestKind.ACCESS -> R.string.screen_access
            TestKind.BENCHMARK -> R.string.screen_benchmark
            TestKind.UPDATE -> R.string.screen_update
            TestKind.USB -> R.string.screen_usb
        }

@get:StringRes
val MatrixId.label: Int
    get() =
        when (this) {
            MatrixId.D1 -> R.string.matrix_d1
            MatrixId.D2 -> R.string.matrix_d2
            MatrixId.D3 -> R.string.matrix_d3
            MatrixId.D4 -> R.string.matrix_d4
            MatrixId.D5 -> R.string.matrix_d5
            MatrixId.OTHER -> R.string.matrix_other
        }

/** One-line description of a log entry, shared by the mobile and TV results screens. */
@Composable
fun entrySummary(entry: TestResult): String {
    val format = rememberFormat()
    return when (val payload = entry.payload) {
        is Payload.Access -> stringResource(payload.finalLevel.label)

        is Payload.Benchmark ->
            stringResource(
                R.string.results_entry_benchmark,
                stringResource(payload.run.strategy.label),
                stringResource(payload.run.outcome.label),
                format.seconds(payload.run.durationMs),
                format.count(payload.run.counts.files)
            )

        is Payload.Update ->
            stringResource(
                R.string.results_entry_pair,
                stringResource(payload.record.step.label),
                stringResource(payload.record.outcome.label)
            )

        is Payload.Usb ->
            with(payload.result) {
                stringResource(
                    R.string.results_entry_usb,
                    stringResource(probe.label),
                    stringResource(write.label),
                    stringResource(readVerify.label),
                    stringResource(delete.label)
                )
            }
    }
}
