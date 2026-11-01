package es.sergiosahuquillo.burnfiles.spike.usb

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import es.sergiosahuquillo.burnfiles.spike.update.Sha256Verifier
import java.io.IOException
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Probe `DOCUMENT_TREE` (research R8): same sequence through the system folder picker grant. */
class DocumentTreeProbe(private val context: Context) {
    suspend fun run(volume: UsbVolume, treeUri: Uri): UsbProbeResult = withContext(Dispatchers.IO) {
        var result = UsbProbeResult(volume.index, ProbeKind.DOCUMENT_TREE, volume.capacityBytes, volume.freeBytes)
        val tree = DocumentFile.fromTreeUri(context, treeUri)
        if (tree == null || !tree.exists()) {
            return@withContext result.copy(detect = UsbOp.FAILED, errorKind = ErrorKind.ENOENT)
        }
        result = result.copy(detect = UsbOp.OK)

        val payload = Random.nextBytes(DirectProbe.PROBE_BYTES)
        val name = "BurnFilesSpike-probe-${UUID.randomUUID()}.bin"
        val file =
            UsbErrors
                .attempt {
                    val created = tree.createFile(MIME, name) ?: throw IOException("createFile returned null")
                    context.contentResolver.openOutputStream(created.uri)?.use { it.write(payload) }
                        ?: throw IOException("No output stream")
                    created
                }.getOrElse { error ->
                    return@withContext result.copy(
                        write = UsbErrors.op(error, tree.exists()),
                        errorKind = UsbErrors.kind(error)
                    )
                }
        result = result.copy(write = UsbOp.OK)

        result =
            UsbErrors
                .attempt {
                    val read =
                        context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                            ?: throw IOException("No input stream")
                    Sha256Verifier.digest(read) == Sha256Verifier.digest(payload)
                }.fold(
                    onSuccess = { same -> result.copy(readVerify = if (same) UsbOp.OK else UsbOp.FAILED) },
                    onFailure = { error ->
                        result.copy(
                            readVerify = UsbErrors.op(error, tree.exists()),
                            errorKind = UsbErrors.kind(error)
                        )
                    }
                )

        UsbErrors.attempt { file.delete() }.fold(
            onSuccess = { deleted -> result.copy(delete = if (deleted) UsbOp.OK else UsbOp.FAILED) },
            onFailure = { error ->
                result.copy(
                    delete = UsbErrors.op(error, tree.exists()),
                    errorKind = result.errorKind ?: UsbErrors.kind(error)
                )
            }
        )
    }

    private companion object {
        const val MIME = "application/octet-stream"
    }
}
