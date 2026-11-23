package tachiyomi.presentation.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// RK: DropSauce's tile (bg_settings_tile): a flat surfaceContainer block with 20dp corners, no shadow.
// Replaces ElevatedCard wherever the app draws a list or stats card.
private val TileShape = RoundedCornerShape(20.dp)

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    shape: Shape = TileShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), // RK
        content = content,
    )
}

@Composable
fun TonalCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = TileShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), // RK
        content = content,
    )
}
