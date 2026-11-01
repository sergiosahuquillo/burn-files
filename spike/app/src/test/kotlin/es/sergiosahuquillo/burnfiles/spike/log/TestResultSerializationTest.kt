package es.sergiosahuquillo.burnfiles.spike.log

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TestResultSerializationTest {
    @ParameterizedTest
    @MethodSource("payloads")
    fun `every payload survives a round trip`(payload: Payload) {
        val original = Fixtures.result(payload)
        assertEquals(original, ResultsJson.decode(ResultsJson.encode(original)))
    }

    @ParameterizedTest
    @MethodSource("payloads")
    fun `top level matches the schema`(payload: Payload) {
        val json = JSONObject(ResultsJson.encode(Fixtures.result(payload)))
        assertEquals(
            setOf("schemaVersion", "timestamp", "appVersionCode", "device", "test", "payload"),
            json.keys().asSequence().toSet()
        )
        assertEquals(1, json.getInt("schemaVersion"))
        assertEquals(payload.test.name, json.getString("test"))
        assertEquals("2026-11-01T10:15:30.123Z", json.getString("timestamp"))
        assertEquals(
            setOf("matrixId", "manufacturer", "model", "sdkInt", "release", "fireOs", "uiMode"),
            json.getJSONObject("device").keys().asSequence().toSet()
        )
    }

    @Test
    fun `absent optional values are written as null, not omitted`() {
        val json = JSONObject(ResultsJson.encode(Fixtures.result(Fixtures.benchmark, Fixtures.phone)))
        assertTrue(json.getJSONObject("device").has("fireOs"))
        assertTrue(json.getJSONObject("device").isNull("fireOs"))
    }

    @Test
    fun `benchmark payload carries derived files per second`() {
        val payload = JSONObject(ResultsJson.encode(Fixtures.result(Fixtures.benchmark))).getJSONObject("payload")
        assertEquals(100_000 * 1000.0 / 8_421, payload.getDouble("filesPerSecond"), 0.001)
    }

    @Test
    fun `one entry is exactly one line`() {
        Fixtures.payloads.forEach { assertFalse(ResultsJson.encode(Fixtures.result(it)).contains('\n')) }
    }

    companion object {
        @JvmStatic
        fun payloads() = Fixtures.payloads
    }
}
