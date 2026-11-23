package raito.presentation.browse

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import mihon.domain.extension.model.ContentWarning
import org.junit.jupiter.api.Test
import raito.domain.source.SourceKey
import raito.novel.source.NovelExtensionFormat
import raito.novel.source.NovelExtensionFormat.APK
import raito.novel.source.NovelExtensionFormat.IREADER
import raito.novel.source.NovelSource
import raito.presentation.browse.catalogue.EntryBrowseListing
import raito.presentation.browse.components.sourceDetail
import raito.presentation.browse.feed.FeedEntry
import raito.presentation.browse.feed.FeedState
import raito.presentation.browse.globalsearch.BrowseSearchRow
import raito.presentation.browse.globalsearch.EntrySearchState
import raito.presentation.browse.globalsearch.GlobalSearchEngine
import raito.presentation.browse.migrate.BrowseMigrateRow
import raito.presentation.browse.migrate.MigrateSourcesEngine
import raito.presentation.browse.source.BrowseSourceRow
import raito.presentation.browse.source.NovelSourcesFilterViewModel
import raito.presentation.browse.source.SourcesEngine
import raito.presentation.browse.source.SourcesListItem
import raito.presentation.migrate.flow.EntryMigrationConfigViewModel
import raito.presentation.migrate.flow.MigrationSourceIcon
import raito.presentation.migrate.flow.MigrationSourceUi
import raito.presentation.migrate.flow.loadingStrip
import raito.presentation.migrate.flow.showsFormat

/**
 * Every list that shows novel sources names each one's packaging once it holds two kinds, so two
 * sources with one name (an app and an IReader copy of one site) can be told apart. The feed's
 * add-source dialog passes its whole list straight to the kernel, so it has no case here.
 */
class SourceFormatLabelTest {

    @Test
    fun `a source's line reads its language, then its packaging`() {
        sourceDetail("English", "APK") shouldBe "English • APK"
    }

    @Test
    fun `global search names packaging once two kinds are searched`() {
        GlobalSearchEngine.State(rows = listOf(searchRow(APK), searchRow(IREADER))).showsFormat shouldBe true
    }

    @Test
    fun `global search names none while every source is packaged alike`() {
        GlobalSearchEngine.State(rows = listOf(searchRow(APK), searchRow(APK))).showsFormat shouldBe false
    }

    @Test
    fun `the Sources tab names packaging once it shows two kinds`() {
        SourcesEngine.State(items = listOf(sourceItem(APK), sourceItem(IREADER))).showsFormat shouldBe true
    }

    @Test
    fun `the Sources tab names none while every source is packaged alike`() {
        SourcesEngine.State(items = listOf(sourceItem(APK), sourceItem(APK))).showsFormat shouldBe false
    }

    @Test
    fun `the sources filter names packaging once it lists two kinds`() {
        NovelSourcesFilterViewModel.State.Success(
            items = listOf("en" to listOf(novelSource(APK), novelSource(IREADER))),
            disabledSources = emptySet(),
            disabledLanguages = emptySet(),
        ).showsFormat shouldBe true
    }

    @Test
    fun `the feed names packaging once it holds two kinds`() {
        FeedState(entries = listOf(feedEntry(APK), feedEntry(IREADER))).showsFormat shouldBe true
    }

    @Test
    fun `the migrate list names packaging once it holds two kinds`() {
        MigrateSourcesEngine.State(items = listOf(migrateRow(APK), migrateRow(IREADER))).showsFormat shouldBe true
    }

    @Test
    fun `the migration source picker counts both of its sections`() {
        EntryMigrationConfigViewModel.State(
            selected = listOf(pickerSource(APK)),
            available = listOf(pickerSource(IREADER)),
        ).showsFormat shouldBe true
    }

    @Test
    fun `a migration search names packaging once two kinds are searched`() {
        listOf(pickerSource(APK).loadingStrip(), pickerSource(IREADER).loadingStrip()).showsFormat shouldBe true
    }

    @Test
    fun `a migration search names none while every source is packaged alike`() {
        listOf(pickerSource(APK).loadingStrip(), pickerSource(APK).loadingStrip()).showsFormat shouldBe false
    }

    private fun searchRow(format: NovelExtensionFormat) = BrowseSearchRow(
        key = SourceKey.Novel("s-$format"),
        name = "Site",
        lang = "en",
        isPinned = false,
        state = EntrySearchState.Loading,
        source = Unit,
        format = format,
    )

    private fun sourceItem(format: NovelExtensionFormat) = SourcesListItem.Row(
        BrowseSourceRow(
            key = SourceKey.Novel("s-$format"),
            name = "Site",
            lang = "en",
            isPinned = false,
            isUsedLast = false,
            supportsLatest = true,
            extensionName = "Site",
            contentWarning = ContentWarning.SAFE,
            source = Unit,
            format = format,
        ),
    )

    private fun novelSource(format: NovelExtensionFormat) = mockk<NovelSource> {
        every { this@mockk.format } returns format
    }

    private fun feedEntry(format: NovelExtensionFormat) = FeedEntry(
        feedId = format.ordinal.toLong(),
        savedSearch = null,
        row = searchRow(format),
        sourceName = "Site",
        listing = EntryBrowseListing.Latest,
    )

    private fun migrateRow(format: NovelExtensionFormat) = BrowseMigrateRow(
        key = SourceKey.Novel("s-$format"),
        name = "Site",
        lang = "en",
        count = 1,
        isStub = false,
        source = Unit,
        format = format,
    )

    private fun pickerSource(format: NovelExtensionFormat) = MigrationSourceUi(
        key = "s-$format",
        name = "Site",
        lang = "en",
        icon = MigrationSourceIcon.NovelUrl(null),
        format = format,
    )
}
