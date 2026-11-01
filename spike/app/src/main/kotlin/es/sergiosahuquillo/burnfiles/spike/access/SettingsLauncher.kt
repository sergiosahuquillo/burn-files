package es.sergiosahuquillo.burnfiles.spike.access

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings

data class LaunchResult(val resolvable: Boolean, val outcome: AttemptOutcome)

/**
 * Starts a system screen without ever crashing when it does not exist (constitution IV, FR-003).
 * `resolveActivity` is recorded as data only: some ROMs resolve the activity and still fail.
 */
@SuppressLint("QueryPermissionsNeeded")
fun Activity.launchSafely(intent: Intent): LaunchResult {
    val resolvable = intent.resolveActivity(packageManager) != null
    val outcome =
        try {
            startActivity(intent)
            AttemptOutcome.OPENED
        } catch (_: ActivityNotFoundException) {
            AttemptOutcome.ACTIVITY_NOT_FOUND
        } catch (_: SecurityException) {
            AttemptOutcome.SECURITY_EXCEPTION
        }
    return LaunchResult(resolvable, outcome)
}

object SettingsIntents {
    // Literal actions so the class also loads on API < 30, where these steps are never used.
    private const val ACTION_APP_ALL_FILES = "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION"
    private const val ACTION_ALL_FILES = "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"

    fun forStep(step: AccessStep, packageName: String): Intent = when (step) {
        AccessStep.APP_SETTINGS -> Intent(ACTION_APP_ALL_FILES, Uri.parse("package:$packageName"))
        AccessStep.GENERIC_SETTINGS -> Intent(ACTION_ALL_FILES)
        else -> error("$step has no settings screen")
    }

    fun unknownSources(packageName: String): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))

    fun adbGrantCommand(packageName: String) = "adb shell appops set --uid $packageName MANAGE_EXTERNAL_STORAGE allow"
}
