package es.sergiosahuquillo.burnfiles.spike.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text as MobileText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import es.sergiosahuquillo.burnfiles.spike.R

object DialogTags {
    const val DIALOG = "confirm_dialog"
    const val CANCEL = "confirm_dialog_cancel"
    const val CONFIRM = "confirm_dialog_confirm"
}

/** Body of every destructive confirmation: number of items and total size (constitution II). */
@Composable
fun destructiveBody(count: Long, bytes: Long): String {
    val format = rememberFormat()
    return pluralStringResource(
        R.plurals.confirm_delete_body,
        count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        format.count(count),
        format.bytes(bytes)
    )
}

/** Mobile confirmation. The default (dismiss) button is "Cancel"; deleting needs an explicit tap. */
@Composable
fun MobileConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag(DialogTags.DIALOG),
        onDismissRequest = onDismiss,
        title = { MobileText(title) },
        text = { MobileText(body) },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(DialogTags.CANCEL)) {
                MobileText(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(DialogTags.CONFIRM)) {
                MobileText(confirmLabel)
            }
        }
    )
}

/** TV confirmation. Initial focus on "Cancel"; "Back" dismisses. */
@Composable
fun TvConfirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).testTag(DialogTags.DIALOG),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TvText(title, style = MaterialTheme.typography.headlineSmall)
                TvText(body, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(cancelFocus).testTag(DialogTags.CANCEL)
                    ) { TvText(stringResource(R.string.action_cancel)) }
                    OutlinedButton(onClick = onConfirm, modifier = Modifier.testTag(DialogTags.CONFIRM)) {
                        TvText(confirmLabel)
                    }
                }
            }
        }
        // Inside the dialog: it composes in its own window, after the caller's effects have run.
        LaunchedEffect(Unit) {
            withFrameNanos { }
            cancelFocus.requestFocus()
        }
    }
}
