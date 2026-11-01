package es.sergiosahuquillo.burnfiles.spike.log

import es.sergiosahuquillo.burnfiles.spike.device.DeviceDetector
import java.time.Instant
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

fun interface ResultSink {
    suspend fun record(payload: Payload)
}

/** Stamps payloads with device, version and time (FR-018) and appends them to the log. */
class Recorder(
    private val device: DeviceDetector,
    private val log: ResultsLog,
    private val appVersionCode: Int,
    private val clock: () -> Instant = Instant::now
) : ResultSink {
    // Results of cancelled operations must still be written.
    override suspend fun record(payload: Payload) = withContext(NonCancellable) {
        log.append(TestResult(clock(), appVersionCode, device.detect(), payload))
    }
}
