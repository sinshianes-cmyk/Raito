package raito.domain.novel.interactor

import dev.zacsweers.metro.Inject
import raito.domain.history.HistoryClearRange
import raito.domain.novel.NovelHistoryRepository
import raito.domain.novel.model.NovelHistoryWithRelations

@Inject
class RemoveNovelHistory(
    private val repository: NovelHistoryRepository,
) {
    suspend fun awaitAll(): Boolean = repository.deleteAllNovelHistory()

    suspend fun awaitRange(range: HistoryClearRange): Boolean = when (range) {
        HistoryClearRange.ALL -> repository.deleteAllNovelHistory()
        HistoryClearRange.NOT_IN_LIBRARY -> repository.deleteNovelHistoryOutsideLibrary()
        HistoryClearRange.LAST_TWO_HOURS, HistoryClearRange.TODAY -> repository.deleteNovelHistorySince(range.since()!!)
    }

    suspend fun await(history: NovelHistoryWithRelations) = repository.resetNovelHistory(history.id)

    suspend fun await(novelId: Long) = repository.resetNovelHistoryByNovelId(novelId)
}
