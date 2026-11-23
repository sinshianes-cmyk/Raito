package raito.data.novel

import raito.domain.novel.model.NovelChapter

/**
 * What [syncChaptersWithNovelSource] did. [newChapters] leaves out what
 * [raito.domain.chapter.chapterArrivals] flags, as the manga sync's return does. [foundRelease] is what
 * [raito.domain.chapter.foundRelease] made of its arrivals, each page judged against what it held.
 */
data class NovelChapterSyncResult(
    val newChapters: List<NovelChapter>,
    val changed: Boolean,
    val foundRelease: Boolean,
) {
    operator fun plus(other: NovelChapterSyncResult) = NovelChapterSyncResult(
        newChapters + other.newChapters,
        changed || other.changed,
        foundRelease || other.foundRelease,
    )

    companion object {
        val UNCHANGED = NovelChapterSyncResult(emptyList(), changed = false, foundRelease = false)
    }
}
