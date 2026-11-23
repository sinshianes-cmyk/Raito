package raito.presentation.novel

import eu.kanade.presentation.manga.DownloadAction
import raito.domain.chapter.DownloadCandidates
import raito.domain.chapter.ReadingOrder
import raito.domain.merge.GroupMarks
import raito.domain.novel.model.NovelChapter

/**
 * Resolve the details toolbar's [DownloadAction] to the novel chapters it should enqueue, over the rule
 * manga's toolbar also runs ([DownloadCandidates.forAction]). [chapters] arrives in the order it is
 * shown and [sortDescending] says which way. [marks] carries a merge group's read and bookmark state, so
 * the next unread is the group's. [isHidden] keys each copy by its own novel's source.
 */
fun selectChaptersForDownloadAction(
    chapters: List<NovelChapter>,
    sortDescending: Boolean,
    action: DownloadAction,
    excludedChapterIds: Set<Long>,
    marks: GroupMarks,
    isHidden: (NovelChapter) -> Boolean,
): List<NovelChapter> = DownloadCandidates.forAction(
    ReadingOrder.of(chapters, sortDescending),
    action,
    isRead = { marks.isRead(it.id, it.read) },
    isBookmarked = { marks.isBookmarked(it.id, it.bookmark) },
    isHidden = isHidden,
    isExcluded = { it.id in excludedChapterIds },
)
