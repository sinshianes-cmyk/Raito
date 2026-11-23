package raito.presentation.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What the bar under a continue-reading row draws: how far through the series the reader is, not
 * through the one chapter the row names, which is what lets a row that was read to its end still say
 * how much of the series is left.
 */
@Immutable
sealed interface RowProgress {
    /** Every chapter of the series is read: a full bar in the done colour, labelled rather than only coloured. */
    data object Done : RowProgress

    /** [fraction] of the series, with the whole [percent] written beside the bar. */
    data class Partial(val fraction: Float, val percent: Int) : RowProgress
}

/**
 * The bar for a row's series, or null where there is nothing to measure: no series counts, or a series
 * with no chapters. Percent rounds down, so a series one chapter short never reads as 100%.
 */
fun rowProgress(series: SeriesProgress?): RowProgress? {
    if (series == null || series.chapterCount <= 0) return null
    val read = series.chaptersRead.coerceIn(0, series.chapterCount)
    if (read == series.chapterCount) return RowProgress.Done
    val fraction = read / series.chapterCount.toFloat()
    return RowProgress.Partial(fraction, (fraction * 100f).toInt())
}

/** Reading-done green. Used for this one state only, and always beside the "Done" label. */
private val DoneColor = Color(0xFF2F8F63)

/** Red, through amber and blue, to green: the bar's colour follows how far through the series the reader is. */
private val RampStops = listOf(
    0f to Color(0xFFE5534B),
    0.4f to Color(0xFFF2A33A),
    0.75f to Color(0xFF4FB3D9),
    1f to Color(0xFF3FBF8A),
)

private fun progressColor(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    val upper = RampStops.indexOfFirst { it.first >= f }.coerceAtLeast(1)
    val (f0, c0) = RampStops[upper - 1]
    val (f1, c1) = RampStops[upper]
    return lerp(c0, c1, ((f - f0) / (f1 - f0)).coerceIn(0f, 1f))
}

/** Keeps "just started" visibly apart from "never opened", which would otherwise both be an empty track. */
private const val MIN_VISIBLE_FRACTION = 0.02f

/**
 * A thick, rounded progress bar with its percent beside it, the shape the History card is built
 * around. Drawn from two boxes rather than a Material indicator: it never animates, so there is
 * nothing for reduced-motion to switch off, and its thickness is exact.
 */
@Composable
fun RowProgressBar(progress: RowProgress, modifier: Modifier = Modifier) {
    val fraction = when (progress) {
        RowProgress.Done -> 1f
        is RowProgress.Partial -> progress.fraction.coerceAtLeast(MIN_VISIBLE_FRACTION)
    }
    val fill = when (progress) {
        RowProgress.Done -> DoneColor
        is RowProgress.Partial -> progressColor(progress.fraction)
    }
    val label = when (progress) {
        RowProgress.Done -> stringResource(MR.strings.recents_row_done)
        is RowProgress.Partial -> "${progress.percent}%"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_THICKNESS)
                .clip(RoundedCornerShape(BAR_THICKNESS / 2))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(fill),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = fill,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 10.dp)
                .widthIn(min = 34.dp),
        )
    }
}

private val BAR_THICKNESS = 7.dp
