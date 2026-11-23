package raito.presentation.security

import androidx.compose.foundation.layout.Column
import eu.kanade.presentation.components.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Two-step PIN entry: choose, then confirm. Mismatch restarts the confirm step with an error. */
@Composable
fun SetPinDialog(onConfirm: (String) -> Unit, onDismissRequest: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(if (first == null) MR.strings.pin_create else MR.strings.pin_confirm)) },
        text = {
            Column {
                Text(
                    text = when {
                        mismatch -> stringResource(MR.strings.pin_mismatch)
                        pin.isEmpty() -> stringResource(MR.strings.pin_min_digits, MIN_PIN)
                        else -> "•".repeat(pin.length)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                PinPad(
                    onDigit = { if (pin.length < MAX_PIN) { pin += it; mismatch = false } },
                    onBackspace = { pin = pin.dropLast(1) },
                    onSubmit = {
                        if (pin.length < MIN_PIN) return@PinPad
                        val firstPin = first
                        when {
                            firstPin == null -> { first = pin; pin = "" }
                            firstPin == pin -> onConfirm(pin)
                            else -> { first = null; pin = ""; mismatch = true }
                        }
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) } },
    )
}
