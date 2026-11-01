package es.sergiosahuquillo.burnfiles.spike.usb

import es.sergiosahuquillo.burnfiles.spike.update.Sha256Verifier
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

/** File operations of the probe, swappable in tests to simulate failures. */
interface ProbeIo {
    fun exists(dir: File): Boolean

    fun write(file: File, bytes: ByteArray)

    fun read(file: File): ByteArray

    fun delete(file: File): Boolean
}

object FileProbeIo : ProbeIo {
    override fun exists(dir: File) = dir.isDirectory

    override fun write(file: File, bytes: ByteArray) = file.writeBytes(bytes)

    override fun read(file: File) = file.readBytes()

    override fun delete(file: File) = file.delete()
}

object UsbErrors {
    /** Runs [block], turning only I/O and permission failures into a result; cancellation and bugs propagate. */
    inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: IOException) {
        Result.failure(error)
    } catch (error: SecurityException) {
        Result.failure(error)
    }

    fun kind(error: Throwable): ErrorKind {
        if (error is SecurityException) return ErrorKind.SECURITY
        val message = error.message.orEmpty()
        return when {
            "EROFS" in message || "Read-only" in message -> ErrorKind.EROFS
            "EACCES" in message || "Permission denied" in message -> ErrorKind.EACCES
            "ENOENT" in message || "No such file" in message -> ErrorKind.ENOENT
            else -> ErrorKind.IO
        }
    }

    fun op(error: Throwable, volumePresent: Boolean): UsbOp = when {
        !volumePresent -> UsbOp.VOLUME_REMOVED
        kind(error) == ErrorKind.EROFS -> UsbOp.READ_ONLY
        else -> UsbOp.FAILED
    }
}

/**
 * Probe `DIRECT` (research R8): write, read back and delete one `BurnFilesSpike-probe-<uuid>.bin`
 * at the root of the volume. Only the file it has just created is ever deleted.
 */
class DirectProbe(
    private val io: ProbeIo = FileProbeIo,
    private val random: Random = Random.Default,
    private val name: () -> String = { "BurnFilesSpike-probe-${UUID.randomUUID()}.bin" }
) {
    fun run(volumeIndex: Int, root: File?, capacityBytes: Long, freeBytes: Long): UsbProbeResult {
        var result = UsbProbeResult(volumeIndex, ProbeKind.DIRECT, capacityBytes, freeBytes)
        if (root == null || !io.exists(root)) {
            return result.copy(detect = UsbOp.FAILED, errorKind = ErrorKind.ENOENT)
        }
        result = result.copy(detect = UsbOp.OK)

        val file = File(root, name())
        val payload = random.nextBytes(PROBE_BYTES)
        UsbErrors.attempt { io.write(file, payload) }.exceptionOrNull()?.let { error ->
            UsbErrors.attempt { io.delete(file) }
            return result.copy(write = UsbErrors.op(error, io.exists(root)), errorKind = UsbErrors.kind(error))
        }
        result = result.copy(write = UsbOp.OK)

        result =
            UsbErrors.attempt { Sha256Verifier.digest(io.read(file)) == Sha256Verifier.digest(payload) }.fold(
                onSuccess = { same -> result.copy(readVerify = if (same) UsbOp.OK else UsbOp.FAILED) },
                onFailure = { error ->
                    result.copy(readVerify = UsbErrors.op(error, io.exists(root)), errorKind = UsbErrors.kind(error))
                }
            )

        return UsbErrors.attempt { io.delete(file) }.fold(
            onSuccess = { deleted ->
                val op =
                    when {
                        deleted -> UsbOp.OK
                        !io.exists(root) -> UsbOp.VOLUME_REMOVED
                        else -> UsbOp.FAILED
                    }
                result.copy(delete = op)
            },
            onFailure = { error ->
                result.copy(
                    delete = UsbErrors.op(error, io.exists(root)),
                    errorKind = result.errorKind ?: UsbErrors.kind(error)
                )
            }
        )
    }

    companion object {
        const val PROBE_BYTES = 64 * 1024
    }
}
