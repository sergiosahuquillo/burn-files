package es.sergiosahuquillo.burnfiles.spike.usb

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sergiosahuquillo.burnfiles.spike.AppContainer
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UsbUiState(
    val loading: Boolean = true,
    val volumes: List<UsbVolume> = emptyList(),
    val selected: Int? = null,
    val running: Boolean = false,
    val direct: UsbProbeResult? = null,
    val tree: UsbProbeResult? = null,
    /** The screen must open the system folder picker for the selected volume. */
    val requestTree: Boolean = false
) {
    val selectedVolume: UsbVolume? get() = volumes.firstOrNull { it.index == selected }
}

class UsbViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UsbUiState())
    val state: StateFlow<UsbUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val volumes = container.volumes.list()
            _state.update { state ->
                state.copy(
                    loading = false,
                    volumes = volumes,
                    selected =
                        state.selected?.takeIf { index -> volumes.any { it.index == index } }
                            ?: volumes.firstOrNull()?.index
                )
            }
        }
    }

    fun select(index: Int) = _state.update { it.copy(selected = index, direct = null, tree = null) }

    /** Runs the DIRECT probe, then asks the screen to open the picker for DOCUMENT_TREE. */
    fun run() {
        val volume = _state.value.selectedVolume ?: return
        if (_state.value.running) return
        viewModelScope.launch {
            _state.update { it.copy(running = true, direct = null, tree = null) }
            val direct =
                withContext(Dispatchers.IO) {
                    container.directProbe.run(volume.index, volume.root, volume.capacityBytes, volume.freeBytes)
                }
            container.recorder.record(Payload.Usb(direct))
            _state.update { it.copy(direct = direct, requestTree = true) }
        }
    }

    fun onTreeRequested() = _state.update { it.copy(requestTree = false) }

    /** [uri] is `null` when the developer closed the picker without choosing. */
    fun onTreePicked(uri: Uri?) {
        val volume = _state.value.selectedVolume ?: return finishTree(null)
        viewModelScope.launch {
            val result =
                if (uri == null) {
                    UsbProbeResult(volume.index, ProbeKind.DOCUMENT_TREE, volume.capacityBytes, volume.freeBytes)
                } else {
                    container.documentTreeProbe.run(volume, uri)
                }
            finishTree(result)
        }
    }

    fun onTreeUnavailable() {
        val volume = _state.value.selectedVolume ?: return finishTree(null)
        finishTree(
            UsbProbeResult(
                volume.index,
                ProbeKind.DOCUMENT_TREE,
                volume.capacityBytes,
                volume.freeBytes,
                detect = UsbOp.ACTIVITY_NOT_FOUND
            )
        )
    }

    private fun finishTree(result: UsbProbeResult?) {
        viewModelScope.launch {
            result?.let { container.recorder.record(Payload.Usb(it)) }
            _state.update { it.copy(running = false, tree = result) }
        }
    }
}
