package es.sergiosahuquillo.burnfiles.spike.usb

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A mounted removable volume. [description] is shown on screen but never logged. */
data class UsbVolume(
    val index: Int,
    val description: String,
    val root: File?,
    val capacityBytes: Long,
    val freeBytes: Long,
    val readOnly: Boolean,
    val storageVolume: StorageVolume
)

class VolumeLister(private val context: Context) {
    suspend fun list(): List<UsbVolume> = withContext(Dispatchers.IO) {
        val storage = context.getSystemService(StorageManager::class.java) ?: return@withContext emptyList()
        storage.storageVolumes
            .filter { it.isRemovable && it.state in MOUNTED_STATES }
            .mapIndexed { index, volume ->
                val root = rootOf(volume)
                val stat = root?.let { runCatching { StatFs(it.absolutePath) }.getOrNull() }
                UsbVolume(
                    index = index,
                    description = volume.getDescription(context),
                    root = root,
                    capacityBytes = stat?.totalBytes ?: 0,
                    freeBytes = stat?.availableBytes ?: 0,
                    readOnly = volume.state == Environment.MEDIA_MOUNTED_READ_ONLY,
                    storageVolume = volume
                )
            }
    }

    /**
     * API ≥ 30 exposes the mount point. Before that, removable volumes are mounted at
     * `/storage/<uuid>` on AOSP and Fire OS; the app-specific folder on the volume is not used
     * (constitution III).
     */
    private fun rootOf(volume: StorageVolume): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return volume.directory
        val uuid = volume.uuid ?: return null
        return File("/storage/$uuid").takeIf { it.isDirectory }
    }

    private companion object {
        val MOUNTED_STATES = setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }
}
