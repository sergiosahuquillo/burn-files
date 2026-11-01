package es.sergiosahuquillo.burnfiles.spike.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sergiosahuquillo.burnfiles.spike.AppContainer
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BenchmarkPhase { IDLE, GENERATING, RUNNING, CLEANING }

sealed interface BenchmarkMessage {
    data class InsufficientSpace(val requiredBytes: Long, val availableBytes: Long) : BenchmarkMessage

    data object NeedsFullAccess : BenchmarkMessage

    data object Cancelled : BenchmarkMessage

    data object Failed : BenchmarkMessage

    data object Done : BenchmarkMessage
}

data class BenchmarkUiState(
    val level: AccessLevel = AccessLevel.NONE,
    val marker: DatasetMarker? = null,
    val phase: BenchmarkPhase = BenchmarkPhase.IDLE,
    val dataset: DatasetKind = DatasetKind.SYNTHETIC,
    val processed: Long = 0,
    val target: Long = 0,
    val elapsedMs: Long = 0,
    val currentStrategy: Strategy? = null,
    val currentRepetition: Int = 0,
    val summaries: List<StrategySummary> = emptyList(),
    val message: BenchmarkMessage? = null,
    val confirmClean: Boolean = false,
    val confirmCancel: Boolean = false
) {
    val busy: Boolean get() = phase != BenchmarkPhase.IDLE
    val hasDataset: Boolean get() = marker != null
    val datasetComplete: Boolean get() = marker?.state == DatasetState.COMPLETE
}

class BenchmarkViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(BenchmarkUiState())
    val state: StateFlow<BenchmarkUiState> = _state.asStateFlow()
    private var job: Job? = null
    private val plan by lazy { DatasetPlan.create() }

    init {
        viewModelScope.launch { container.access.level.collect { level -> _state.update { it.copy(level = level) } } }
        viewModelScope.launch {
            container.log.entries.collect { entries ->
                val runs = entries.mapNotNull { (it.payload as? Payload.Benchmark)?.run }
                _state.update { it.copy(summaries = BenchmarkStats.summarize(runs)) }
            }
        }
        viewModelScope.launch { reloadMarker() }
    }

    fun selectDataset(kind: DatasetKind) = _state.update { it.copy(dataset = kind) }

    fun generate() {
        if (_state.value.busy) return
        if (!_state.value.level.isFull) {
            _state.update { it.copy(message = BenchmarkMessage.NeedsFullAccess) }
            return
        }
        job =
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        phase = BenchmarkPhase.GENERATING,
                        processed = 0,
                        target = plan.fileCount.toLong(),
                        message = null
                    )
                }
                val message =
                    try {
                        withContext(Dispatchers.Default) {
                            container.generator.generate(plan) { created ->
                                _state.update { it.copy(processed = created.toLong()) }
                            }
                        }
                        container.benchmark.markCold()
                        BenchmarkMessage.Done
                    } catch (error: InsufficientSpaceException) {
                        BenchmarkMessage.InsufficientSpace(error.requiredBytes, error.availableBytes)
                    } catch (_: CancellationException) {
                        BenchmarkMessage.Cancelled
                    } catch (_: IOException) {
                        BenchmarkMessage.Failed
                    }
                finish(message)
            }
    }

    fun run() {
        if (_state.value.busy) return
        val dataset = _state.value.dataset
        job =
            viewModelScope.launch {
                _state.update {
                    it.copy(phase = BenchmarkPhase.RUNNING, processed = 0, target = 0, elapsedMs = 0, message = null)
                }
                val message =
                    try {
                        container.benchmark.run(dataset) { event ->
                            if (event is BenchmarkEvent.Progress) {
                                _state.update {
                                    it.copy(
                                        currentStrategy = event.strategy,
                                        currentRepetition = event.repetition,
                                        processed = event.counts.files,
                                        elapsedMs = event.elapsedMs
                                    )
                                }
                            }
                        }
                        BenchmarkMessage.Done
                    } catch (_: CancellationException) {
                        BenchmarkMessage.Cancelled
                    }
                finish(message)
            }
    }

    /** "Back" during an operation asks first (contracts/ui-navigation.md). */
    fun requestCancel() = _state.update { it.copy(confirmCancel = it.busy) }

    fun dismissCancel() = _state.update { it.copy(confirmCancel = false) }

    fun cancel() {
        _state.update { it.copy(confirmCancel = false) }
        job?.cancel()
    }

    fun requestClean() {
        if (!_state.value.busy && _state.value.hasDataset) _state.update { it.copy(confirmClean = true) }
    }

    fun dismissClean() = _state.update { it.copy(confirmClean = false) }

    fun confirmClean() {
        _state.update { it.copy(confirmClean = false) }
        val marker = _state.value.marker ?: return
        job =
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        phase = BenchmarkPhase.CLEANING,
                        processed = 0,
                        target = marker.createdFiles.toLong(),
                        message = null
                    )
                }
                val message =
                    try {
                        container.cleaner.clean { deleted -> _state.update { it.copy(processed = deleted.toLong()) } }
                        BenchmarkMessage.Done
                    } catch (_: CancellationException) {
                        BenchmarkMessage.Cancelled
                    } catch (_: IOException) {
                        BenchmarkMessage.Failed
                    } catch (_: IllegalStateException) {
                        BenchmarkMessage.Failed
                    }
                finish(message)
            }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    // Also runs after a cancellation, so it must not be cancellable itself.
    private suspend fun finish(message: BenchmarkMessage) = withContext(NonCancellable) {
        reloadMarker()
        _state.update {
            it.copy(phase = BenchmarkPhase.IDLE, currentStrategy = null, confirmCancel = false, message = message)
        }
    }

    private suspend fun reloadMarker() {
        val marker = withContext(Dispatchers.IO) { DatasetMarker.read(container.datasetRoot) }
        _state.update { it.copy(marker = marker) }
    }
}
