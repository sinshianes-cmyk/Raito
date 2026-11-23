package raito.presentation.details

import eu.kanade.presentation.components.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Asks for an image address; [onConfirm] gets it only when it is an http or https one. */
@Composable
fun CoverUrlDialog(onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    val valid = url.trim().let { it.startsWith("http://") || it.startsWith("https://") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.cover_from_url)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                placeholder = { Text("https://") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(url.trim()) }) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
