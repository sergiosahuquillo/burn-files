package es.sergiosahuquillo.burnfiles.spike.ui.tv

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.HomeViewModel
import es.sergiosahuquillo.burnfiles.spike.MatrixState
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.ui.common.Screen
import es.sergiosahuquillo.burnfiles.spike.ui.common.label
import es.sergiosahuquillo.burnfiles.spike.ui.common.spikeViewModel

/** 5 % overscan margins on a 960×540 dp TV canvas (contracts/ui-navigation.md). */
private val OverscanHorizontal = 48.dp
private val OverscanVertical = 27.dp

@Composable
fun TvShell() {
    val home = spikeViewModel { HomeViewModel(it) }
    val matrix by home.matrix.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Box(Modifier.padding(horizontal = OverscanHorizontal, vertical = OverscanVertical)) {
            when (val state = matrix) {
                MatrixState.Loading -> Unit
                MatrixState.Missing -> TvMatrixPickerScreen(onPick = home::choose)
                is MatrixState.Chosen -> TvNavigation(home, state.id)
            }
        }
    }
}

@Composable
private fun TvNavigation(home: HomeViewModel, matrixId: MatrixId) {
    var screen by rememberSaveable { mutableStateOf<Screen?>(null) }
    var lastScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
    val tested by home.testsWithResults.collectAsStateWithLifecycle()
    val itemFocus = remember { Screen.entries.associateWith { FocusRequester() } }

    // "Back" from a screen returns to Home (the drawer); from Home it leaves the app.
    BackHandler(enabled = screen != null) {
        lastScreen = screen
        screen = null
    }
    LaunchedEffect(screen) {
        if (screen == null) {
            val target = lastScreen ?: Screen.initialFor(tested)
            requestFocusNextFrame(itemFocus.getValue(target))
        }
    }

    NavigationDrawer(
        drawerContent = {
            Column(
                modifier = Modifier.fillMaxHeight().padding(12.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                Screen.entries.forEach { entry ->
                    NavigationDrawerItem(
                        selected = screen == entry,
                        onClick = { screen = entry },
                        leadingContent = { Icon(painterResource(entry.icon), contentDescription = null) },
                        modifier = Modifier.focusRequester(itemFocus.getValue(entry))
                    ) { Text(stringResource(entry.title)) }
                }
            }
        }
    ) {
        Box(Modifier.fillMaxSize().padding(start = 24.dp)) {
            when (screen) {
                null -> TvHomeContent(matrixId)
                Screen.ACCESS -> TvAccessScreen()
                Screen.BENCHMARK -> TvBenchmarkScreen()
                Screen.UPDATE -> TvUpdateScreen()
                Screen.USB -> TvUsbScreen()
                Screen.RESULTS -> TvResultsScreen()
            }
        }
    }
}

@Composable
private fun TvHomeContent(matrixId: MatrixId) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TvTitle(stringResource(R.string.app_name))
        TvBody(stringResource(R.string.home_device, stringResource(matrixId.label), Build.MODEL, Build.VERSION.SDK_INT))
        TvBody(stringResource(R.string.home_tv_help))
    }
}
