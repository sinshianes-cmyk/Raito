package raito.presentation.reader

import android.content.res.Resources
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset

/*
 * Reader chrome primitives (scrim color + top/bottom bar show-hide animation) for
 * [eu.kanade.presentation.reader.appbars.ReaderAppBars], which the one reader host draws for both
 * content types, and for the vertical rail.
 */

/** Slide timing for the bars. */
val ReaderBarsSlideSpec = tween<IntOffset>(200)

/** Fade timing for the bars. */
val ReaderBarsFadeSpec = tween<Float>(150)

/** Translucent toolbar / scrim color behind both readers' bars. */
@Composable
fun readerChromeColor(): Color = MaterialTheme.colorScheme
    .surfaceContainer // RK: DropSauce's reader panels (bg_reader_panel) sit on colorSurfaceContainer
    .copy(alpha = if (isSystemInDarkTheme()) 0.94f else 0.97f)

/** Bars travel 24 dp while fading, rather than their full height. */
private fun barSlideOffsetPx(fromBottom: Boolean): Int {
    val px = (24 * Resources.getSystem().displayMetrics.density).toInt()
    return if (fromBottom) px else -px
}

/** Enter transition for a top ([fromBottom] = false) or bottom ([fromBottom] = true) reader bar. */
fun readerBarEnter(fromBottom: Boolean): EnterTransition =
    slideInVertically(ReaderBarsSlideSpec) { barSlideOffsetPx(fromBottom) } + fadeIn(ReaderBarsFadeSpec)

/** Exit transition mirroring [readerBarEnter]. */
fun readerBarExit(fromBottom: Boolean): ExitTransition =
    slideOutVertically(ReaderBarsSlideSpec) { barSlideOffsetPx(fromBottom) } + fadeOut(ReaderBarsFadeSpec)
