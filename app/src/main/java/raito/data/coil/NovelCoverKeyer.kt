package raito.data.coil

import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import raito.domain.entry.EntryId
import raito.domain.novel.model.NovelCover

/** Twin of [eu.kanade.tachiyomi.data.coil.MangaCoverKeyer], pinned by [libraryCustomCoverOwner]. */
class NovelCoverKeyer(private val coverCache: CoverCache) : Keyer<NovelCover> {
    override fun key(data: NovelCover, options: Options): String {
        val owner =
            libraryCustomCoverOwner(EntryId.Novel(data.novelId), data.isNovelFavorite, coverCache::hasCustomCover)
        return coverCacheKey(owner, data.url, data.lastModified)
    }
}
