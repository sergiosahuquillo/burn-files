package es.sergiosahuquillo.burnfiles.spike.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only network partner of the spike (constitution VI, FR-015). */
object GitHubHosts {
    private val ALLOWED =
        setOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )

    fun isAllowed(url: URL) = url.protocol == "https" && url.host in ALLOWED
}

sealed interface ReleasesResult {
    data class Ok(val releases: List<ReleaseInfo>) : ReleasesResult

    data class RateLimited(val resetEpochSeconds: Long?) : ReleasesResult

    data class NetworkError(val httpCode: Int?) : ReleasesResult
}

class GitHubReleasesClient(private val repository: String = "sergiosahuquillo/burn-files") {
    private var etag: String? = null
    private var cached: List<ReleaseInfo>? = null

    suspend fun releases(): ReleasesResult = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/$repository/releases?per_page=20")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            etag?.let { connection.setRequestProperty("If-None-Match", it) }
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_NOT_MODIFIED && cached != null -> ReleasesResult.Ok(cached!!)

                code == HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val releases = ReleaseSelector.parse(body)
                    etag = connection.getHeaderField("ETag")
                    cached = releases
                    ReleasesResult.Ok(releases)
                }

                (code == HTTP_FORBIDDEN || code == HTTP_TOO_MANY) &&
                    connection.getHeaderField("X-RateLimit-Remaining") == "0" ->
                    ReleasesResult.RateLimited(connection.getHeaderField("X-RateLimit-Reset")?.toLongOrNull())

                else -> ReleasesResult.NetworkError(code)
            }
        } catch (_: IOException) {
            ReleasesResult.NetworkError(null)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val TIMEOUT_MS = 15_000
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_TOO_MANY = 429
    }
}
