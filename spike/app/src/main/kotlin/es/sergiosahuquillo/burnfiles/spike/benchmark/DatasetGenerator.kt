package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class InsufficientSpaceException(val requiredBytes: Long, val availableBytes: Long) :
    IllegalStateException("Not enough free space")

/** Writes a [DatasetPlan] under the spike's own `dataset/` folder (FR-007, FR-008). */
class DatasetGenerator(private val datasetRoot: File, private val clock: () -> Long = System::nanoTime) {
    /**
     * Creates the missing files of [plan]. Resumes an INCOMPLETE dataset with the same seed and
     * profile; [onProgress] gets the number of files present at most every [PROGRESS_INTERVAL_NS].
     */
    suspend fun generate(plan: DatasetPlan, onProgress: (created: Int) -> Unit): DatasetMarker =
        withContext(Dispatchers.IO) {
            val existing = DatasetMarker.read(datasetRoot)
            if (existing != null && existing.matches(plan) && existing.state == DatasetState.COMPLETE) {
                return@withContext existing
            }
            ensureSpace(plan)

            DatasetMarker.forPlan(plan, existing?.createdFiles ?: 0, DatasetState.INCOMPLETE).write(datasetRoot)
            for (index in 0 until plan.folderCount) {
                File(datasetRoot, plan.folderPath(index)).mkdirs()
            }

            val buffer = ByteArray(plan.profile.largeMaxBytes) { (it % FILLER_PERIOD).toByte() }
            var lastProgress = clock()
            for (index in 0 until plan.fileCount) {
                if (index % CANCEL_CHECK_EVERY == 0) currentCoroutineContext().ensureActive()
                val file = File(datasetRoot, plan.filePath(index))
                val size = plan.fileSize(index)
                if (!file.isFile || file.length() != size.toLong()) {
                    FileOutputStream(file).use { it.write(buffer, 0, size) }
                }
                if (index % MARKER_EVERY == 0) {
                    DatasetMarker.forPlan(plan, index, DatasetState.INCOMPLETE).write(datasetRoot)
                }
                val now = clock()
                if (now - lastProgress >= PROGRESS_INTERVAL_NS) {
                    onProgress(index + 1)
                    lastProgress = now
                }
            }
            onProgress(plan.fileCount)
            DatasetMarker.forPlan(plan, plan.fileCount, DatasetState.COMPLETE).also { it.write(datasetRoot) }
        }

    private fun ensureSpace(plan: DatasetPlan) {
        val parent = datasetRoot.parentFile ?: datasetRoot
        parent.mkdirs()
        val required = (plan.diskBytes() * SPACE_MARGIN).toLong()
        val available = parent.usableSpace
        if (available < required) throw InsufficientSpaceException(required, available)
    }

    companion object {
        private const val SPACE_MARGIN = 1.10
        private const val CANCEL_CHECK_EVERY = 64
        private const val MARKER_EVERY = 2_000
        private const val FILLER_PERIOD = 251
        const val PROGRESS_INTERVAL_NS = 250_000_000L
    }
}
