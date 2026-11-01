package es.sergiosahuquillo.burnfiles.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import es.sergiosahuquillo.burnfiles.spike.device.UiMode
import es.sergiosahuquillo.burnfiles.spike.ui.common.LocalAppContainer
import es.sergiosahuquillo.burnfiles.spike.ui.mobile.MobileShell
import es.sergiosahuquillo.burnfiles.spike.ui.theme.MobileTheme
import es.sergiosahuquillo.burnfiles.spike.ui.theme.TvTheme
import es.sergiosahuquillo.burnfiles.spike.ui.tv.TvShell
import kotlinx.coroutines.launch

/** Single activity; the UI layer (mobile or TV) is chosen at runtime (research R3). */
class SpikeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SpikeApp).container
        val uiMode = container.device.uiMode()
        if (uiMode == UiMode.MOBILE) enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            // Permissions are re-evaluated every time the app returns to the foreground (FR-001).
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { container.access.refresh() } }
            CompositionLocalProvider(LocalAppContainer provides container) {
                when (uiMode) {
                    UiMode.TV -> TvTheme { TvShell() }
                    UiMode.MOBILE -> MobileTheme { MobileShell() }
                }
            }
        }
    }
}
