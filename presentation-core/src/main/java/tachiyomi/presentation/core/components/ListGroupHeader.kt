package tachiyomi.presentation.core.components

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(
                horizontal = ScreenHorizontalPadding,
                vertical = 12.dp,
            ),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}
