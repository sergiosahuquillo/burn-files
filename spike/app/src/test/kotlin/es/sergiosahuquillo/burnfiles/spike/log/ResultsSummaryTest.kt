package es.sergiosahuquillo.burnfiles.spike.log

import es.sergiosahuquillo.burnfiles.spike.benchmark.RunOutcome
import es.sergiosahuquillo.burnfiles.spike.usb.UsbOp
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultsSummaryTest {
    @Test
    fun `one row per test in a fixed order, also without entries`() {
        val rows = ResultsSummary.summarize(emptyList())
        assertEquals(TestKind.entries, rows.map { it.test })
        rows.forEach {
            assertEquals(0, it.total)
            assertNull(it.last)
        }
    }

    @Test
    fun `counts totals, successes and the latest entry`() {
        val failed = Fixtures.benchmark.copy(run = Fixtures.benchmark.run.copy(outcome = RunOutcome.CANCELLED))
        val early = Fixtures.result(Fixtures.benchmark, at = Instant.parse("2026-11-01T10:00:00Z"))
        val late = Fixtures.result(failed, at = Instant.parse("2026-11-01T11:00:00Z"))
        val rows = ResultsSummary.summarize(listOf(late, early, Fixtures.result(Fixtures.access)))

        val benchmark = rows.single { it.test == TestKind.BENCHMARK }
        assertEquals(2, benchmark.total)
        assertEquals(1, benchmark.ok)
        assertEquals(late, benchmark.last)
        assertEquals(1, rows.single { it.test == TestKind.ACCESS }.total)
    }

    @Test
    fun `success rules per test`() {
        assertFalse(ResultsSummary.isOk(Fixtures.result(Fixtures.access)), "media only is not full access")
        assertTrue(ResultsSummary.isOk(Fixtures.result(Fixtures.benchmark)))
        assertFalse(ResultsSummary.isOk(Fixtures.result(Fixtures.update)))
        assertFalse(ResultsSummary.isOk(Fixtures.result(Fixtures.usb)))
        val allOk =
            Fixtures.usb.copy(
                result = Fixtures.usb.result.copy(
                    write = UsbOp.OK,
                    readVerify = UsbOp.OK,
                    delete = UsbOp.OK,
                    errorKind = null
                )
            )
        assertTrue(ResultsSummary.isOk(Fixtures.result(allOk)))
    }
}
