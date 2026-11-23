package raito.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * DropSauce's sheet tabs: a scrolling row where the picked tab sits on a rounded tonal pill instead of
 * under an underline. Give [icons] to draw icon tabs, with the label as their description.
 */
@Composable
fun PillTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val container by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                label = "pillTabContainer",
            )
            val content by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "pillTabContent",
            )
            Row(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .widthIn(min = 64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(container)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icons != null) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = label,
                        tint = content,
                    )
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * DropSauce's explore kind rail: a small tonal rail that hugs its tabs, labels in the accent colour when
 * picked, with a short rounded underline beneath the label rather than a full-width one.
 */
@Composable
fun KindRail(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "kindTabColor",
                )
                val indicator by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "kindTabIndicator",
                )
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 88.dp)
                        .clickable(role = Role.Tab) { onSelect(index) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = color,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(indicator),
                        )
                    }
                }
            }
        }
    }
}
