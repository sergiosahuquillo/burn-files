package es.sergiosahuquillo.burnfiles.spike.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sergiosahuquillo.burnfiles.spike.HomeViewModel
import es.sergiosahuquillo.burnfiles.spike.MatrixState
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.ui.common.Screen
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel

@Composable
fun MobileShell() {
    val home = spikeViewModel { HomeViewModel(it) }
    val matrix by home.matrix.collectAsStateWithLifecycle()
    // Paint the theme background ourselves: the window background is only a launch placeholder.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = matrix) {
            MatrixState.Loading -> Box(Modifier.fillMaxSize())

            MatrixState.Missing ->
                Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    MobileMatrixPickerScreen(onPick = home::choose)
                }

            is MatrixState.Chosen -> MobileNavigation(state.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileNavigation(matrixId: MatrixId) {
    var screen by rememberSaveable { mutableStateOf<Screen?>(null) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    BackHandler(enabled = screen != null) { screen = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(screen?.title ?: R.string.app_name)) },
                navigationIcon = {
                    if (screen != null) {
                        // Goes through the dispatcher so screens can intercept it (e.g. running benchmark).
                        IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                null -> MobileHomeScreen(matrixId, onOpen = { screen = it })
                Screen.ACCESS -> MobileAccessScreen()
                Screen.BENCHMARK -> MobileBenchmarkScreen()
                Screen.UPDATE -> MobileUpdateScreen()
                Screen.USB -> MobileUsbScreen()
                Screen.RESULTS -> MobileResultsScreen()
            }
        }
    }
}
