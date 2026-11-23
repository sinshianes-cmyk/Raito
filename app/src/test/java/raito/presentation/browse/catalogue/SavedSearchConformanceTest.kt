package raito.presentation.browse.catalogue

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel
import exh.source.ExhPreferences
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import raito.domain.novel.FavoritedNovels
import raito.domain.source.RaitoSourcePreferences
import raito.novel.source.NovelFilterState
import raito.novel.source.NovelFilters
import raito.novel.source.NovelSource
import raito.novel.source.NovelSourceManager
import raito.presentation.browse.BulkFavoriteViewModel
import raito.presentation.browse.EntryBulkFavoriteViewModel
import raito.presentation.migrate.flow.MigrationPickHandoff
import raito.presentation.novel.browse.NovelBrowseViewModel
import raito.presentation.novel.browse.NovelBulkFavoriteViewModel
import raito.presentation.recents.EmittingPreferenceStore
import raito.util.testAppScope
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * A saved search, once applied, is what the filter sheet holds, on both catalogues: otherwise the
 * sheet shows the filters from before, its next Filter tap throws the saved ones away, and saving
 * again saves the stale ones. Run through each type's own adapter, over a filter set both share.
 */
class SavedSearchConformanceTest {

    // viewModelScope is Main-based, so both models need a Main the test controls.
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @ParameterizedTest(name = "{0}")
    @MethodSource("probes")
    fun `an applied saved search is what the filter sheet holds`(probe: SavedSearchProbe) = runTest {
        val behavior = probe.open()
        probe.applyFromSheet("saved")
        val saved = behavior.captureSearch().filtersJson
        probe.applyFromSheet("changed since")

        behavior.applySearch(null, saved)

        behavior.captureSearch().filtersJson shouldBe saved
    }

    companion object {
        @JvmStatic
        fun probes() = listOf(MangaSavedSearchProbe(), NovelSavedSearchProbe())
    }
}

/** A text filter built fresh per call, as a source builds its list. */
private class AuthorFilter : Filter.Text("Author")

/** One catalogue, opened through its adapter, whose filter sheet the test can fill in and apply. */
interface SavedSearchProbe {
    suspend fun open(): EntryBrowseBehavior

    /** What typing [author] into the sheet and tapping its Filter button does. */
    fun applyFromSheet(author: String)
}

private class MangaSavedSearchProbe : SavedSearchProbe {

    override fun toString() = "manga"

    private lateinit var model: BrowseSourceViewModel

    override suspend fun open(): EntryBrowseBehavior {
        val store = EmittingPreferenceStore()
        val source = mockk<CatalogueSource>(relaxed = true) {
            every { id } returns 1L
            every { getFilterList() } answers { FilterList(AuthorFilter()) }
        }
        model = BrowseSourceViewModel(
            appScope = testAppScope(),
            sourceId = 1L,
            listingQuery = mangaListingQuery(startLatest = false, initialQuery = null),
            sourceManager = mockk(relaxed = true) { coEvery { getOrStub(1L) } returns source },
            sourcePreferences = SourcePreferences(store),
            libraryPreferences = LibraryPreferences(store),
            getRemoteManga = mockk(relaxed = true),
            getManga = mockk(relaxed = true),
            getIncognitoState = mockk(relaxed = true),
            raitoSourcePreferences = RaitoSourcePreferences(store),
            mangaLibraryAdder = mockk(relaxed = true),
            getFlatMetadataById = mockk(relaxed = true),
            exhPreferences = ExhPreferences(store),
        )
        // The source resolves on Dispatchers.IO, which the test scheduler cannot advance.
        model.state.first { it.source != null }
        val bulk = mockk<BulkFavoriteViewModel> {
            every { state } returns MutableStateFlow(EntryBulkFavoriteViewModel.State())
        }
        return MangaBrowseAdapter(model, bulk)
    }

    override fun applyFromSheet(author: String) {
        val filters = model.state.value.filters
        (filters.single() as Filter.Text).state = author
        model.setFilters(filters)
        model.search(filters = model.state.value.filters)
    }
}

private class NovelSavedSearchProbe : SavedSearchProbe {

    override fun toString() = "novels"

    private lateinit var model: NovelBrowseViewModel

    override suspend fun open(): EntryBrowseBehavior {
        val store = EmittingPreferenceStore()
        val source = mockk<NovelSource>(relaxed = true) {
            every { id } returns SOURCE_ID
            every { filters } returns NovelFilters.FilterListSchema { FilterList(AuthorFilter()) }
        }
        // Stubbed off the mock: `get` inside a mockk block binds to MockK's own dynamic-call helper.
        val manager = mockk<NovelSourceManager>(relaxed = true)
        coEvery { manager.get(SOURCE_ID) } returns source
        model = NovelBrowseViewModel(
            appScope = testAppScope(),
            sourceId = SOURCE_ID,
            initialQuery = "",
            startLatest = false,
            installer = mockk(relaxed = true),
            manager = manager,
            novelRepository = mockk(relaxed = true) {
                every { getFavoritedKeysAsFlow() } returns flowOf(FavoritedNovels.None)
            },
            libraryAdder = mockk(relaxed = true),
            pickHandoff = MigrationPickHandoff(),
            raitoSourcePreferences = RaitoSourcePreferences(store),
            sourcePreferences = SourcePreferences(store),
            getIncognitoState = mockk(relaxed = true),
            libraryPreferences = LibraryPreferences(store),
        )
        // The plugin resolves on Dispatchers.IO, which the test scheduler cannot advance.
        model.state.first { it.source != null }
        val bulk = mockk<NovelBulkFavoriteViewModel> {
            every { state } returns MutableStateFlow(EntryBulkFavoriteViewModel.State())
        }
        return NovelBrowseAdapter(model, bulk, SOURCE_ID)
    }

    override fun applyFromSheet(author: String) {
        val filters = (model.state.value.filterDraft as NovelFilterState.Filters).list
        (filters.single() as Filter.Text).state = author
        model.setFilters(filters)
        model.applyFilters()
    }

    private companion object {
        const val SOURCE_ID = "src"
    }
}
