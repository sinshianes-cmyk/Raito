package eu.kanade.presentation.more.settings.widget

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * DropSauce's segmented settings row: a title (and optional subtitle) with the whole choice laid out
 * beneath it as a connected button group, for short fixed sets that do not deserve a dialog.
 */
@Composable
fun <T> SegmentedPreferenceWidget(
    title: String,
    entries: Map<T, String>,
    value: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val keys = entries.keys.toList()
    val labels = entries.values.toList()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding, vertical = PrefsVerticalPadding),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        SegmentedRow(
            labels = labels,
            selectedIndex = keys.indexOf(value),
            onSelected = { onValueChange(keys[it]) },
        )
    }
}

/**
 * Connected button group: 2dp gaps, pill-shaped outer corners, 8dp inner seams, and the selected
 * segment swelling slightly as it takes the accent colour.
 */
@Composable
fun SegmentedRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            val isFirst = index == 0
            val isLast = index == labels.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (isFirst) 50.dp else 8.dp,
                bottomStart = if (isFirst) 50.dp else 8.dp,
                topEnd = if (isLast) 50.dp else 8.dp,
                bottomEnd = if (isLast) 50.dp else 8.dp,
            )
            val alpha = if (enabled) 1f else 0.38f
            val background by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                } else {
                    // the tonal surfaces are flat now, so the idle segment uses a faint on-surface wash
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * alpha)
                },
                label = "segment_bg_$index",
            )
            val foreground by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                },
                label = "segment_fg_$index",
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.94f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "segment_scale_$index",
            )
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onSelected(index)
                },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .scale(scale),
                shape = shape,
                color = background,
                contentColor = foreground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}
