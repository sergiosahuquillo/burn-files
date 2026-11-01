package es.sergiosahuquillo.burnfiles.spike.benchmark

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Strategy `MEDIASTORE` (research R5): reads the system media index instead of the disk. The only
 * strategy available in limited mode. Folders are not reported by the index, so `folders` is 0.
 */
class MediaStoreTraversal(private val resolver: ContentResolver) {
    /** [scope] limits the query to one folder (the synthetic dataset); `null` = whole volume. */
    suspend fun run(scope: File?, storageRoot: File, onProgress: (Counts) -> Unit = {}): Counts =
        withContext(Dispatchers.IO) {
            val context = coroutineContext
            val (selection, args) = selectionFor(scope, storageRoot)
            val projection =
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )
            var files = 0L
            var bytes = 0L
            resolver
                .query(MediaStore.Files.getContentUri("external"), projection, selection, args, null)
                ?.use { cursor ->
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        files++
                        bytes += cursor.getLong(sizeColumn)
                        // Read the remaining columns too, as the real index would.
                        cursor.getString(mimeColumn)
                        cursor.getLong(dateColumn)
                        if (files % PROGRESS_EVERY == 0L) {
                            context.ensureActive()
                            onProgress(Counts(files, 0, bytes))
                        }
                    }
                }
            Counts(files, 0, bytes).also(onProgress)
        }

    private fun selectionFor(scope: File?, storageRoot: File): Pair<String?, Array<String>?> {
        if (scope == null) return null to null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relative = scope.relativeTo(storageRoot).invariantSeparatorsPath
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" to arrayOf("$relative/%")
        } else {
            @Suppress("DEPRECATION")
            "${MediaStore.MediaColumns.DATA} LIKE ?" to arrayOf("${scope.absolutePath}/%")
        }
    }

    private companion object {
        const val PROGRESS_EVERY = 1_000L
    }
}
