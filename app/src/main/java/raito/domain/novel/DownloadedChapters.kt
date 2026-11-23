package raito.domain.novel

import raito.domain.novel.model.Novel
import raito.domain.novel.model.NovelChapter
import raito.novel.download.NovelDownloadCache

/**
 * Ids of [chapters] on disk, each probed under its own novel in [owners] (see [ownersOf]), as manga's
 * [raito.domain.manga.downloadedChapterIds] probes each copy under its own source: a merged list pools
 * several novels' chapters. A chapter whose novel is missing from [owners] is not on disk.
 */
fun NovelDownloadCache.downloadedChapterIds(chapters: List<NovelChapter>, owners: Map<Long, Novel>): Set<Long> =
    chapters.groupBy { it.novelId }.flatMapTo(HashSet()) { (novelId, owned) ->
        owners[novelId]?.let { downloadedChapterIds(it, owned) }.orEmpty()
    }
