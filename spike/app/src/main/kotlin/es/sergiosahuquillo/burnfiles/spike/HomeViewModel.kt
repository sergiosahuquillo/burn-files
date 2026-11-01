package es.sergiosahuquillo.burnfiles.spike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.log.TestKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MatrixState {
    data object Loading : MatrixState

    data object Missing : MatrixState

    data class Chosen(val id: MatrixId) : MatrixState
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _matrix = MutableStateFlow<MatrixState>(MatrixState.Loading)
    val matrix: StateFlow<MatrixState> = _matrix.asStateFlow()

    /** Tests with at least one result, to pick the initial focus on TV (contracts/ui-navigation.md). */
    val testsWithResults: StateFlow<Set<TestKind>> =
        container.log.entries
            .map { entries -> entries.map { it.test }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            _matrix.value = container.device.matrixId()?.let(MatrixState::Chosen) ?: MatrixState.Missing
        }
    }

    fun choose(id: MatrixId) {
        viewModelScope.launch {
            container.device.saveMatrixId(id)
            _matrix.value = MatrixState.Chosen(id)
        }
    }
}
