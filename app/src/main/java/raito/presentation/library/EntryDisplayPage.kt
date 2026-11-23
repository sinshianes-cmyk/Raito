package raito.presentation.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import tachiyomi.presentation.core.components.TonalFilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import raito.domain.library.LibraryProgressMode
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private val displayModes = listOf(
    MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
    MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    // Order matches Komikku (List before Cover-only, panorama last) so the panorama chip wraps to
    // the end of the row instead of wedging into the middle.
    MR.strings.action_display_list to LibraryDisplayMode.List,
    MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    MR.strings.action_display_comfortable_grid_panorama to LibraryDisplayMode.ComfortableGridPanorama,
)

/**
 * The single library-settings Display tab for both manga and novels: display mode, columns, badges,
 * continue-reading, the tabs section, and the Raito category/hopper settings. The one per-type
 * divergence is a parameter, so the tab can't drift: [showLocalBadge] gates the manga-only local badge.
 */
@Composable
fun ColumnScope.EntryDisplayPage(
    viewModel: LibrarySettingsViewModel,
    showLocalBadge: Boolean,
) {
    val displayMode by viewModel.libraryPreferences.displayMode.collectAsState()
    SettingsChipRow(MR.strings.action_display_mode) {
        displayModes.map { (titleRes, mode) ->
            TonalFilterChip(
                selected = displayMode == mode,
                onClick = { viewModel.setDisplayMode(mode) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }

    if (displayMode != LibraryDisplayMode.List) {
        val configuration = LocalConfiguration.current
        val columnPreference = remember {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                viewModel.libraryPreferences.landscapeColumns
            } else {
                viewModel.libraryPreferences.portraitColumns
            }
        }

        val columns by columnPreference.collectAsState()
        SliderItem(
            value = columns,
            valueRange = 0..10,
            label = stringResource(MR.strings.pref_library_columns),
            valueString = if (columns > 0) {
                columns.toString()
            } else {
                stringResource(MR.strings.label_auto)
            },
            onChange = columnPreference::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }

    HeadingItem(MR.strings.overlay_header)
    CheckboxItem(
        label = stringResource(MR.strings.action_display_download_badge),
        pref = viewModel.libraryPreferences.downloadBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_unread_badge),
        pref = viewModel.libraryPreferences.unreadBadge,
    )
    if (showLocalBadge) {
        CheckboxItem(
            label = stringResource(MR.strings.action_display_local_badge),
            pref = viewModel.libraryPreferences.localBadge,
        )
    }
    CheckboxItem(
        label = stringResource(MR.strings.action_display_language_badge),
        pref = viewModel.libraryPreferences.languageBadge,
    )
    val progressMode by viewModel.raitoLibraryPreferences.progressBadge.collectAsState()
    SettingsChipRow(MR.strings.action_display_progress_badge) {
        LibraryProgressMode.entries.forEach { mode ->
            TonalFilterChip(
                selected = progressMode == mode,
                onClick = { viewModel.raitoLibraryPreferences.progressBadge.set(mode) },
                label = { Text(stringResource(mode.titleRes)) },
            )
        }
    }
    CheckboxItem(
        label = stringResource(MR.strings.action_display_source_badge),
        pref = viewModel.raitoLibraryPreferences.sourceBadge,
    )
    // Master switch (also in Settings); the same-title suggestion moved there too.
    CheckboxItem(
        label = stringResource(MR.strings.action_series_merging),
        pref = viewModel.raitoLibraryPreferences.seriesMergingEnabled,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_merge_source_icons),
        pref = viewModel.raitoLibraryPreferences.showMergeSourceIcons,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_continue_reading_button),
        pref = viewModel.libraryPreferences.showContinueReadingButton,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_tap_to_read),
        pref = viewModel.libraryPreferences.tapToRead,
    )

    HeadingItem(MR.strings.tabs_header)
    // Single-list (show-all) view toggle; off keeps Mihon's swipeable pager.
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_all_categories),
        pref = viewModel.raitoLibraryPreferences.showAllCategories,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_tabs),
        pref = viewModel.libraryPreferences.categoryTabs,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_number_of_items),
        pref = viewModel.libraryPreferences.categoryNumberOfItems,
    )

    RaitoCategoriesPage(viewModel = viewModel)
}
