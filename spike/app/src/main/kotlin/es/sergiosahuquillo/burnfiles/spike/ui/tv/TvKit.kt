package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.ui.theme.LocalSpikeColors

@Composable
fun TvScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
fun TvTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun TvBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun TvMessage(text: String, isError: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else LocalSpikeColors.current.success
    )
}

/** Label/value line. Not focusable: use [TvFocusableInfoRow] inside long lists. */
@Composable
fun TvInfoRow(
    label: String,
    value: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor
        )
        // A long value may take at most half of the row so the label never collapses.
        Text(
            value,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Focusable row so that long tables can be scrolled with the D-pad. Text takes the surface's content
 * colour, which inverts together with the container when the row is focused.
 */
@Composable
fun TvFocusableInfoRow(label: String, value: String, onClick: () -> Unit = {}, details: String? = null) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        val content = LocalContentColor.current
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TvInfoRow(label, value, labelColor = content, valueColor = content)
            details?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = content)
            }
        }
    }
}

/**
 * Requests focus on [requester] once, when the screen is first shown. Waits one frame so the target
 * is attached and containers that react to focus (the navigation drawer) have finished initialising.
 */
@Composable
fun InitialFocus(requester: FocusRequester, key: Any? = Unit) {
    LaunchedEffect(key) { requestFocusNextFrame(requester) }
}

/**
 * tv-material keeps disabled buttons focusable but draws no focus indication on them, so the D-pad
 * seems to lose focus. Use it for buttons that stay unavailable (no dataset, nothing to install), not
 * for ones disabled only while an operation runs: those would drop the focus they hold.
 */
fun Modifier.focusableWhen(enabled: Boolean): Modifier = focusProperties { canFocus = enabled }

suspend fun requestFocusNextFrame(requester: FocusRequester) {
    withFrameNanos { }
    runCatching { requester.requestFocus() }
}
