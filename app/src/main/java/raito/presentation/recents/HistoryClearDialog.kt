package raito.presentation.recents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import eu.kanade.presentation.components.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import raito.domain.history.HistoryClearRange
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Clear history asks how much to clear, the way a single-choice list does, with Today preselected. */
@Composable
fun HistoryClearDialog(
    onDismissRequest: () -> Unit,
    onClear: (HistoryClearRange) -> Unit,
) {
    var selected by remember { mutableStateOf(HistoryClearRange.TODAY) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.pref_clear_history)) },
        text = {
            Column {
                HistoryClearRange.entries.forEach { range ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = range == selected, role = Role.RadioButton) { selected = range }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = range == selected, onClick = null)
                        Text(text = stringResource(range.label), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onClear(selected)
                    onDismissRequest()
                },
            ) { Text(stringResource(MR.strings.action_clear)) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) } },
    )
}

private val HistoryClearRange.label
    get() = when (this) {
        HistoryClearRange.LAST_TWO_HOURS -> MR.strings.clear_history_last_two_hours
        HistoryClearRange.TODAY -> MR.strings.clear_history_today
        HistoryClearRange.NOT_IN_LIBRARY -> MR.strings.clear_history_not_in_library
        HistoryClearRange.ALL -> MR.strings.clear_history_all
    }
