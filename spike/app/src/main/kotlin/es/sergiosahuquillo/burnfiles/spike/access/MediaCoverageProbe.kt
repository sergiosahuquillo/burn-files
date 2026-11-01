package es.sergiosahuquillo.burnfiles.spike.access

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Counts what limited mode can see, to quantify its coverage (FR-005). */
class MediaCoverageProbe(private val resolver: ContentResolver) {
    suspend fun measure(): MediaCoverage = withContext(Dispatchers.IO) {
        MediaCoverage(
            images = count(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            video = count(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audio = count(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            downloads = downloads()
        )
    }

    private fun downloads(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        count(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
    } else {
        @Suppress("DEPRECATION")
        count(
            MediaStore.Files.getContentUri("external"),
            "${MediaStore.Files.FileColumns.DATA} LIKE ?",
            arrayOf("%/Download/%")
        )
    }

    private fun count(uri: Uri, selection: String? = null, args: Array<String>? = null): Int = runCatching {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), selection, args, null)?.use { it.count } ?: 0
    }.getOrDefault(0)
}
