package raito.domain.novel.interactor

import dev.zacsweers.metro.Inject
import raito.domain.download.NovelRemovableDownloads
import raito.domain.merge.ChapterUnit
import raito.domain.merge.expandToUnits
import raito.domain.novel.NovelChapterRepository
import raito.domain.novel.NovelPreferences
import raito.domain.reader.chapterToDeleteBehind
import raito.domain.reader.duplicatesOfRead
import raito.novel.download.NovelDownloadManager
import raito.novel.download.NovelDownloadPendingDeleter
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * "After reading automatically delete" in the novel reader, as Mihon's reader runs it: finishing a
 * chapter queues the one the slots retire, picked by [chapterToDeleteBehind] like manga's
 * `ReaderViewModel.deleteChapterIfNeeded`, and leaving the reader deletes the queue. So a chapter just
 * finished is still on disk to page back to. The sibling of [DeleteNovelChaptersAfterRead], which
 * fires on marking by hand.
 */
@Inject
class DeleteNovelChaptersBehindReader(
    private val novelPreferences: NovelPreferences,
    private val removableDownloads: NovelRemovableDownloads,
    // Deferred for the same reason as in [DeleteNovelChaptersAfterRead]: building the manager resumes
    // the persisted download queue, and a reader open must not do that.
    private val downloadManager: () -> NovelDownloadManager,
    private val chapterRepository: NovelChapterRepository,
    private val pendingDeleter: NovelDownloadPendingDeleter,
    private val libraryPreferences: LibraryPreferences,
) {

    /** [orderedIds] is the session's reading order, so the slots count positions the user actually
     *  moves through rather than raw chapter numbers. [memberIds] and [stitch] are the merge group's,
     *  as [raito.presentation.reader.text.NovelChapterFinish] takes them. */
    suspend fun await(orderedIds: List<Long>, readChapterId: Long, memberIds: List<Long>, stitch: List<ChapterUnit>) {
        val slots = novelPreferences.removeAfterReadSlots().get()
        val targetId = orderedIds.chapterToDeleteBehind(readChapterId, slots) { it } ?: return
        val target = chapterRepository.getById(targetId) ?: return
        if (!target.read) return
        // The copies "mark duplicate read" marked with it go too, as manga's trim takes them
        // (DeleteDuplicatesBehindReaderConformanceTest).
        val markDuplicates = libraryPreferences.markDuplicateReadChapterAsRead.get()
            .contains(LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING)
        val duplicates = if (!markDuplicates) {
            emptyList()
        } else {
            memberIds.flatMap { chapterRepository.getByNovelId(it) }
                .duplicatesOfRead(
                    target,
                    expandToUnits(setOf(target.id), stitch),
                    numberOf = { it.chapterNumber },
                    idOf = { it.id },
                    ownerOf = { it.novelId },
                )
                .map { it.copy(read = true) }
        }
        val removable = removableDownloads(listOf(target) + duplicates)
        if (removable.isNotEmpty()) pendingDeleter.addChapters(removable)
    }

    /** Deletes what [await] queued. The host calls it on leaving the reader. */
    suspend fun deletePending() {
        val ids = pendingDeleter.takePendingChapterIds()
        if (ids.isEmpty()) return
        downloadManager().deleteChapters(ids.mapNotNull { chapterRepository.getById(it) })
    }
}
