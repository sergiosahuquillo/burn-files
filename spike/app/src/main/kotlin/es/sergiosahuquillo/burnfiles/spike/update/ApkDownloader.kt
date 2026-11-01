package es.sergiosahuquillo.burnfiles.spike.update

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class DownloadException(message: String) : IOException(message)

/** Downloads release assets into `cacheDir/updates/`, never leaving partial files behind. */
class ApkDownloader(private val directory: File) {
    suspend fun downloadText(asset: ReleaseAsset): String = withContext(Dispatchers.IO) {
        open(asset.url).let { connection ->
            try {
                connection.inputStream.bufferedReader().use { it.readText().take(MAX_TEXT) }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun download(asset: ReleaseAsset, onProgress: (downloaded: Long, total: Long) -> Unit): File =
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val target = File(directory, asset.name)
            val partial = File(directory, "${asset.name}.part")
            val connection = open(asset.url)
            try {
                val total = connection.contentLengthLong.takeIf { it > 0 } ?: asset.size
                var downloaded = 0L
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
                target.delete()
                if (!partial.renameTo(target)) throw DownloadException("Could not finalize download")
                target
            } catch (error: CancellationException) {
                partial.delete()
                throw error
            } catch (error: IOException) {
                partial.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }

    fun discard(file: File) {
        file.delete()
    }

    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    /** Follows redirects by hand so that every hop stays on a GitHub host. */
    private fun open(start: String): HttpURLConnection {
        var url = URL(start)
        repeat(MAX_REDIRECTS) {
            if (!GitHubHosts.isAllowed(url)) throw DownloadException("Host not allowed")
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = GitHubReleasesClient.TIMEOUT_MS
            connection.readTimeout = GitHubReleasesClient.TIMEOUT_MS
            when (val code = connection.responseCode) {
                in REDIRECTS -> {
                    val location = connection.getHeaderField("Location") ?: throw DownloadException("Bad redirect")
                    connection.disconnect()
                    url = URL(url, location)
                }

                HttpURLConnection.HTTP_OK -> return connection

                else -> {
                    connection.disconnect()
                    throw DownloadException("HTTP $code")
                }
            }
        }
        throw DownloadException("Too many redirects")
    }

    private companion object {
        const val BUFFER = 64 * 1024
        const val MAX_REDIRECTS = 5
        const val MAX_TEXT = 4 * 1024
        val REDIRECTS = setOf(301, 302, 303, 307, 308)
    }
}
