package raito.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.theme.TachiyomiTheme
import kotlinx.coroutines.delay
import raito.domain.entry.EntryId
import raito.domain.entry.signedKey
import tachiyomi.domain.manga.model.MangaCover

/** The cover tint a details screen applies, and whether it is still being found. */
class CoverSeed(val color: Color?, val waiting: Boolean)

private const val SEED_WAIT_MS = 700L

/**
 * The cover tint for [id]. A colour found on an earlier visit is read from the cache at once, so the
 * screen opens in the cover theme instead of the app theme and then switching. A title seen for the
 * first time is [CoverSeed.waiting] until Palette finishes (or [SEED_WAIT_MS] passes), so nothing is
 * drawn in the wrong colours meanwhile.
 */
@Composable
fun rememberCoverSeed(id: EntryId, extracted: Color?, enabled: Boolean): CoverSeed {
    val cached = remember(id) { MangaCover.vibrantCoverColorMap[id.signedKey()]?.let { Color(it) } }
    var timedOut by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, enabled) {
        if (enabled) {
            delay(SEED_WAIT_MS)
            timedOut = true
        }
    }
    val color = extracted ?: cached
    return CoverSeed(
        color = color.takeIf { enabled },
        waiting = enabled && color == null && !timedOut,
    )
}

/** [TachiyomiTheme] seeded from the cover; holds the content back while the first colour is found. */
@Composable
fun CoverTheme(seed: CoverSeed, content: @Composable () -> Unit) {
    if (seed.waiting) {
        Box(Modifier.fillMaxSize())
    } else {
        TachiyomiTheme(seedColor = seed.color, content = content)
    }
}
