package es.sergiosahuquillo.burnfiles.spike.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Installs a verified APK through a [PackageInstaller] session. The system confirmation is always
 * shown: `USER_ACTION_NOT_REQUIRED` is never requested (FR-014).
 */
class SpikeInstaller(private val context: Context) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun install(apk: File, fromVersionCode: Int, toVersionCode: Int) = withContext(Dispatchers.IO) {
        rememberPending(fromVersionCode, toVersionCode)
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            session.commit(statusReceiver(sessionId).intentSender)
        }
    }

    private fun statusReceiver(sessionId: Int): PendingIntent {
        val intent = Intent(context, InstallResultReceiver::class.java).setAction(InstallResultReceiver.ACTION)
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
    }

    /**
     * Updating itself kills the process, so success is detected on the next start by comparing the
     * running version with the one that was being installed.
     */
    private fun rememberPending(from: Int, to: Int) {
        prefs
            .edit()
            .putInt(KEY_FROM, from)
            .putInt(KEY_TO, to)
            .commit()
    }

    suspend fun takePending(): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        if (!prefs.contains(KEY_TO)) return@withContext null
        val pending = prefs.getInt(KEY_FROM, 0) to prefs.getInt(KEY_TO, 0)
        prefs
            .edit()
            .remove(KEY_FROM)
            .remove(KEY_TO)
            .commit()
        pending
    }

    private companion object {
        const val PREFS = "update"
        const val KEY_FROM = "pending_from"
        const val KEY_TO = "pending_to"
    }
}
