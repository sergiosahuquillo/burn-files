package es.sergiosahuquillo.burnfiles.spike.log

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sergiosahuquillo.burnfiles.spike.AppContainer
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExportMessage {
    data class Copied(val fileName: String) : ExportMessage

    data object CopyFailed : ExportMessage

    data object ShareUnavailable : ExportMessage
}

data class ResultsUiState(
    val summary: List<SummaryRow> = emptyList(),
    val entries: List<TestResult> = emptyList(),
    val message: ExportMessage? = null
)

class ResultsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ResultsUiState())
    val state: StateFlow<ResultsUiState> = _state.asStateFlow()

    val adbCommand = "adb shell run-as ${container.app.packageName} cat files/results/${ResultsLog.FILE_NAME}"

    init {
        viewModelScope.launch {
            container.log.entries.collect { entries ->
                _state.update {
                    it.copy(
                        summary = ResultsSummary.summarize(entries),
                        entries = entries.sortedByDescending(TestResult::timestamp)
                    )
                }
            }
        }
    }

    fun share(activity: Activity) {
        if (!container.exporter.share(activity)) _state.update { it.copy(message = ExportMessage.ShareUnavailable) }
    }

    fun copyToDownloads() {
        viewModelScope.launch {
            val matrixId = container.device.matrixId() ?: MatrixId.OTHER
            val name = container.exporter.copyToDownloads(matrixId)
            _state.update { it.copy(message = name?.let(ExportMessage::Copied) ?: ExportMessage.CopyFailed) }
        }
    }
}
