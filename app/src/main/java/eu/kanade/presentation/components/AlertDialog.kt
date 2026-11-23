package eu.kanade.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog as MaterialAlertDialog

// DropSauce's dialog card: very round corners (half of its 46dp corner button + 24dp padding) with a
// 1dp outline-variant stroke, on the container colour of its expressive dialog.
private val DropSauceDialogShape = RoundedCornerShape(23.dp + 24.dp)

/**
 * Drop-in for Material 3's `AlertDialog` that every dialog in the app goes through, so they all take
 * DropSauce's card look in one place. Same parameters as the Material one; only the defaults differ.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = DropSauceDialogShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    MaterialAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}
