package raito.domain.novel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import raito.domain.library.ContentType
import raito.domain.merge.ChapterUnit
import raito.domain.merge.ReconcileMergedChapters
import raito.domain.merge.renderMergedReadingOrder
import raito.domain.novel.model.NovelChapter

/**
 * A novel group's chapter list as the stored stitch decided it, never stitched at the call site. Twin
 * of [raito.domain.manga.MergedChapterProvider], pinned by [ReconcileMergedChapters.currentStitch] and
 * [renderMergedReadingOrder], which both call; only the chapter type differs.
 */
@Inject
@SingleIn(AppScope::class)
class NovelMergedChapterProvider(
    private val mergeManager: NovelMergeManager,
    private val reconcile: ReconcileMergedChapters,
) {

    /** The group's stored stitch, rebuilt first when it is stale. Empty when the novel is ungrouped. */
    suspend fun stitchOf(anchorId: Long): List<ChapterUnit> =
        mergeManager.groupIdOf(anchorId)?.let { reconcile.currentStitch(ContentType.NOVELS, it) }.orEmpty()

    /** [chapters] as the merged reading order [stitch] describes. */
    fun merged(chapters: List<NovelChapter>, stitch: List<ChapterUnit>): List<NovelChapter> =
        renderMergedReadingOrder(chapters, stitch, { it.id }) { chapter, order -> chapter.copy(sourceOrder = order) }
}
