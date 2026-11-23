package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BaseBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: () -> Unit = {},
    icon: @Composable RowScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            // RK --> DropSauce list card: tonal tile, 20dp corners, 16dp margins
            .padding(horizontal = MaterialTheme.padding.medium, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            // RK <--
            .combinedClickable(
                onClick = onClickItem,
                onLongClick = onLongClickItem,
            )
            .padding(horizontal = MaterialTheme.padding.medium, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        content()
        action()
    }
}
