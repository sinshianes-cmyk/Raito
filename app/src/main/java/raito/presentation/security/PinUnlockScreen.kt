package raito.presentation.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Full-screen PIN pad used by the unlock activity. Auto-submits at 4+ digits when the PIN matches. */
@Composable
fun PinUnlockScreen(check: (String) -> Boolean, onUnlocked: () -> Unit, onCancel: () -> Unit) {
    TachiyomiTheme {
        BackHandler(onBack = onCancel)
        Surface(modifier = Modifier.fillMaxSize()) {
            var pin by remember { mutableStateOf("") }
            var wrong by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (wrong) stringResource(MR.strings.pin_wrong) else "•".repeat(pin.length).ifEmpty { " " },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(24.dp))
                PinPad(
                    onDigit = {
                        wrong = false
                        if (pin.length < MAX_PIN) pin += it
                        if (pin.length >= MIN_PIN && check(pin)) onUnlocked()
                    },
                    onBackspace = { pin = pin.dropLast(1); wrong = false },
                    onSubmit = { if (check(pin)) onUnlocked() else { wrong = true; pin = "" } },
                )
            }
        }
    }
}

const val MIN_PIN = 4
const val MAX_PIN = 12

@Composable
fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d -> PadKey(d) { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadKey("⌫", onBackspace)
            PadKey("0") { onDigit("0") }
            PadKey("OK", onSubmit)
        }
    }
}

@Composable
private fun PadKey(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.size(width = 80.dp, height = 56.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
