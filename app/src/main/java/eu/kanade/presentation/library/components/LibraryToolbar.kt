package eu.kanade.presentation.library.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.icons.materialsymbols.roundedfilled.VisibilityOff
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    // RK: library-wide tracker refresh
    onClickRefreshTrackers: () -> Unit,
    // RK: opt-in "Update errors" overflow entry (null = hidden)
    onClickUpdateErrors: (() -> Unit)? = null,
    // RK --> DropSauce overflow head: incognito switch and the three sheet / screen shortcuts
    incognito: Boolean = false,
    onToggleIncognito: () -> Unit = {},
    onClickSort: (() -> Unit)? = null,
    onClickDisplay: (() -> Unit)? = null,
    onClickCategories: (() -> Unit)? = null,
    onClickDownloadQueue: (() -> Unit)? = null,
    onClickSettings: (() -> Unit)? = null,
    // RK <--
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomManga = onClickOpenRandomManga,
        // RK: tracker refresh and the opt-in update-errors entry
        onClickRefreshTrackers = onClickRefreshTrackers,
        onClickUpdateErrors = onClickUpdateErrors,
        incognito = incognito,
        onToggleIncognito = onToggleIncognito,
        onClickSort = onClickSort,
        onClickDisplay = onClickDisplay,
        onClickCategories = onClickCategories,
        onClickDownloadQueue = onClickDownloadQueue,
        onClickSettings = onClickSettings,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    // RK: tracker refresh and the opt-in update-errors entry
    onClickRefreshTrackers: () -> Unit,
    onClickUpdateErrors: (() -> Unit)? = null,
    incognito: Boolean,
    onToggleIncognito: () -> Unit,
    onClickSort: (() -> Unit)?,
    onClickDisplay: (() -> Unit)?,
    onClickCategories: (() -> Unit)?,
    onClickDownloadQueue: (() -> Unit)?,
    onClickSettings: (() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    SearchToolbar(
        titleContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false),
                    overflow = TextOverflow.Ellipsis,
                )
                if (title.numberOfManga != null) {
                    Pill(
                        text = "${title.numberOfManga}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                        fontSize = 14.sp,
                    )
                }
            }
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        pillHint = stringResource(MR.strings.action_search_in, title.text), // RK
        onClickSettings = onClickSettings, // RK
        actions = {
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            AppBarActions(
                // RK: listOfNotNull, since the update-errors entry is null when hidden
                listOfNotNull(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = MaterialSymbols.Rounded.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    // RK --> DropSauce overflow head, in the order its menu shows them
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        icon = MaterialSymbols.RoundedFilled.VisibilityOff,
                        checked = incognito,
                        onClick = onToggleIncognito,
                    ),
                    onClickSort?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_sorting_order),
                            icon = MaterialSymbols.AutoMirroredRounded.Sort,
                            hasSubmenu = true,
                            onClick = it,
                        )
                    },
                    onClickDisplay?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_display_options),
                            icon = MaterialSymbols.AutoMirroredRoundedFilled.ViewList,
                            onClick = it,
                        )
                    },
                    onClickCategories?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_manage_categories),
                            icon = MaterialSymbols.AutoMirroredRounded.Label,
                            onClick = it,
                        )
                    },
                    onClickDownloadQueue?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.label_download_queue),
                            icon = MaterialSymbols.Rounded.Download,
                            onClick = it,
                        )
                    },
                    // RK <--
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomManga,
                    ),
                    // RK: pulls fresh score/status for every tracked entry, both content types
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_refresh_trackers),
                        onClick = onClickRefreshTrackers,
                    ),
                    // RK: opt-in entry to the Update errors screen
                    onClickUpdateErrors?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.label_update_errors),
                            onClick = it,
                        )
                    },
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfManga: Int? = null,
)
