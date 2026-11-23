package raito.presentation.download

import eu.kanade.tachiyomi.extension.ExtensionManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import raito.domain.download.SeriesCompletions
import raito.domain.novel.LnSourceIdentity
import raito.domain.novel.NovelPreferences
import raito.domain.novel.model.Novel
import raito.novel.download.NovelDownload
import raito.novel.download.NovelDownloadManager
import raito.novel.source.NovelSourceManager
import raito.presentation.recents.EmittingPreferenceStore
import raito.util.testAppScope

class NovelDownloadQueueProviderTest {

    @Test
    fun `a queued novel from a removed plugin shows the plugin's last name`() = runTest {
        val provider = NovelDownloadQueueProvider(
            downloadManager = mockk<NovelDownloadManager> {
                every { queueState } returns
                    MutableStateFlow(listOf(NovelDownload(novelId = 1L, chapterId = 2L, url = "/c")))
                every { downloadingNovelId } returns MutableStateFlow(null)
                every { completions } returns SeriesCompletions()
                every { isQueueRestored } returns true
            },
            novelRepo = mockk { coEvery { getById(1L) } returns Novel.create().copy(id = 1L, source = "gone") },
            chapterRepo = mockk(),
            sourceManager = NovelSourceManager(
                scope = testAppScope(),
                installer = { mockk(relaxed = true) },
                extensionManager = mockk<ExtensionManager> {
                    every { loadedNovelExtensionsFlow } returns MutableStateFlow(emptyList())
                },
                prefs = NovelPreferences(EmittingPreferenceStore()).apply {
                    seenNovelSources().set(mapOf("gone" to LnSourceIdentity("Removed plugin")))
                },
            ),
        )

        provider.snapshots.first().labels.getValue(1L).sourceName shouldBe "Removed plugin"
    }
}
