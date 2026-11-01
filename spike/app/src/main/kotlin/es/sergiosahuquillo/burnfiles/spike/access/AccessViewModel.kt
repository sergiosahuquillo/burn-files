package es.sergiosahuquillo.burnfiles.spike.access

import android.app.Activity
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sergiosahuquillo.burnfiles.spike.AppContainer
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccessUiState(
    val level: AccessLevel = AccessLevel.NONE,
    val running: Boolean = false,
    val step: AccessStep? = null,
    val attempts: List<AccessAttempt> = emptyList(),
    val finalLevel: AccessLevel? = null,
    val coverage: MediaCoverage? = null,
    /** Runtime permissions the screen must request now; `null` when none. */
    val permissionRequest: List<String>? = null
)

class AccessViewModel(private val container: AppContainer) : ViewModel() {
    private val chain = AccessChain(modernPermissionModel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    private val _state = MutableStateFlow(AccessUiState())
    val state: StateFlow<AccessUiState> = _state.asStateFlow()

    val adbCommand = SettingsIntents.adbGrantCommand(container.app.packageName)

    init {
        viewModelScope.launch { container.access.level.collect { level -> _state.update { it.copy(level = level) } } }
        viewModelScope.launch {
            container.access.resumes.collect { level ->
                if (_state.value.running) {
                    chain.onResumed(level)
                    afterTransition(level)
                }
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            val level = container.access.peek()
            chain.start(level)
            _state.update { it.copy(running = true, coverage = null) }
            afterTransition(level)
        }
    }

    /** Executes the current step. Settings screens need the [activity] to be started from. */
    fun runStep(activity: Activity) {
        val step = chain.step ?: return
        when (step) {
            AccessStep.APP_SETTINGS, AccessStep.GENERIC_SETTINGS -> {
                val result = activity.launchSafely(SettingsIntents.forStep(step, activity.packageName))
                if (result.outcome == AttemptOutcome.OPENED) {
                    chain.onLaunched(result.resolvable)
                } else {
                    chain.onLaunchFailed(result.resolvable, result.outcome, _state.value.level)
                }
                viewModelScope.launch { afterTransition(_state.value.level) }
            }

            AccessStep.ADB -> viewModelScope.launch { container.access.refresh() }

            AccessStep.RUNTIME_DIALOG, AccessStep.MEDIA_ONLY ->
                _state.update { it.copy(permissionRequest = AccessChecker.runtimePermissionsFor(step)) }
        }
    }

    fun onPermissionResult() {
        _state.update { it.copy(permissionRequest = null) }
        viewModelScope.launch {
            val level = container.access.peek()
            val granted = if (chain.step == AccessStep.RUNTIME_DIALOG) level.isFull else level != AccessLevel.NONE
            chain.onRuntimeResult(granted, level)
            afterTransition(level)
        }
    }

    fun skipAdb() {
        viewModelScope.launch {
            val level = container.access.peek()
            chain.skipAdb(level)
            afterTransition(level)
        }
    }

    private suspend fun afterTransition(level: AccessLevel) {
        // Media access may already be there (e.g. READ granted, WRITE denied): nothing to ask.
        if (!chain.isFinished && chain.step == AccessStep.MEDIA_ONLY && level == AccessLevel.MEDIA_ONLY) {
            chain.onRuntimeResult(granted = true, level = level)
        }
        _state.update { it.copy(step = chain.step, attempts = chain.attempts) }
        val finalLevel = chain.finalLevel ?: return
        if (!_state.value.running) return
        val coverage = if (finalLevel == AccessLevel.MEDIA_ONLY) container.mediaCoverage.measure() else null
        container.recorder.record(Payload.Access(chain.attempts, finalLevel, coverage))
        _state.update { it.copy(running = false, step = null, finalLevel = finalLevel, coverage = coverage) }
    }
}
