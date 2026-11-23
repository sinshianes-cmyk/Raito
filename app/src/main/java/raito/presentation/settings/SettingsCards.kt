package raito.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding

/** Where a settings row sits in its group: 24dp outer corners, 4dp seams, as DropSauce's SettingsGroup. */
@Immutable
data class CardPosition(val index: Int, val total: Int) {
    val shape: Shape
        get() = when {
            total <= 1 -> RoundedCornerShape(OuterCorner)
            index == 0 -> RoundedCornerShape(OuterCorner, OuterCorner, SeamCorner, SeamCorner)
            index == total - 1 -> RoundedCornerShape(SeamCorner, SeamCorner, OuterCorner, OuterCorner)
            else -> RoundedCornerShape(SeamCorner)
        }

    private companion object {
        val OuterCorner = 24.dp
        val SeamCorner = 4.dp
    }
}

/** Null outside a grouped preference list, so a widget reused elsewhere keeps its flat look. */
val LocalPreferenceCardPosition = compositionLocalOf<CardPosition?> { null }

/** Tonal card for one grouped row, with the 16dp page margin and the 2dp seam below it. */
@Composable
fun Modifier.preferenceCard(position: CardPosition, color: Color = MaterialTheme.colorScheme.surfaceContainer): Modifier =
    this
        .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = 2.dp)
        .clip(position.shape)
        .background(color)

/** Circle icon colours for one settings category: tinted container, contrasting glyph. */
@Immutable
data class CategoryIconColors(val container: Color, val onContainer: Color)

/**
 * Icon tile colours per settings category, drawn from the live colour scheme so every DropSauce
 * theme (and Monet) tints the tiles itself. Categories share the primary, secondary and tertiary
 * container families, so neighbouring rows still read apart without a hard-coded palette.
 */
object CategoryPalette {
    @Composable
    fun forKey(key: String): CategoryIconColors {
        val c = MaterialTheme.colorScheme
        return when (key) {
            "appearance", "services" -> CategoryIconColors(c.primaryContainer, c.onPrimaryContainer)
            "library", "backup" -> CategoryIconColors(c.secondaryContainer, c.onSecondaryContainer)
            "reader", "downloads" -> CategoryIconColors(c.tertiaryContainer, c.onTertiaryContainer)
            "storage" -> CategoryIconColors(c.primary, c.onPrimary)
            "tracker" -> CategoryIconColors(c.errorContainer, c.onErrorContainer)
            else -> CategoryIconColors(c.surfaceVariant, c.onSurfaceVariant)
        }
    }
}

/** The 44dp round icon tile on a settings row. */
@Composable
fun SettingsIconBubble(icon: ImageVector, colors: CategoryIconColors, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}
