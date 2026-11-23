package raito.presentation.browse

import kotlinx.coroutines.flow.MutableStateFlow
import raito.presentation.browse.catalogue.EntryBrowseRow
import raito.presentation.browse.catalogue.EntryBrowseRowContent

/** A result row carrying only [key], for tests about which rows land where rather than what they show. */
fun resultRow(key: String) = EntryBrowseRow(
    key = key,
    content = MutableStateFlow(
        EntryBrowseRowContent(EntryBrowseItemUi(title = key, cover = key, favorite = false), key),
    ),
)
