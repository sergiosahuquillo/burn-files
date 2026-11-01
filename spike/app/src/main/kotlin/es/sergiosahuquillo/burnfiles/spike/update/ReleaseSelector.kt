package es.sergiosahuquillo.burnfiles.spike.update

import org.json.JSONArray

data class ReleaseAsset(val name: String, val url: String, val size: Long)

data class ReleaseInfo(val tag: String, val prerelease: Boolean, val assets: List<ReleaseAsset>)

data class SpikeRelease(
    val versionName: String,
    val versionCode: Int,
    val apk: ReleaseAsset,
    val checksum: ReleaseAsset
)

/** Picks the spike test build to install (contracts/spike-release.md). */
object ReleaseSelector {
    const val TAG_PREFIX = "spike-v"
    private const val MAJOR_FACTOR = 10_000
    private const val MINOR_FACTOR = 100
    private val SEMVER = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

    fun parse(json: String): List<ReleaseInfo> {
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val release = array.getJSONObject(index)
            val assets = release.optJSONArray("assets") ?: JSONArray()
            ReleaseInfo(
                tag = release.getString("tag_name"),
                prerelease = release.optBoolean("prerelease"),
                assets =
                    (0 until assets.length()).map { assetIndex ->
                        val asset = assets.getJSONObject(assetIndex)
                        ReleaseAsset(
                            name = asset.getString("name"),
                            url = asset.getString("browser_download_url"),
                            size = asset.optLong("size")
                        )
                    }
            )
        }
    }

    /** `major * 10000 + minor * 100 + patch`, as in the constitution; `null` if not X.Y.Z. */
    fun versionCode(versionName: String): Int? {
        val match = SEMVER.matchEntire(versionName) ?: return null
        val (major, minor, patch) = match.destructured.toList().map(String::toInt)
        if (minor >= MINOR_FACTOR || patch >= MINOR_FACTOR) return null
        return major * MAJOR_FACTOR + minor * MINOR_FACTOR + patch
    }

    fun apkName(versionName: String) = "burnfiles-spike-$versionName.apk"

    fun checksumName(versionName: String) = "${apkName(versionName)}.sha256"

    fun select(releases: List<ReleaseInfo>): SpikeRelease? = releases
        .mapNotNull { release ->
            if (!release.tag.startsWith(TAG_PREFIX)) return@mapNotNull null
            val versionName = release.tag.removePrefix(TAG_PREFIX)
            val code = versionCode(versionName) ?: return@mapNotNull null
            val apk = release.assets.firstOrNull { it.name == apkName(versionName) }
            val checksum = release.assets.firstOrNull { it.name == checksumName(versionName) }
            if (apk == null || checksum == null) null else SpikeRelease(versionName, code, apk, checksum)
        }.maxByOrNull { it.versionCode }
}
