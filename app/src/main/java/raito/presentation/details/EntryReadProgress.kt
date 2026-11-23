package raito.presentation.details

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Book
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * DropSauce's progress card: "N of M chapters read", the percent in the accent colour and a rounded bar.
 * Shown above the chapter list once the entry has at least one chapter.
 */
@Composable
internal fun EntryReadProgressCard(
    readCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    if (totalCount <= 0) return
    val accent = MaterialTheme.colorScheme.primary
    val fraction = (readCount.toFloat() / totalCount).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = fraction, label = "entryReadProgress")
    val percent = (fraction * 100f).toInt()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Book,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(MR.strings.entry_read_progress, readCount, totalCount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            if (readCount > 0) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.22f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animated)
                            .background(accent),
                    )
                }
            }
        }
    }
}
