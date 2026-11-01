package es.sergiosahuquillo.burnfiles.spike.log

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Append-only JSON Lines log at `filesDir/results/results.jsonl` (research R9).
 * Works without any storage permission, also in limited mode.
 */
class ResultsLog(filesDir: File) {
    val file: File = File(filesDir, "results/$FILE_NAME")

    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<TestResult>>(emptyList())
    val entries: StateFlow<List<TestResult>> = _entries.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock { _entries.value = readAll() }
    }

    suspend fun append(result: TestResult) = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.parentFile?.mkdirs()
            file.appendText(ResultsJson.encode(result) + "\n")
            _entries.value = _entries.value + result
        }
    }

    private fun readAll(): List<TestResult> {
        if (!file.exists()) return emptyList()
        // A line written by an older build may not parse; skip it rather than lose the whole log.
        return file.readLines().filter { it.isNotBlank() }.mapNotNull { line ->
            runCatching { ResultsJson.decode(line) }.getOrNull()
        }
    }

    companion object {
        const val FILE_NAME = "results.jsonl"
    }
}
