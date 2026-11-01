package es.sergiosahuquillo.burnfiles.spike.update

import java.net.URL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ReleaseSelectionTest {
    private fun release(tag: String, vararg assets: String) = """
        {"tag_name": "$tag", "prerelease": true, "assets": [
          ${assets.joinToString(",") {
        """{"name": "$it", "browser_download_url": "https://github.com/x/$it", "size": 10}"""
    }}
        ]}
    """.trimIndent()

    private fun complete(version: String) =
        release("spike-v$version", "burnfiles-spike-$version.apk", "burnfiles-spike-$version.apk.sha256")

    @Test
    fun `parses the GitHub releases response`() {
        val parsed = ReleaseSelector.parse("[${complete("0.0.2")}]").single()
        assertEquals("spike-v0.0.2", parsed.tag)
        assertEquals(true, parsed.prerelease)
        assertEquals(
            listOf("burnfiles-spike-0.0.2.apk", "burnfiles-spike-0.0.2.apk.sha256"),
            parsed.assets.map {
                it.name
            }
        )
    }

    @ParameterizedTest
    @CsvSource("0.0.1,1", "0.0.2,2", "1.2.3,10203", "2.10.0,21000")
    fun `version code follows the constitution formula`(name: String, code: Int) {
        assertEquals(code, ReleaseSelector.versionCode(name))
    }

    @ParameterizedTest
    @CsvSource("1.2", "v1.2.3", "1.100.0", "1.0.100", "1.2.3-beta")
    fun `invalid versions are rejected`(name: String) {
        assertNull(ReleaseSelector.versionCode(name))
    }

    @Test
    fun `selects the highest spike release that has both assets`() {
        val json =
            "[" +
                listOf(
                    complete("0.0.2"),
                    complete("0.0.10"),
                    release("spike-v0.0.11", "burnfiles-spike-0.0.11.apk"),
                    release("v1.0.0", "burnfiles-1.0.0.apk"),
                    complete("0.0.3")
                ).joinToString(",") + "]"
        val selected = ReleaseSelector.select(ReleaseSelector.parse(json))
        assertEquals("0.0.10", selected?.versionName)
        assertEquals(10, selected?.versionCode)
        assertEquals("burnfiles-spike-0.0.10.apk.sha256", selected?.checksum?.name)
    }

    @Test
    fun `no spike release means nothing to install`() {
        assertNull(ReleaseSelector.select(ReleaseSelector.parse("[${release("v1.0.0")}]")))
    }

    @Test
    fun `only GitHub hosts over https are allowed`() {
        assertEquals(true, GitHubHosts.isAllowed(URL("https://github.com/a/b")))
        assertEquals(true, GitHubHosts.isAllowed(URL("https://release-assets.githubusercontent.com/x")))
        assertEquals(false, GitHubHosts.isAllowed(URL("http://github.com/a/b")))
        assertEquals(false, GitHubHosts.isAllowed(URL("https://github.com.evil.example/a")))
    }
}
