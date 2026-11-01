package es.sergiosahuquillo.burnfiles.spike.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import es.sergiosahuquillo.burnfiles.spike.AppContainer
import es.sergiosahuquillo.burnfiles.spike.access.AccessUiState
import es.sergiosahuquillo.burnfiles.spike.access.AccessViewModel
import es.sergiosahuquillo.burnfiles.spike.usb.UsbUiState
import es.sergiosahuquillo.burnfiles.spike.usb.UsbViewModel

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("No AppContainer") }

/** Activity-scoped ViewModel shared by the mobile and TV layers. */
@Composable
inline fun <reified VM : ViewModel> spikeViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel { create(container) }
}

/** Launches the runtime permission dialog when the access chain asks for it. */
@Composable
fun AccessPermissionEffect(viewModel: AccessViewModel, state: AccessUiState) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onPermissionResult()
        }
    LaunchedEffect(state.permissionRequest) {
        state.permissionRequest?.let { launcher.launch(it.toTypedArray()) }
    }
}

/** Opens the system folder picker for the DOCUMENT_TREE probe, tolerating its absence on TV. */
@Composable
fun UsbTreePickerEffect(viewModel: UsbViewModel, state: UsbUiState) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            viewModel.onTreePicked(uri)
        }
    LaunchedEffect(state.requestTree) {
        if (!state.requestTree) return@LaunchedEffect
        viewModel.onTreeRequested()
        val initial: Uri? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                state.selectedVolume?.storageVolume?.createOpenDocumentTreeIntent()?.let {
                    IntentCompat.getParcelableExtra(it, DocumentsContract.EXTRA_INITIAL_URI, Uri::class.java)
                }
            } else {
                null
            }
        try {
            launcher.launch(initial)
        } catch (_: ActivityNotFoundException) {
            viewModel.onTreeUnavailable()
        }
    }
}

fun Context.copyToClipboard(text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text))
}
