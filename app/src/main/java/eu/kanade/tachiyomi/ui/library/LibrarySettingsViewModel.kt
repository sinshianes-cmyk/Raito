package eu.kanade.tachiyomi.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import raito.domain.library.CategorySortOrder
import raito.domain.library.RaitoLibraryPreferences
import raito.presentation.library.HopperLongPressAction
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>()) // RK: Raito's own graph port, same binding
class LibrarySettingsViewModel(
    val preferences: BasePreferences,
    val libraryPreferences: LibraryPreferences,
    // RK -->
    val raitoLibraryPreferences: RaitoLibraryPreferences,
    // RK <--
    private val setDisplayMode: SetDisplayMode,
    trackerManager: TrackerManager,
) : ViewModel() {

    val trackersFlow = trackerManager.loggedInTrackersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = trackerManager.loggedInTrackers(),
        )

    // RK: toggleFilter and toggleTracker moved to LibraryEngine's LibrarySettingsBinding, as below

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setDisplayMode.await(mode)
    }

    // RK --> Raito settings-sheet actions still on the Display tab. The filter, sort and group actions
    // that used to live here moved to LibraryEngine's LibrarySettingsBinding, which the one shared
    // sheet drives, so upstream's own setSort went with them.

    fun setCategorySortOrder(value: CategorySortOrder) {
        raitoLibraryPreferences.categorySortOrder.set(value)
    }

    fun setHopperLongPressAction(action: HopperLongPressAction) {
        raitoLibraryPreferences.hopperLongPressAction.set(action.code)
    }
    // RK <--
}
