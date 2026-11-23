package raito.domain.novel

import raito.domain.chapter.hiddenChapterKey
import raito.domain.novel.model.NovelChapter

/**
 * A novel chapter's hidden key, from the stored source of the novel that owns this copy, which holds
 * whether or not that source's plugin is installed. Null while the owner is not in [sourceByNovelId].
 */
fun NovelChapter.hiddenKey(sourceByNovelId: Map<Long, String?>): String? =
    sourceByNovelId[novelId]?.let { hiddenChapterKey(it, url) }
