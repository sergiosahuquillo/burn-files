package es.sergiosahuquillo.burnfiles.spike.log

import es.sergiosahuquillo.burnfiles.spike.benchmark.RunOutcome
import es.sergiosahuquillo.burnfiles.spike.update.UpdateOutcome
import es.sergiosahuquillo.burnfiles.spike.usb.UsbOp

data class SummaryRow(val test: TestKind, val total: Int, val ok: Int, val last: TestResult?)

/** Human-readable overview of the log: one row per test, in a fixed order. */
object ResultsSummary {
    fun summarize(entries: List<TestResult>): List<SummaryRow> {
        val byTest = entries.groupBy { it.test }
        return TestKind.entries.map { test ->
            val group = byTest[test].orEmpty()
            SummaryRow(test, group.size, group.count(::isOk), group.maxByOrNull { it.timestamp })
        }
    }

    fun isOk(result: TestResult): Boolean = when (val payload = result.payload) {
        is Payload.Access -> payload.finalLevel.isFull

        is Payload.Benchmark -> payload.run.outcome == RunOutcome.OK

        is Payload.Update -> payload.record.outcome == UpdateOutcome.OK

        is Payload.Usb ->
            with(payload.result) { listOf(detect, write, readVerify, delete).all { it == UsbOp.OK } }
    }
}
