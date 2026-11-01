package es.sergiosahuquillo.burnfiles.spike.benchmark

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Walks a tree with one of the file-based strategies of research R5, splitting the work by
 * first-level folder over `Dispatchers.IO.limitedParallelism(n)`.
 *
 * Directories in [excluded] (`Android/data`, `Android/obb`) are never entered (FR-011).
 */
class FileTraversal(private val strategy: Strategy, excluded: Collection<File>) {
    init {
        require(strategy != Strategy.MEDIASTORE) { "Use MediaStoreTraversal" }
    }

    private val excludedPaths: Set<Path> = excluded.map { it.toPath().toAbsolutePath().normalize() }.toSet()

    private val files = AtomicLong()
    private val folders = AtomicLong()
    private val bytes = AtomicLong()

    private fun snapshot() = Counts(files.get(), folders.get(), bytes.get())

    suspend fun run(
        root: File,
        progressIntervalMs: Long = PROGRESS_INTERVAL_MS,
        onProgress: (Counts) -> Unit = {}
    ): Counts = withContext(Dispatchers.IO) {
        files.set(0)
        folders.set(0)
        bytes.set(0)
        coroutineScope {
            val reporter =
                launch {
                    while (isActive) {
                        onProgress(snapshot())
                        delay(progressIntervalMs)
                    }
                }
            val dispatcher = Dispatchers.IO.limitedParallelism(strategy.parallelism)
            val topLevel = root.listFiles().orEmpty()
            val subtrees =
                topLevel.mapNotNull { entry ->
                    when {
                        !entry.isDirectory -> {
                            files.incrementAndGet()
                            bytes.addAndGet(entry.length())
                            null
                        }

                        isExcluded(entry.toPath()) -> null

                        else -> entry
                    }
                }
            subtrees
                .map { dir ->
                    async(dispatcher) {
                        folders.incrementAndGet()
                        when (strategy) {
                            Strategy.NIO_P1, Strategy.NIO_P4 -> walkNio(dir)
                            else -> walkIo(dir)
                        }
                    }
                }.awaitAll()
            reporter.cancel()
        }
        snapshot().also(onProgress)
    }

    private fun isExcluded(path: Path) = path.toAbsolutePath().normalize() in excludedPaths

    private suspend fun walkNio(dir: File) {
        val job = coroutineContext[Job]
        val start = dir.toPath()
        Files.walkFileTree(
            start,
            emptySet(),
            Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (job?.isActive == false) throw CancellationException("Traversal cancelled")
                    if (path == start) return FileVisitResult.CONTINUE
                    if (isExcluded(path)) return FileVisitResult.SKIP_SUBTREE
                    folders.incrementAndGet()
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    files.incrementAndGet()
                    bytes.addAndGet(attrs.size())
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(path: Path, exc: IOException) = FileVisitResult.CONTINUE
            }
        )
    }

    private suspend fun walkIo(dir: File) {
        val context = coroutineContext
        val pending = ArrayDeque<File>()
        pending.addLast(dir)
        while (pending.isNotEmpty()) {
            context.ensureActive()
            pending.removeLast().listFiles()?.forEach { child -> visitIo(child, pending) }
        }
    }

    private fun visitIo(child: File, pending: ArrayDeque<File>) {
        when {
            !child.isDirectory -> {
                files.incrementAndGet()
                bytes.addAndGet(child.length())
            }

            !isExcluded(child.toPath()) -> {
                folders.incrementAndGet()
                pending.addLast(child)
            }
        }
    }

    companion object {
        const val PROGRESS_INTERVAL_MS = 250L
    }
}
