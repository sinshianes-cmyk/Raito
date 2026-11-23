package raito.presentation.components

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import tachiyomi.presentation.core.components.TonalFilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import raito.domain.library.ContentType
import raito.domain.library.labelRes
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Sticky `All / Manga / Novels` switch shared by every content-type chip row, labelled through
 * [labelRes] so callers only pass the selected type. Net-new Raito UI, built on the
 * [raito.presentation.components.MergeSourceChips] chip-row pattern.
 */
@Composable
fun ContentTypeFilterChips(
    selected: ContentType,
    onSelect: (ContentType) -> Unit,
    modifier: Modifier = Modifier,
    types: List<ContentType> = ContentType.entries,
    // Opt-in per-type count badge (the Extensions tab passes pending updates per type); empty = none.
    badges: Map<ContentType, Int> = emptyMap(),
) {
    val context = LocalContext.current
    // Novels off: Manga is the only type left, so there is nothing to switch and the row goes.
    val novelsEnabled by remember { context.appGraph.raitoSourcePreferences.novelsEnabled }.collectAsState()
    if (!novelsEnabled) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            val count = badges[type] ?: 0
            TonalFilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(stringResource(type.labelRes)) },
                trailingIcon = if (count > 0) {
                    { Badge { Text(text = "$count") } }
                } else {
                    null
                },
            )
        }
    }
}

/**
 * The scroll position of the list under a [ContentTypeFilterChips] row, which starts at the top whenever
 * [selected] changes. Left to itself a keyed list keeps the old first row in view, so a section the new
 * chip adds above it (the Extensions tab's pending updates) opens scrolled off the top.
 */
@Composable
fun rememberChipListState(selected: ContentType): LazyListState =
    rememberSaveable(selected, saver = LazyListState.Saver) { LazyListState() }

/** [rememberChipListState] for a plain scrolling column under the chip row. */
@Composable
fun rememberChipScrollState(selected: ContentType): ScrollState =
    rememberSaveable(selected, saver = ScrollState.Saver) { ScrollState(0) }
