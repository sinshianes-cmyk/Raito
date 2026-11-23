package raito.presentation.stats.details

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import raito.domain.entry.EntryCustomInfo
import raito.domain.entry.EntryId
import raito.domain.entry.GetEntryCustomInfo
import raito.domain.library.ContentType
import raito.domain.library.includes
import raito.domain.library.librarySortComparator
import raito.domain.library.toSortMode
import raito.domain.novel.NovelRepository
import raito.presentation.library.MangaLibraryRows
import raito.presentation.library.libraryTrackerMeans
import raito.presentation.library.memberIds
import raito.presentation.library.mixedLibraryItemSortFields
import raito.presentation.library.novels.NovelLibraryRows
import raito.presentation.stats.ReadState
import raito.presentation.stats.StatsGroupKey
import raito.presentation.stats.StatsLibrary
import raito.presentation.stats.StatsLibraryLoader
import raito.presentation.stats.StatsRow
import raito.presentation.stats.StatsSort
import raito.presentation.stats.dimension
import raito.presentation.stats.inReadState
import raito.presentation.stats.readState
import raito.presentation.stats.rows
import raito.presentation.stats.sortedForDisplay
import raito.presentation.stats.standingFor
import raito.presentation.stats.statsDetailsScreen
import raito.presentation.stats.titleOf
import raito.presentation.stats.titlesIn
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences

/** The row's summary card, the read state it is narrowed to, and its series. */
@Immutable
data class StatsSeriesState(val summary: StatsRow?, val readState: ReadState?, val rows: List<LibraryItem>)

/**
 * The series one Statistics row counts, as the library draws them: the library's own rows, before its
 * filters and search, in the library's display mode, columns and global sort, with its continue-reading
 * button when that is on. Its filters do not apply, or the list would hold fewer series than the row counts.
 */
@AssistedInject
class StatsSeriesViewModel(
    @Assisted private val key: StatsGroupKey,
    @Assisted private val contentType: ContentType,
    private val loader: StatsLibraryLoader,
    private val mangaLibraryRows: MangaLibraryRows,
    private val novelLibraryRows: NovelLibraryRows,
    getEntryCustomInfo: GetEntryCustomInfo,
    private val novelRepository: NovelRepository,
    private val libraryPreferences: LibraryPreferences,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(key: StatsGroupKey, contentType: ContentType): StatsSeriesViewModel
    }

    private val readState = MutableStateFlow<ReadState?>(null)

    private val rowsOfRow = combine(
        flow { emit(loader.load()) },
        // A type the row does not cover is never built, so it waits on nothing.
        if (contentType.includes(ContentType.MANGA)) mangaLibraryRows.rows() else flowOf(emptyList()),
        if (contentType.includes(ContentType.NOVELS)) novelLibraryRows.rows() else flowOf(emptyList()),
        getEntryCustomInfo.subscribeAll(),
    ) { library, manga, novels, customInfo ->
        val timeZone = TimeZone.currentSystemDefault()
        val titles = library.titles(contentType).titlesIn(key, timeZone)
        val readStates = titles.associate { it.entryId to it.readState }
        // Raw rows: the library sorts by source values and overlays Edit info only at its display read.
        val rows = (manga + novels).standingFor(readStates.keys, library::titleOf)
        val summary = library.rows(key.dimension, contentType, StatsSort.COUNT, timeZone).firstOrNull {
            it.group.key ==
                key
        }
        Loaded(library, rows, customInfo, readStates, summary)
    }

    /** Null while the library loads. */
    val state: StateFlow<StatsSeriesState?> = combine(
        rowsOfRow,
        libraryPreferences.sortingMode.changes(),
        libraryPreferences.randomSortSeed.changes(),
        readState,
    ) { loaded, sort, seed, readState ->
        // Lazy, so only the tracker-score sort pays for the means, as in the library.
        val means = lazy { loaded.library.trackerMeans(loaded.rows) }
        val fields = mixedLibraryItemSortFields { item -> means.value[item.entryId] }
        val comparator = librarySortComparator(sort.type.toSortMode(), sort.isAscending, seed.toLong(), fields)
        StatsSeriesState(
            summary = loaded.summary,
            readState = readState,
            rows = loaded.rows
                .inReadState(readState) { loaded.library.titleOf(it)?.let(loaded.readStates::get) }
                .sortedForDisplay(comparator, loaded.customInfo),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setReadState(state: ReadState?) {
        readState.value = state
    }

    val showContinueReading: StateFlow<Boolean> = libraryPreferences.showContinueReadingButton.changes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, libraryPreferences.showContinueReadingButton.get())

    fun displayMode(): PreferenceMutableState<LibraryDisplayMode> =
        libraryPreferences.displayMode.asState(viewModelScope)

    fun columnsForOrientation(isLandscape: Boolean): PreferenceMutableState<Int> =
        (if (isLandscape) libraryPreferences.landscapeColumns else libraryPreferences.portraitColumns)
            .asState(viewModelScope)

    suspend fun detailsScreen(entryId: EntryId): Screen? = statsDetailsScreen(entryId, novelRepository)

    /** The reader at the row's next chapter, as the library's continue-reading opens it; null when none. */
    suspend fun resumeIntent(context: Context, item: LibraryItem): Intent? = when (item.entryId) {
        is EntryId.Manga -> mangaLibraryRows.nextUnreadChapter(item.libraryManga.manga)
            ?.let { ReaderActivity.newIntent(context, it.mangaId, it.id) }
        is EntryId.Novel -> novelLibraryRows.resumeChapter(item.id)
            ?.let { ReaderActivity.newNovelIntent(context, it.novelId, it.id) }
    }

    private class Loaded(
        val library: StatsLibrary,
        val rows: List<LibraryItem>,
        val customInfo: Map<EntryId, EntryCustomInfo>,
        val readStates: Map<EntryId, ReadState>,
        val summary: StatsRow?,
    )

    // Each type's means keyed by its own row ids, as each library provider computes them.
    private fun StatsLibrary.trackerMeans(rows: List<LibraryItem>): Map<EntryId, Double> {
        val trackers = loggedInTrackers.associateBy { it.id }
        val (mangaRows, novelRows) = rows.partition { it.entryId is EntryId.Manga }
        val manga = libraryTrackerMeans(mangaRows.associate { it.id to it.memberIds() }, mangaTracks, trackers)
        val novels = libraryTrackerMeans(novelRows.associate { it.id to it.memberIds() }, novelTracks, trackers)
        return manga.mapKeys { EntryId.Manga(it.key) } + novels.mapKeys { EntryId.Novel(it.key) }
    }
}
