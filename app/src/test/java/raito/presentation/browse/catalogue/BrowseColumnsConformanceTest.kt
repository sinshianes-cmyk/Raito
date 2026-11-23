package raito.presentation.browse.catalogue

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueSource
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

/** Both catalogues lay their grid out in the library's column counts, read by their models. */
class BrowseColumnsConformanceTest {

    // viewModelScope is Main-based, so both models need a Main the test controls.
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @ParameterizedTest(name = "{0}")
    @MethodSource("probes")
    fun `a catalogue follows the library's column counts`(probe: ColumnsProbe) = runTest {
        val store = EmittingPreferenceStore()
        LibraryPreferences(store).portraitColumns.set(3)
        LibraryPreferences(store).landscapeColumns.set(5)

        probe.open(store).columnsWhen { it.portrait == 3 } shouldBe BrowseColumns(portrait = 3, landscape = 5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("probes")
    fun `a column count changed while the catalogue is open reaches its grid`(probe: ColumnsProbe) = runTest {
        val store = EmittingPreferenceStore()
        val behavior = probe.open(store)
        behavior.columnsWhen { true }

        LibraryPreferences(store).portraitColumns.set(4)

        behavior.columnsWhen { it.portrait == 4 }.portrait shouldBe 4
    }

    private suspend fun EntryBrowseBehavior.columnsWhen(predicate: (BrowseColumns) -> Boolean): BrowseColumns =
        state.first { state ->
            val style = (state as? EntryBrowseScreenState.Loaded)?.rowStyle as? EntryBrowseRowStyle.Standard
            style != null && predicate(style.columns)
        }.let { ((it as EntryBrowseScreenState.Loaded).rowStyle as EntryBrowseRowStyle.Standard).columns }

    companion object {
        @JvmStatic
        fun probes() = listOf(MangaColumnsProbe(), NovelColumnsProbe())
    }
}

/** One catalogue opened over [EmittingPreferenceStore], through its own adapter. */
interface ColumnsProbe {
    fun open(store: EmittingPreferenceStore): EntryBrowseBehavior
}

private class MangaColumnsProbe : ColumnsProbe {

    override fun toString() = "manga"

    override fun open(store: EmittingPreferenceStore): EntryBrowseBehavior {
        val source = mockk<CatalogueSource>(relaxed = true) {
            every { id } returns 1L
            every { getFilterList() } returns FilterList()
        }
        val model = BrowseSourceViewModel(
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
        val bulk = mockk<BulkFavoriteViewModel> {
            every { state } returns MutableStateFlow(EntryBulkFavoriteViewModel.State())
        }
        return MangaBrowseAdapter(model, bulk)
    }
}

private class NovelColumnsProbe : ColumnsProbe {

    override fun toString() = "novels"

    override fun open(store: EmittingPreferenceStore): EntryBrowseBehavior {
        val source = mockk<NovelSource>(relaxed = true) { every { id } returns "src" }
        // Stubbed off the mock: `get` inside a mockk block binds to MockK's own dynamic-call helper.
        val manager = mockk<NovelSourceManager>(relaxed = true)
        coEvery { manager.get("src") } returns source
        val model = NovelBrowseViewModel(
            appScope = testAppScope(),
            sourceId = "src",
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
        val bulk = mockk<NovelBulkFavoriteViewModel> {
            every { state } returns MutableStateFlow(EntryBulkFavoriteViewModel.State())
        }
        return NovelBrowseAdapter(model, bulk, "src")
    }
}
