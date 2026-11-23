package raito.presentation.novel.details

import raito.domain.chapter.ReadingOrder
import raito.domain.merge.GroupMarks
import raito.domain.novel.NovelPreferences
import raito.domain.novel.model.Novel
import raito.domain.novel.model.NovelChapter
import raito.domain.novel.model.effectiveSortDescending
import raito.domain.novel.model.sortedAndFiltered
import raito.presentation.details.HiddenChapterView
import raito.presentation.details.resolveHiddenChapterView

/** A novel's chapter list as [novelShownRows] builds it, and the chapter its Resume button opens. */
data class NovelShownRows(
    val view: HiddenChapterView<NovelChapter>,
    val resume: NovelChapter?,
)

/**
 * The rows a novel's chapter list shows out of [chapters], in its order: hidden ones dropped unless they
 * are being shown, then the filters and sort. The list and Mark previous as read both take their rows
 * from here, so, as on manga, the action never reaches a chapter the list leaves out. Resume reads every
 * row the filters keep, hidden ones last, so it opens a hidden chapter only when nothing else is unread.
 */
fun novelShownRows(
    chapters: List<NovelChapter>,
    novel: Novel,
    prefs: NovelPreferences,
    hiddenKeys: Set<String>,
    showHiddenRequested: Boolean,
    keyOf: (NovelChapter) -> String,
    downloadedChapterIds: Set<Long>,
    marks: GroupMarks,
    downloadedOnly: Boolean,
): NovelShownRows {
    val listed = { rows: List<NovelChapter> ->
        rows.sortedAndFiltered(novel, prefs, downloadedChapterIds, marks, downloadedOnly)
    }
    val view = resolveHiddenChapterView(chapters, hiddenKeys, showHiddenRequested, keyOf)
    val resume = ReadingOrder.resumeAt(
        ReadingOrder.of(listed(chapters), novel.effectiveSortDescending(prefs)),
        isHidden = { keyOf(it) in hiddenKeys },
    ) { marks.isRead(it.id, it.read) }
    return NovelShownRows(view.copy(visible = listed(view.visible)), resume)
}
