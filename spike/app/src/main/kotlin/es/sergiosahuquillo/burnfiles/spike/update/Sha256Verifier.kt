package es.sergiosahuquillo.burnfiles.spike.update

import java.io.File
import java.security.MessageDigest

object Sha256Verifier {
    private val HEX64 = Regex("^[0-9a-fA-F]{64}$")
    private const val BUFFER = 64 * 1024

    /**
     * Parses a `sha256sum` line (`<hex>  <name>` or `<hex> *<name>`). When the line names a file it
     * must be [expectedName]. Returns the lowercase digest or `null` if the content is invalid.
     */
    fun parseChecksum(text: String, expectedName: String): String? {
        val line = text.lineSequence().map(String::trim).firstOrNull { it.isNotEmpty() } ?: return null
        val parts = line.split(Regex("\\s+"), limit = 2)
        val hex = parts[0]
        if (!HEX64.matches(hex)) return null
        val name = parts.getOrNull(1)?.removePrefix("*")
        if (name != null && name != expectedName) return null
        return hex.lowercase()
    }

    fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun digest(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    fun matches(file: File, expectedHex: String): Boolean = digest(file).equals(expectedHex, ignoreCase = true)

    fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and BYTE_MASK
            append(HEX_DIGITS[value ushr NIBBLE])
            append(HEX_DIGITS[value and NIBBLE_MASK])
        }
    }

    private const val HEX_DIGITS = "0123456789abcdef"
    private const val BYTE_MASK = 0xFF
    private const val NIBBLE = 4
    private const val NIBBLE_MASK = 0x0F
}
