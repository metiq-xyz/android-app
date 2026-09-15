package xyz.metiq.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import xyz.metiq.MAX_CUSTOM_MIX_NAME_LENGTH
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter

@Composable
internal fun SaveMixDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mix_save_title), fontFamily = Inter) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_CUSTOM_MIX_NAME_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.mix_save_name_hint), fontFamily = Inter) },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim().replace('|', ' ')) },
            ) { Text(stringResource(R.string.mix_save_confirm), fontFamily = Inter) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
            }
        },
    )
}

@Composable
internal fun DeleteMixDialog(
    mixName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mix_delete_title), fontFamily = Inter) },
        text = { Text(stringResource(R.string.mix_delete_message, mixName), fontFamily = Inter) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.mix_delete_confirm), fontFamily = Inter)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
            }
        },
    )
}
