package raito.presentation.browse.feed

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import raito.domain.novel.FavoritedNovels
import raito.domain.source.CatalogueEnd
import raito.domain.source.SourceKey
import raito.domain.source.model.SavedSearch
import raito.novel.host.NovelItem
import raito.novel.source.NovelFilterState
import raito.novel.source.NovelFilters
import raito.novel.source.NovelItemsPage
import raito.novel.source.NovelListing
import raito.novel.source.NovelSource
import raito.presentation.browse.globalsearch.BrowseSearchRow
import raito.presentation.browse.globalsearch.EntrySearchState
import raito.presentation.browse.item

/**
 * A feed row holding a saved search shows what that search's catalogue shows. An LNReader plugin's
 * filters-only search browsed Latest in the feed and Popular in the catalogue it opens, and several
 * plugins drop their filters on Latest, so the row showed the plain Latest list.
 */
class NovelFeedProviderTest {

    private val source = mockk<NovelSource>(relaxed = true) {
        every { id } returns "plugin"
        every { supportsLatest } returns true
        every { filters } returns NovelFilters.LnSchema(JsonObject(emptyMap()))
        coEvery { browse(any(), any(), any()) } answers {
            NovelItemsPage(listOf(NovelItem(firstArg<NovelListing>().name, "/x", null)), CatalogueEnd.Reported(false))
        }
    }

    private val row = BrowseSearchRow(
        key = SourceKey.Novel("plugin"),
        name = "Plugin",
        lang = "en",
        isPinned = false,
        state = EntrySearchState.Loading,
        source = source,
    )

    @Test
    fun `a filters-only plugin saved search pages the Popular listing its catalogue pages`() = runTest {
        val saved = SavedSearch(1L, SourceKey.Novel("plugin"), "Saved", query = null, filtersJson = null)

        val shown = NovelFeedProvider(mockk(), mockk(), MutableStateFlow(FavoritedNovels.None)).load(row, saved)

        shown.map { it.item.name } shouldBe listOf(NovelListing.Popular.name)
    }

    @Test
    fun `a saved search's filters reach the plugin`() = runTest {
        val sent = slot<NovelFilterState?>()
        coEvery { source.browse(any(), any(), captureNullable(sent)) } returns
            NovelItemsPage(emptyList(), CatalogueEnd.Reported(false))
        val saved = SavedSearch(1L, SourceKey.Novel("plugin"), "Saved", query = null, filtersJson = """{"genre":"x"}""")

        NovelFeedProvider(mockk(), mockk(), MutableStateFlow(FavoritedNovels.None)).load(row, saved)

        (sent.captured as NovelFilterState.LnValues).values["genre"] shouldBe JsonPrimitive("x")
    }
}
