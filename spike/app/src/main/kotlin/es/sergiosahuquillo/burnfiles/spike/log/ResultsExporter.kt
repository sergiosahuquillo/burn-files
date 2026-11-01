package es.sergiosahuquillo.burnfiles.spike.log

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import es.sergiosahuquillo.burnfiles.spike.access.AttemptOutcome
import es.sergiosahuquillo.burnfiles.spike.access.launchSafely
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exports the log (FR-020): share sheet, copy to `Download/BurnFilesSpike/`, and always
 * `adb shell run-as es.sergiosahuquillo.burnfiles.spike cat files/results/results.jsonl`.
 */
class ResultsExporter(private val context: Context, private val log: ResultsLog) {
    fun share(activity: Activity): Boolean {
        if (!log.file.exists()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", log.file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return activity.launchSafely(Intent.createChooser(send, null)).outcome == AttemptOutcome.OPENED
    }

    /** Returns the exported file name, or `null` if it could not be written. */
    suspend fun copyToDownloads(matrixId: MatrixId): String? = withContext(Dispatchers.IO) {
        if (!log.file.exists()) return@withContext null
        val stamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val name = "results-${matrixId.name}-$stamp.jsonl"
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) copyWithMediaStore(name) else copyDirect(name)
            name
        }.getOrNull()
    }

    private fun copyWithMediaStore(name: String) {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        checkNotNull(resolver.openOutputStream(uri)).use { output -> log.file.inputStream().use { it.copyTo(output) } }
    }

    @Suppress("DEPRECATION")
    private fun copyDirect(name: String) {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
        folder.mkdirs()
        log.file.copyTo(File(folder, name), overwrite = true)
    }

    private companion object {
        const val FOLDER = "BurnFilesSpike"
    }
}
