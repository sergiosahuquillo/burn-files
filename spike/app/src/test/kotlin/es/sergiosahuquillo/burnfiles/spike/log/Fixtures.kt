package es.sergiosahuquillo.burnfiles.spike.log

import es.sergiosahuquillo.burnfiles.spike.access.AccessAttempt
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep
import es.sergiosahuquillo.burnfiles.spike.access.AttemptOutcome
import es.sergiosahuquillo.burnfiles.spike.access.MediaCoverage
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkRun
import es.sergiosahuquillo.burnfiles.spike.benchmark.CacheState
import es.sergiosahuquillo.burnfiles.spike.benchmark.Counts
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetKind
import es.sergiosahuquillo.burnfiles.spike.benchmark.RunOutcome
import es.sergiosahuquillo.burnfiles.spike.benchmark.Strategy
import es.sergiosahuquillo.burnfiles.spike.device.DeviceInfo
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.device.UiMode
import es.sergiosahuquillo.burnfiles.spike.update.UpdateOutcome
import es.sergiosahuquillo.burnfiles.spike.update.UpdateRecord
import es.sergiosahuquillo.burnfiles.spike.update.UpdateStep
import es.sergiosahuquillo.burnfiles.spike.usb.ErrorKind
import es.sergiosahuquillo.burnfiles.spike.usb.ProbeKind
import es.sergiosahuquillo.burnfiles.spike.usb.UsbOp
import es.sergiosahuquillo.burnfiles.spike.usb.UsbProbeResult
import java.time.Instant

object Fixtures {
    val fireTv =
        DeviceInfo(
            matrixId = MatrixId.D2,
            manufacturer = "Amazon",
            model = "AFTMM",
            sdkInt = 28,
            release = "9",
            fireOs = "7",
            uiMode = UiMode.TV
        )

    val phone = fireTv.copy(
        matrixId = MatrixId.D1,
        manufacturer = "Google",
        model = "Pixel 8",
        sdkInt = 34,
        release = "14",
        fireOs = null,
        uiMode = UiMode.MOBILE
    )

    val access =
        Payload.Access(
            attempts =
                listOf(
                    AccessAttempt(AccessStep.APP_SETTINGS, false, AttemptOutcome.ACTIVITY_NOT_FOUND, AccessLevel.NONE),
                    AccessAttempt(AccessStep.GENERIC_SETTINGS, null, AttemptOutcome.NO_EFFECT, AccessLevel.NONE),
                    AccessAttempt(AccessStep.ADB, null, AttemptOutcome.SKIPPED, AccessLevel.NONE),
                    AccessAttempt(AccessStep.MEDIA_ONLY, null, AttemptOutcome.GRANTED, AccessLevel.MEDIA_ONLY)
                ),
            finalLevel = AccessLevel.MEDIA_ONLY,
            mediaCoverage = MediaCoverage(images = 12, video = 3, audio = 0, downloads = 7)
        )

    val benchmark =
        Payload.Benchmark(
            BenchmarkRun(
                strategy = Strategy.NIO_P4,
                dataset = DatasetKind.SYNTHETIC,
                repetition = 2,
                cache = CacheState.WARM,
                durationMs = 8_421,
                counts = Counts(files = 100_000, folders = 4_000, bytes = 209_715_200),
                outcome = RunOutcome.OK
            )
        )

    val update =
        Payload.Update(
            UpdateRecord(
                fromVersionCode = 1,
                toVersionCode = 2,
                step = UpdateStep.INSTALL,
                outcome = UpdateOutcome.USER_CANCELLED,
                downloadBytes = 9_000_000,
                durationMs = 1_200,
                installerStatus = 3
            )
        )

    val usb =
        Payload.Usb(
            UsbProbeResult(
                volumeIndex = 0,
                probe = ProbeKind.DIRECT,
                capacityBytes = 32_000_000_000,
                freeBytes = 1_000_000,
                detect = UsbOp.OK,
                write = UsbOp.FAILED,
                errorKind = ErrorKind.EACCES
            )
        )

    val payloads = listOf(access, benchmark, update, usb)

    fun result(payload: Payload, device: DeviceInfo = fireTv, at: Instant = Instant.parse("2026-11-01T10:15:30.123Z")) =
        TestResult(at, appVersionCode = 1, device = device, payload = payload)
}
