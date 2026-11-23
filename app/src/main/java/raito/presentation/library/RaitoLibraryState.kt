package raito.presentation.library

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import raito.domain.library.LibraryProgressMode
import raito.domain.library.RaitoLibraryPreferences
import tachiyomi.core.common.preference.Preference

/**
 * Raito's net-new library display state, bundled into one object rather than ~18 loose fields. Fed from
 * [raito.domain.library.RaitoLibraryPreferences] and read through [LibraryEngine.display]: it is
 * library-wide, so it belongs to the engine and not to either content type's model.
 */
@Immutable
data class RaitoLibraryState(
    // Only what the TAB reads. The assembly-side preferences (grouping mode, category sort order,
    // hidden categories, collapsed-at-bottom) feed LibraryEngine's own prefs flow instead; carrying
    // them here too left dead fields a future reader could wrongly trust.
    val collapsedCategories: Set<String> = emptySet(),
    /** Normalized once here, so [raitoIsCollapsed] is a plain lookup per bucket per frame. */
    val collapsedDynamicCategories: Set<String> = emptySet(),
    val showCategoryInTitle: Boolean = false,
    val showAllCategories: Boolean = true,
    val hideHopper: Boolean = false,
    val autohideHopper: Boolean = true,
    val hopperGravity: Int = 1,
    val hopperLongPressAction: HopperLongPressAction? = HopperLongPressAction.SEARCH,
    val trackUpdateErrors: Boolean = false,
    val trackNovelUpdateErrors: Boolean = false,
    /** Gates the bulk Merge action: with grouping off the write is refused, so offering it is a dead tap. */
    val seriesMergingEnabled: Boolean = true,
    val progressMode: LibraryProgressMode = LibraryProgressMode.OFF,
)

/**
 * The whole library's display config, Raito's own preferences plus the two Mihon ones the shared tab
 * reads. One holder for the library rather than one per content type, because none of these settings is
 * per-type: the chip changes what is listed, not how it is drawn.
 */
@Immutable
data class LibraryDisplayState(
    val raito: RaitoLibraryState = RaitoLibraryState(),
    val showCategoryTabs: Boolean = false,
    val showItemCounts: Boolean = false,
)

/** Every Raito library display preference, folded into one reactive state. */
@Suppress("UNCHECKED_CAST")
fun RaitoLibraryPreferences.libraryStateFlow(): Flow<RaitoLibraryState> = combine(
    collapsedCategories.changes(),
    collapsedDynamicCategories.changes(),
    showCategoryInTitle.changes(),
    showAllCategories.changes(),
    hideHopper.changes(),
    autohideHopper.changes(),
    hopperGravity.changes(),
    hopperLongPressAction.changes(),
    trackUpdateErrors.changes(),
    trackNovelUpdateErrors.changes(),
    seriesMergingEnabled.changes(),
    progressBadge.changes(),
) {
    RaitoLibraryState(
        collapsedCategories = it[0] as Set<String>,
        collapsedDynamicCategories = (it[1] as Set<String>).normalizedDynamicKeys(),
        showCategoryInTitle = it[2] as Boolean,
        showAllCategories = it[3] as Boolean,
        hideHopper = it[4] as Boolean,
        autohideHopper = it[5] as Boolean,
        hopperGravity = it[6] as Int,
        hopperLongPressAction = HopperLongPressAction.fromCode(it[7] as Int),
        trackUpdateErrors = it[8] as Boolean,
        trackNovelUpdateErrors = it[9] as Boolean,
        seriesMergingEnabled = it[10] as Boolean,
        progressMode = it[11] as LibraryProgressMode,
    )
}

/** Collapse or expand one real category, keyed by its header key. */
fun RaitoLibraryPreferences.toggleCategoryCollapsed(headerKey: String) {
    collapsedCategories.toggle(headerKey)
}

/** Collapse or expand one dynamic group, keyed by its normalized key. Removal matches by normalized
 *  form so entries persisted before normalization still toggle off. */
fun RaitoLibraryPreferences.toggleDynamicCategoryCollapsed(headerKey: String) {
    val current = collapsedDynamicCategories.get()
    val equivalent = current.filterTo(HashSet()) { normalizeDynamicKey(it) == headerKey }
    collapsedDynamicCategories.set(if (equivalent.isNotEmpty()) current - equivalent else current + headerKey)
}

/**
 * Toggle every displayed bucket collapsed or expanded (the hopper long-press), across both the real
 * categories and the dynamic groups, which are collapsed through separate preferences.
 */
fun RaitoLibraryPreferences.toggleAllCategoriesCollapsed(buckets: List<LibraryBucket>) {
    val defaultKeys = buckets.filterIsInstance<LibraryBucket.Real>().mapTo(HashSet()) { it.key }
    val dynamicKeys = buckets.filterIsInstance<LibraryBucket.Dynamic>().mapTo(HashSet()) { it.key }
    // Dynamic keys compare and clear by normalized form, so pre-normalization entries count as
    // collapsed and expand-all actually removes them.
    val storedDynamic = collapsedDynamicCategories.get()
    val allCollapsed = collapsedCategories.get().containsAll(defaultKeys) &&
        storedDynamic.normalizedDynamicKeys().containsAll(dynamicKeys)
    if (allCollapsed) {
        collapsedCategories.set(collapsedCategories.get() - defaultKeys)
        collapsedDynamicCategories.set(
            storedDynamic.filterNotTo(HashSet()) { normalizeDynamicKey(it) in dynamicKeys },
        )
    } else {
        collapsedCategories.set(collapsedCategories.get() + defaultKeys)
        collapsedDynamicCategories.set(storedDynamic + dynamicKeys)
    }
}

private fun Preference<Set<String>>.toggle(key: String) {
    val current = get()
    set(if (key in current) current - key else current + key)
}
