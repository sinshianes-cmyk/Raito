package raito.presentation.migrate.flow

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import raito.presentation.browse.EntryBrowseItemUi
import raito.presentation.browse.EntrySearchCardRow
import raito.presentation.browse.EntrySearchSection
import raito.presentation.browse.components.formatLabel
import raito.presentation.browse.components.sourceDetail
import raito.presentation.browse.rememberEntryGestures
import raito.presentation.browse.sourceFailureMessage
import raito.presentation.browse.sourceLanguageName

/**
 * One source's migration candidates, under the shared global-search section header and rendering the
 * same result cards global search does. Both surfaces that offer a target render this: the
 * single-entry search screen and the batch list's override strip.
 * Tapping a candidate picks it; long-pressing opens its details, so a match can be checked before it
 * is committed to. Tapping the header browses the whole source, the way out when search cannot reach
 * the title.
 */
@Composable
internal fun MigrationCandidateStrip(
    strip: SourceStrip,
    /** Whether the searched sources hold more than one packaging, so this heading names its own. */
    showsFormat: Boolean,
    onPick: (MigrationCandidate) -> Unit,
    onPreview: (MigrationCandidate) -> Unit,
    onBrowseSource: () -> Unit,
    modifier: Modifier = Modifier,
    /** This is the source the entry is on now. Marked, as upstream marks it, because a hit here is
     *  the entry itself far more often than it is a target worth having. */
    isCurrentSource: Boolean = false,
) {
    val result = strip.result
    val context = LocalContext.current
    // A strip always chooses, so its long press previews and add is never reached.
    val gestures = rememberEntryGestures(choose = onPick, open = onPreview, add = {})
    EntrySearchSection(
        title = if (isCurrentSource) "▶ ${strip.sourceName}" else strip.sourceName,
        subtitle = sourceDetail(
            language = sourceLanguageName(strip.sourceLang, context),
            format = formatLabel(strip.sourceFormat, showsFormat),
        ).orEmpty(),
        onClick = onBrowseSource,
        modifier = modifier,
    ) {
        when (result) {
            is StripResult.Loading -> GlobalSearchLoadingResultItem()
            // A source that threw says so; "no results" would be a different, wrong answer.
            is StripResult.Failed ->
                GlobalSearchErrorResultItem(message = with(context) { result.error.sourceFailureMessage })
            // The same row global search renders, so a candidate reads here exactly as it does
            // there: in-library entries dimmed and badged, and still pickable, since migrating onto
            // a library entry is the replace case rather than a mistake.
            is StripResult.Loaded -> EntrySearchCardRow(
                entries = result.candidates,
                key = { it.key },
                toUi = { EntryBrowseItemUi(title = it.title, cover = it.cover ?: "", favorite = it.inLibrary) },
                gestures = gestures,
                isSelected = { false },
            )
        }
    }
}
