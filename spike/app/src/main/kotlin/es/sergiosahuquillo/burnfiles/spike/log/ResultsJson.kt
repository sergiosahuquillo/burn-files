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
import org.json.JSONArray
import org.json.JSONObject

/** JSON Lines codec for [TestResult], following contracts/results-log.schema.json. */
object ResultsJson {
    fun encode(result: TestResult): String = JSONObject()
        .put("schemaVersion", TestResult.SCHEMA_VERSION)
        .put("timestamp", result.timestamp.toString())
        .put("appVersionCode", result.appVersionCode)
        .put("device", encodeDevice(result.device))
        .put("test", result.test.name)
        .put("payload", encodePayload(result.payload))
        .toString()

    fun decode(line: String): TestResult {
        val json = JSONObject(line)
        require(json.getInt("schemaVersion") == TestResult.SCHEMA_VERSION) { "Unsupported schema" }
        val test = enumOf<TestKind>(json.getString("test"))
        return TestResult(
            timestamp = Instant.parse(json.getString("timestamp")),
            appVersionCode = json.getInt("appVersionCode"),
            device = decodeDevice(json.getJSONObject("device")),
            payload = decodePayload(test, json.getJSONObject("payload"))
        )
    }

    private fun encodeDevice(device: DeviceInfo) = JSONObject()
        .put("matrixId", device.matrixId.name)
        .put("manufacturer", device.manufacturer)
        .put("model", device.model)
        .put("sdkInt", device.sdkInt)
        .put("release", device.release)
        .put("fireOs", device.fireOs ?: JSONObject.NULL)
        .put("uiMode", device.uiMode.name)

    private fun decodeDevice(json: JSONObject) = DeviceInfo(
        matrixId = enumOf<MatrixId>(json.getString("matrixId")),
        manufacturer = json.getString("manufacturer"),
        model = json.getString("model"),
        sdkInt = json.getInt("sdkInt"),
        release = json.getString("release"),
        fireOs = json.optStringOrNull("fireOs"),
        uiMode = enumOf<UiMode>(json.getString("uiMode"))
    )

    private fun encodePayload(payload: Payload): JSONObject = when (payload) {
        is Payload.Access -> encodeAccess(payload)
        is Payload.Benchmark -> encodeBenchmark(payload.run)
        is Payload.Update -> encodeUpdate(payload.record)
        is Payload.Usb -> encodeUsb(payload.result)
    }

    private fun decodePayload(test: TestKind, json: JSONObject): Payload = when (test) {
        TestKind.ACCESS -> decodeAccess(json)
        TestKind.BENCHMARK -> Payload.Benchmark(decodeBenchmark(json))
        TestKind.UPDATE -> Payload.Update(decodeUpdate(json))
        TestKind.USB -> Payload.Usb(decodeUsb(json))
    }

    private fun encodeAccess(payload: Payload.Access): JSONObject {
        val attempts = JSONArray()
        payload.attempts.forEach { attempt ->
            attempts.put(
                JSONObject()
                    .put("step", attempt.step.name)
                    .put("resolvable", attempt.resolvable ?: JSONObject.NULL)
                    .put("outcome", attempt.outcome.name)
                    .put("levelAfter", attempt.levelAfter.name)
            )
        }
        val coverage =
            payload.mediaCoverage?.let {
                JSONObject()
                    .put("images", it.images)
                    .put("video", it.video)
                    .put("audio", it.audio)
                    .put("downloads", it.downloads)
            }
        return JSONObject()
            .put("attempts", attempts)
            .put("finalLevel", payload.finalLevel.name)
            .put("mediaCoverage", coverage ?: JSONObject.NULL)
    }

    private fun decodeAccess(json: JSONObject): Payload.Access {
        val array = json.getJSONArray("attempts")
        val attempts =
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                AccessAttempt(
                    step = enumOf<AccessStep>(item.getString("step")),
                    resolvable = if (item.isNull("resolvable")) null else item.getBoolean("resolvable"),
                    outcome = enumOf<AttemptOutcome>(item.getString("outcome")),
                    levelAfter = enumOf<AccessLevel>(item.getString("levelAfter"))
                )
            }
        val coverage =
            if (json.isNull("mediaCoverage")) {
                null
            } else {
                json.getJSONObject("mediaCoverage").let {
                    MediaCoverage(it.getInt("images"), it.getInt("video"), it.getInt("audio"), it.getInt("downloads"))
                }
            }
        return Payload.Access(attempts, enumOf<AccessLevel>(json.getString("finalLevel")), coverage)
    }

    private fun encodeBenchmark(run: BenchmarkRun) = JSONObject()
        .put("strategy", run.strategy.name)
        .put("dataset", run.dataset.name)
        .put("repetition", run.repetition)
        .put("cache", run.cache.name)
        .put("durationMs", run.durationMs)
        .put("files", run.counts.files)
        .put("folders", run.counts.folders)
        .put("bytes", run.counts.bytes)
        .put("filesPerSecond", run.filesPerSecond)
        .put("outcome", run.outcome.name)

    private fun decodeBenchmark(json: JSONObject) = BenchmarkRun(
        strategy = enumOf<Strategy>(json.getString("strategy")),
        dataset = enumOf<DatasetKind>(json.getString("dataset")),
        repetition = json.getInt("repetition"),
        cache = enumOf<CacheState>(json.getString("cache")),
        durationMs = json.getLong("durationMs"),
        counts = Counts(json.getLong("files"), json.getLong("folders"), json.getLong("bytes")),
        outcome = enumOf<RunOutcome>(json.getString("outcome"))
    )

    private fun encodeUpdate(record: UpdateRecord) = JSONObject()
        .put("fromVersionCode", record.fromVersionCode)
        .put("toVersionCode", record.toVersionCode)
        .put("step", record.step.name)
        .put("outcome", record.outcome.name)
        .put("downloadBytes", record.downloadBytes)
        .put("durationMs", record.durationMs)
        .put("installerStatus", record.installerStatus ?: JSONObject.NULL)

    private fun decodeUpdate(json: JSONObject) = UpdateRecord(
        fromVersionCode = json.getInt("fromVersionCode"),
        toVersionCode = json.getInt("toVersionCode"),
        step = enumOf<UpdateStep>(json.getString("step")),
        outcome = enumOf<UpdateOutcome>(json.getString("outcome")),
        downloadBytes = json.optLong("downloadBytes"),
        durationMs = json.optLong("durationMs"),
        installerStatus = if (json.isNull("installerStatus")) null else json.getInt("installerStatus")
    )

    private fun encodeUsb(result: UsbProbeResult) = JSONObject()
        .put("volumeIndex", result.volumeIndex)
        .put("probe", result.probe.name)
        .put("capacityBytes", result.capacityBytes)
        .put("freeBytes", result.freeBytes)
        .put("detect", result.detect.name)
        .put("write", result.write.name)
        .put("readVerify", result.readVerify.name)
        .put("delete", result.delete.name)
        .put("errorKind", result.errorKind?.name ?: JSONObject.NULL)

    private fun decodeUsb(json: JSONObject) = UsbProbeResult(
        volumeIndex = json.getInt("volumeIndex"),
        probe = enumOf<ProbeKind>(json.getString("probe")),
        capacityBytes = json.optLong("capacityBytes"),
        freeBytes = json.optLong("freeBytes"),
        detect = enumOf<UsbOp>(json.getString("detect")),
        write = enumOf<UsbOp>(json.getString("write")),
        readVerify = enumOf<UsbOp>(json.getString("readVerify")),
        delete = enumOf<UsbOp>(json.getString("delete")),
        errorKind = json.optStringOrNull("errorKind")?.let { enumOf<ErrorKind>(it) }
    )

    private inline fun <reified E : Enum<E>> enumOf(name: String): E = enumValueOf(name)

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
}
