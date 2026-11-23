package eu.kanade.tachiyomi.ui.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import raito.domain.category.RecentsSurface
import raito.domain.category.categoryFilterPrefs
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.source.RaitoSourcePreferences
import raito.presentation.library.subscribeAllInCategoryOrder
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.preference.toggle
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.updates.service.UpdatesPreferences

// RK: assisted, since the recents surface the sheet backs comes from the call site
@AssistedInject
class UpdatesSettingsViewModel(
    val updatesPreferences: UpdatesPreferences,
    // RK -->
    // Which surface's sheet this is backing. The filter sheet is shared by every recents surface, and
    // while the combined tab is off Updates and History are two tabs, so each edits its own selection.
    @Assisted private val surface: RecentsSurface,
    private val raitoSourcePreferences: RaitoSourcePreferences,
    private val categoryRepository: CategoryRepository,
    private val raitoLibraryPreferences: RaitoLibraryPreferences,
    // RK <--
) : ViewModel() {

    fun toggleFilter(preference: (UpdatesPreferences) -> Preference<TriState>) {
        preference(updatesPreferences).getAndSet {
            it.next()
        }
    }

    // RK --> backing for the include/exclude category-filter picker. One selection over the whole
    // category table, so the picker lists every row of both libraries, as the library's own picker orders them.
    val categories: StateFlow<List<Category>> = categoryRepository
        .subscribeAllInCategoryOrder(raitoLibraryPreferences)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // The one place this sheet's surface turns into keys. The stored selection is exposed rather than the
    // resolved filter, because the picker shows it even while the toggle is off, which the resolved one clears.
    private val categoryPrefs = raitoSourcePreferences.categoryFilterPrefs(surface)
    val filterCategories: StateFlow<Boolean> = categoryPrefs.first.stateIn(viewModelScope)
    val filterCategoriesInclude: StateFlow<Set<String>> = categoryPrefs.second.stateIn(viewModelScope)
    val filterCategoriesExclude: StateFlow<Set<String>> = categoryPrefs.third.stateIn(viewModelScope)

    // Not surface-scoped like the category prefs: only the combined tab draws the modes this applies
    // to, so there is no second surface to hold a competing value.
    val showRead: StateFlow<Boolean> = raitoSourcePreferences.recentsShowRead.stateIn(viewModelScope)

    val groupBySeries: StateFlow<Boolean> = raitoSourcePreferences.updatesGroupBySeries.stateIn(viewModelScope)

    fun setFilterCategories(enabled: Boolean) {
        categoryPrefs.first.set(enabled)
    }

    fun setCategorySelections(include: Set<Long>, exclude: Set<Long>) {
        categoryPrefs.second.set(include.map(Long::toString).toSet())
        categoryPrefs.third.set(exclude.map(Long::toString).toSet())
    }

    fun toggleShowRead() {
        raitoSourcePreferences.recentsShowRead.toggle()
    }

    fun toggleGroupBySeries() {
        raitoSourcePreferences.updatesGroupBySeries.toggle()
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(surface: RecentsSurface): UpdatesSettingsViewModel
    }
    // RK <--
}
