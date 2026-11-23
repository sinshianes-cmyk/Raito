package eu.kanade.presentation.more.settings.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding

/** Colours a preview drawing may use, taken from the live colour scheme by the widget. */
class PreviewPalette(val page: Color, val accent: Color)

/** What one tile of a [PreviewChoicePreferenceWidget] draws, in the tile's own coordinates. */
typealias PreviewDrawing = DrawScope.(PreviewPalette) -> Unit

/**
 * DropSauce's preview-choice row: a title with one tile per option beneath it, each tile a tiny drawing
 * of what the option does, the picked one wearing a thick accent outline.
 */
@Composable
fun <T> PreviewChoicePreferenceWidget(
    title: String,
    entries: Map<T, String>,
    value: T,
    onValueChange: (T) -> Unit,
    drawing: (T) -> PreviewDrawing,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val palette = PreviewPalette(
        page = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        accent = MaterialTheme.colorScheme.primary,
    )
    val haptic = LocalHapticFeedback.current
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEach { (key, label) ->
                val draw = drawing(key)
                PreviewTile(
                    label = label,
                    selected = key == value,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onValueChange(key)
                    },
                    // Share the row on phones, stop growing on tablets.
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 96.dp),
                ) {
                    Canvas(modifier = Modifier.matchParentSize(), onDraw = { draw(this, palette) })
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val borderWidth by animateDpAsState(if (selected) 3.dp else 1.dp, label = "tileBorder")
    val borderColor by animateColorAsState(
        if (selected) scheme.primary else scheme.outlineVariant,
        label = "tileBorderColor",
    )
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clickable(interactionSource = null, indication = null, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(shape)
                .background(scheme.surfaceContainerHighest)
                .border(borderWidth, borderColor, shape),
            content = content,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A rounded block placed in fractions of the tile (0..1 on both axes). */
fun DrawScope.previewBlock(color: Color, x: Float, y: Float, w: Float, h: Float, radius: Float = 0.04f) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x * size.width, y * size.height),
        size = Size(w * size.width, h * size.height),
        cornerRadius = CornerRadius(radius * size.width),
    )
}

/** An arrow between two points given in fractions of the tile. */
fun DrawScope.previewArrow(color: Color, x1: Float, y1: Float, x2: Float, y2: Float) {
    val start = Offset(x1 * size.width, y1 * size.height)
    val end = Offset(x2 * size.width, y2 * size.height)
    val stroke = size.width * 0.06f
    val head = size.width * 0.13f
    val dir = (end - start) / (end - start).getDistance()
    val normal = Offset(-dir.y, dir.x)
    drawLine(color, start, end, stroke, StrokeCap.Round)
    drawLine(color, end, end - dir * head + normal * head, stroke, StrokeCap.Round)
    drawLine(color, end, end - dir * head - normal * head, stroke, StrokeCap.Round)
}
