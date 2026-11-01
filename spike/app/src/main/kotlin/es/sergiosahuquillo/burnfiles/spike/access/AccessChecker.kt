package es.sergiosahuquillo.burnfiles.spike.access

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AccessChecker(private val context: Context) {
    fun current(): AccessLevel {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) return AccessLevel.FULL
            return if (hasMediaRead()) AccessLevel.MEDIA_ONLY else AccessLevel.NONE
        }
        val read = granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        val write = granted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return when {
            read && write -> AccessLevel.LEGACY_FULL
            read -> AccessLevel.MEDIA_ONLY
            else -> AccessLevel.NONE
        }
    }

    private fun hasMediaRead(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        mediaPermissions().any(::granted)
    } else {
        granted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Runtime permissions requested by the dialog-based steps of the chain. */
        fun runtimePermissionsFor(step: AccessStep): List<String> = when (step) {
            AccessStep.RUNTIME_DIALOG ->
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            AccessStep.MEDIA_ONLY ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mediaPermissions()
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            else -> emptyList()
        }

        private fun mediaPermissions() = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    }
}

/**
 * Re-evaluates the access level every time the app comes to the foreground (constitution IV).
 * [resumes] emits on every evaluation, even when the level did not change, so the chain can tell
 * "came back from settings without effect" apart from "nothing happened".
 */
class AccessMonitor(private val checker: AccessChecker) {
    private val _level = MutableStateFlow(AccessLevel.NONE)
    val level: StateFlow<AccessLevel> = _level.asStateFlow()

    private val _resumes = MutableSharedFlow<AccessLevel>(extraBufferCapacity = 1)
    val resumes: SharedFlow<AccessLevel> = _resumes.asSharedFlow()

    suspend fun refresh(): AccessLevel = withContext(Dispatchers.Default) {
        checker.current().also {
            _level.value = it
            _resumes.tryEmit(it)
        }
    }

    /** Evaluates without notifying [resumes], for checks that are not a return to the foreground. */
    suspend fun peek(): AccessLevel = withContext(Dispatchers.Default) {
        checker.current().also { _level.value = it }
    }
}
