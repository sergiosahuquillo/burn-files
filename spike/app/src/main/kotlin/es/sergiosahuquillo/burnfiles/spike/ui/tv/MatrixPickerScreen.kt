package es.sergiosahuquillo.burnfiles.spike.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.ui.common.label

/** First launch on TV: choose the matrix device. Focus starts on the first entry. */
@Composable
fun TvMatrixPickerScreen(onPick: (MatrixId) -> Unit) {
    val first = remember { FocusRequester() }
    TvScreenColumn {
        TvTitle(stringResource(R.string.matrix_title))
        TvBody(stringResource(R.string.matrix_help))
        Column(modifier = Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MatrixId.entries.forEachIndexed { index, id ->
                ListItem(
                    selected = false,
                    onClick = { onPick(id) },
                    modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                    headlineContent = { Text(stringResource(id.label)) }
                )
            }
        }
    }
    InitialFocus(first)
}
