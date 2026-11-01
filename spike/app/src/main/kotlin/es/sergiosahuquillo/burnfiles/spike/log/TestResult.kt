package es.sergiosahuquillo.burnfiles.spike.log

import es.sergiosahuquillo.burnfiles.spike.access.AccessAttempt
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel
import es.sergiosahuquillo.burnfiles.spike.access.MediaCoverage
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkRun
import es.sergiosahuquillo.burnfiles.spike.device.DeviceInfo
import es.sergiosahuquillo.burnfiles.spike.update.UpdateRecord
import es.sergiosahuquillo.burnfiles.spike.usb.UsbProbeResult
import java.time.Instant

enum class TestKind { ACCESS, BENCHMARK, UPDATE, USB }

/**
 * One line of the results log (contracts/results-log.schema.json).
 *
 * Payloads only hold enums and numbers so that no path, file name or file content can leak into
 * an exported log (constitution VI, FR-019).
 */
data class TestResult(val timestamp: Instant, val appVersionCode: Int, val device: DeviceInfo, val payload: Payload) {
    val test: TestKind get() = payload.test

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

sealed interface Payload {
    val test: TestKind

    data class Access(
        val attempts: List<AccessAttempt>,
        val finalLevel: AccessLevel,
        val mediaCoverage: MediaCoverage?
    ) : Payload {
        override val test get() = TestKind.ACCESS
    }

    data class Benchmark(val run: BenchmarkRun) : Payload {
        override val test get() = TestKind.BENCHMARK
    }

    data class Update(val record: UpdateRecord) : Payload {
        override val test get() = TestKind.UPDATE
    }

    data class Usb(val result: UsbProbeResult) : Payload {
        override val test get() = TestKind.USB
    }
}
