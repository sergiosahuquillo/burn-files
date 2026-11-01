package es.sergiosahuquillo.burnfiles.spike.ui.mobile

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.device.MatrixId
import es.sergiosahuquillo.burnfiles.spike.ui.common.LocalAppContainer
import es.sergiosahuquillo.burnfiles.spike.ui.common.Screen
import es.sergiosahuquillo.burnfiles.spike.ui.common.label

@Composable
fun MobileHomeScreen(matrixId: MatrixId, onOpen: (Screen) -> Unit) {
    val entries by LocalAppContainer.current.log.entries
        .collectAsStateWithLifecycle()
    MobileScreenColumn {
        Text(
            stringResource(R.string.home_device, stringResource(matrixId.label), Build.MODEL, Build.VERSION.SDK_INT),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Screen.entries.forEach { screen ->
            val count = if (screen.test == null) entries.size else entries.count { it.test == screen.test }
            Card(onClick = { onOpen(screen) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(screen.icon), contentDescription = null, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(screen.title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(screen.description), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            pluralStringResource(R.plurals.home_results_count, count, count),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MobileMatrixPickerScreen(onPick: (MatrixId) -> Unit) {
    MobileScreenColumn {
        Text(stringResource(R.string.matrix_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.matrix_help), style = MaterialTheme.typography.bodyMedium)
        MatrixId.entries.forEach { id ->
            Card(onClick = { onPick(id) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(id.label),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
