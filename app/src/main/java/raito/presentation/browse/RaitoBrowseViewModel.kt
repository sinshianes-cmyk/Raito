package raito.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import raito.domain.extension.ExtensionUpdateCounts
import raito.domain.library.ContentType
import raito.domain.source.RaitoSourcePreferences
import raito.domain.suggestions.SuggestionsPreferences
import raito.domain.suggestions.SuggestionsRepository
import raito.domain.suggestions.SuggestionsState
import raito.domain.recommendation.RelatedMangaCandidate
import raito.domain.recommendation.localIdOf
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import raito.novel.update.LnPluginUpdateChecker
import tachiyomi.core.common.util.lang.launchIO
import kotlin.time.Duration.Companion.seconds

/**
 * Browse-level state shared by the Raito Sources, Extensions and Migration tab wrappers: the sticky
 * content-type filter (one key, so the tabs stay in sync), the Browse search query, and the
 * extension update counts that feed the Extensions tab badges. Kicks the cache-gated plugin update
 * check on Browse open so the badge is fresh without the user opening the Novels chip.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class RaitoBrowseViewModel(
    private val sourcePreferences: RaitoSourcePreferences,
    updateCounts: ExtensionUpdateCounts,
    updateChecker: LnPluginUpdateChecker,
    private val suggestionsRepository: SuggestionsRepository,
    private val suggestionsPreferences: SuggestionsPreferences,
    private val networkToLocalManga: NetworkToLocalManga,
) : ViewModel() {

    val contentType: StateFlow<ContentType> = sourcePreferences.browseContentType.stateIn(viewModelScope)

    /** Whether the Feed tab is shown at all, and whether it leads the tab row. */
    val showFeedTab: StateFlow<Boolean> = sourcePreferences.showFeedTab.stateIn(viewModelScope)
    val feedTabInFront: StateFlow<Boolean> = sourcePreferences.feedTabInFront.stateIn(viewModelScope)

    val exploreGrid: StateFlow<Boolean> = sourcePreferences.exploreGrid.stateIn(viewModelScope)
    val exploreNovels: StateFlow<Boolean> = sourcePreferences.exploreNovels.stateIn(viewModelScope)
    val exploreColumns: StateFlow<Int> = sourcePreferences.exploreGridColumns.stateIn(viewModelScope)

    val novelUpdatesCount: StateFlow<Int> = updateCounts.novel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), 0)

    val totalUpdatesCount: StateFlow<Int> = updateCounts.total
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), 0)

    /**
     * The Browse search bar's query. It lives here rather than on either content type's model
     * because the bar sits above the tabs and filters one list serving both.
     */
    val searchQuery: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    /** The Services switch for the suggestions row; the row is absent from Browse while it is off. */
    val suggestionsEnabled: StateFlow<Boolean> = suggestionsPreferences.enabled.stateIn(viewModelScope)
    val suggestions: StateFlow<SuggestionsState> = suggestionsRepository.state

    init {
        viewModelScope.launchIO { updateChecker.runIfStale() }
        viewModelScope.launchIO {
            suggestionsEnabled.collect { on ->
                if (on) suggestionsRepository.refresh() else suggestionsRepository.clear()
            }
        }
    }

    init {
        // Flipping "hide titles in my library" rebuilds the row so it matches the switch at once.
        viewModelScope.launchIO {
            kotlinx.coroutines.flow.merge(
                suggestionsPreferences.hideInLibrary.changes(),
                suggestionsPreferences.hideNsfw.changes(),
            ).drop(2).collect {
                if (suggestionsPreferences.enabled.get()) suggestionsRepository.refresh(force = true)
            }
        }
    }

    fun refreshSuggestions() {
        viewModelScope.launchIO { suggestionsRepository.refresh(force = true) }
    }

    /** The stored manga id a tapped suggestion opens, or null when it has to go through search. */
    suspend fun resolveSuggestion(candidate: RelatedMangaCandidate): Long? = networkToLocalManga.localIdOf(candidate)

    fun setContentType(type: ContentType) {
        sourcePreferences.browseContentType.set(type)
    }

    fun setExploreGrid(grid: Boolean) {
        sourcePreferences.exploreGrid.set(grid)
    }

    fun setExploreNovels(novels: Boolean) {
        sourcePreferences.exploreNovels.set(novels)
    }

    fun setExploreColumns(columns: Int) {
        sourcePreferences.exploreGridColumns.set(columns)
    }

    fun search(query: String?) {
        searchQuery.update { query }
    }
}
