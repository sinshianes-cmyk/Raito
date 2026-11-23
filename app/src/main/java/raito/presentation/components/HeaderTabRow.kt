package raito.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A tab strip heading a block whose chip row filters what the tabs pick: global search's All / Manga /
 * Novels above its source chips, and the recents views above their content-type chips. Tabs rather
 * than a second chip row, since the two do different kinds of thing and two chip rows would both
 * read "All". Drawn as DropSauce's kind rail.
 */
@Composable
fun <T> HeaderTabRow(
    items: List<T>,
    selected: T,
    label: (T) -> StringResource,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    KindRail(
        labels = items.map { stringResource(label(it)) },
        selectedIndex = items.indexOf(selected).coerceAtLeast(0),
        onSelect = { onSelect(items[it]) },
        modifier = modifier,
    )
}
