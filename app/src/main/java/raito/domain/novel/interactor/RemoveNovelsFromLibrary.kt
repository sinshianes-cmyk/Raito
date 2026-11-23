package raito.domain.novel.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.cache.CoverCache
import raito.domain.entry.EntryId
import raito.domain.library.EntryLibraryRemoval
import raito.domain.novel.NovelMergeManager
import raito.domain.novel.NovelRepository
import raito.domain.novel.model.NovelUpdate
import raito.domain.track.source.SourceTrackerDispatcher
import kotlin.time.Instant

/** [EntryLibraryRemoval] over the novels table. Writes only the library date. */
@Inject
class RemoveNovelsFromLibrary(
    mergeManager: NovelMergeManager,
    sourceTracker: SourceTrackerDispatcher,
    private val novelRepository: NovelRepository,
    private val coverCache: CoverCache,
) : EntryLibraryRemoval(mergeManager, sourceTracker) {

    override fun entryId(id: Long): EntryId = EntryId.Novel(id)

    override suspend fun favoriteAt(id: Long): Instant? = novelRepository.getById(id)?.favoriteAt

    override suspend fun writeFavoriteAt(favoriteAt: Map<Long, Instant?>): Boolean =
        novelRepository.updateAll(favoriteAt.map { (id, at) -> NovelUpdate(id) { this.favoriteAt = at } })

    override suspend fun deleteCovers(id: Long) {
        coverCache.deleteFromCache(
            EntryId.Novel(id),
            novelRepository.getById(id)?.thumbnailUrl,
            deleteCustomCover = true,
        )
    }
}
