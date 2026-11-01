package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/**
 * Permanently deletes the synthetic dataset. Justified exception to constitution II (plan,
 * *Complexity Tracking*): it only ever deletes inside `BurnFilesSpike/dataset/` and only when the
 * spike's own marker is present.
 */
class DatasetCleaner(private val datasetRoot: File) {
    suspend fun clean(onProgress: (deleted: Int) -> Unit) = withContext(Dispatchers.IO) {
        check(datasetRoot.name == DATASET_DIR && datasetRoot.parentFile?.name == SPIKE_DIR) {
            "Refusing to delete outside the spike folder"
        }
        val marker = checkNotNull(DatasetMarker.read(datasetRoot)) { "No dataset marker" }
        marker.copy(state = DatasetState.CLEANING).write(datasetRoot)

        val job = coroutineContext[Job]
        val rootPath = datasetRoot.toPath()
        val markerPath = rootPath.resolve(DatasetMarker.FILE_NAME)
        var deleted = 0
        Files.walkFileTree(
            rootPath,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (job?.isActive == false) return FileVisitResult.TERMINATE
                    if (file != markerPath) {
                        Files.deleteIfExists(file)
                        deleted++
                        if (deleted % PROGRESS_EVERY == 0) onProgress(deleted)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (dir != rootPath) Files.deleteIfExists(dir)
                    return FileVisitResult.CONTINUE
                }
            }
        )
        if (job?.isActive != false) {
            Files.deleteIfExists(markerPath)
            Files.deleteIfExists(rootPath)
        }
        onProgress(deleted)
    }

    companion object {
        const val SPIKE_DIR = "BurnFilesSpike"
        const val DATASET_DIR = "dataset"
        private const val PROGRESS_EVERY = 500
    }
}
