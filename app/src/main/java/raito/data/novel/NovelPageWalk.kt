package raito.data.novel

import raito.domain.chapter.ChapterNumberOverrideRepository
import raito.domain.novel.NovelChapterRepository
import raito.domain.novel.NovelRepository
import raito.domain.novel.model.Novel
import raito.novel.download.NovelDownloadManager
import raito.novel.source.NovelSource
import raito.util.runCatchingCancellable
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * Walk a paged source's chapter pages [fromPage]..[toPage] inclusive, syncing each into the novel's
 * stored chapters (tagged with its transport index). Callers pass `fromPage = oldTotalPages` so the
 * previously-last page is re-fetched too, surfacing chapters appended to it before a new page opened.
 * Page 1 is the caller's responsibility, coming from `parseNovel` rather than `parsePage`. A page that
 * throws or returns nothing is skipped rather than fatal, so one flaky page cannot abort the rest.
 * Returns the pages' sync results added together.
 */
suspend fun walkNovelPages(
    novel: Novel,
    source: NovelSource,
    fromPage: Long,
    toPage: Long,
    novelChapterRepository: NovelChapterRepository,
    novelRepository: NovelRepository,
    libraryPreferences: LibraryPreferences,
    numberOverrides: ChapterNumberOverrideRepository,
    novelDownloadManager: NovelDownloadManager? = null,
): NovelChapterSyncResult {
    var walked = NovelChapterSyncResult.UNCHANGED
    if (toPage <= 1L) return walked
    for (p in maxOf(fromPage, 1L)..toPage) {
        val key = p.toString()
        val chapters = runCatchingCancellable { source.parsePage(novel.url, key)?.chapters }.getOrNull().orEmpty()
        if (chapters.isNotEmpty()) {
            runCatchingCancellable {
                syncChaptersWithNovelSource(
                    chapters,
                    novel,
                    novelChapterRepository,
                    novelRepository,
                    libraryPreferences,
                    numberOverrides,
                    page = key,
                    novelDownloadManager = novelDownloadManager,
                )
            }.onSuccess { walked += it }
        }
    }
    return walked
}
