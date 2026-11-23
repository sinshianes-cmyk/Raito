package raito.presentation.library

import androidx.compose.foundation.layout.ColumnScope
import tachiyomi.presentation.core.components.TonalFilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import raito.domain.library.CategorySortOrder
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Raito's category and hopper settings, under a "Categories" heading at the bottom of the Display tab
 * ([EntryDisplayPage]). They are library-wide rather than per-content-type, which is why they hang off
 * the shared display model rather than off a [LibrarySettingsBinding].
 *
 * Follows Mihon's settings idiom (preferences read in the composable via `collectAsState`), consistent
 * with the rest of the settings sheet.
 */
@Composable
fun ColumnScope.RaitoCategoriesPage(viewModel: LibrarySettingsViewModel) {
    HeadingItem(MR.strings.categories)
    val categorySortOrder by viewModel.raitoLibraryPreferences.categorySortOrder.collectAsState()
    SettingsChipRow(MR.strings.pref_category_sort_order) {
        CategorySortOrder.entries.forEach { order ->
            TonalFilterChip(
                selected = categorySortOrder == order,
                onClick = { viewModel.setCategorySortOrder(order) },
                label = { Text(stringResource(order.titleRes)) },
            )
        }
    }
    CheckboxItem(
        label = stringResource(MR.strings.always_show_current_category),
        pref = viewModel.raitoLibraryPreferences.showCategoryInTitle,
    )
    CheckboxItem(
        label = stringResource(MR.strings.move_dynamic_to_bottom),
        pref = viewModel.raitoLibraryPreferences.collapsedDynamicAtBottom,
    )
    CheckboxItem(
        label = stringResource(MR.strings.show_hidden_categories),
        pref = viewModel.raitoLibraryPreferences.showHiddenCategories,
    )
    CheckboxItem(
        label = stringResource(MR.strings.hide_category_hopper),
        pref = viewModel.raitoLibraryPreferences.hideHopper,
    )
    CheckboxItem(
        label = stringResource(MR.strings.autohide_category_hopper),
        pref = viewModel.raitoLibraryPreferences.autohideHopper,
    )
    val hopperLongPress by viewModel.raitoLibraryPreferences.hopperLongPressAction.collectAsState()
    SettingsChipRow(MR.strings.hopper_long_press) {
        HopperLongPressAction.entries.forEach { action ->
            TonalFilterChip(
                selected = hopperLongPress == action.code,
                onClick = { viewModel.setHopperLongPressAction(action) },
                label = { Text(stringResource(action.labelRes)) },
            )
        }
    }
}
