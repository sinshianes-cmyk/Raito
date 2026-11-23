package raito.domain.novel.interactor

import dev.zacsweers.metro.Inject
import raito.domain.novel.NovelHistoryRepository
import raito.domain.novel.model.NovelHistoryUpdate

@Inject
class UpsertNovelHistory(
    private val repository: NovelHistoryRepository,
) {
    suspend fun await(update: NovelHistoryUpdate) = repository.upsertNovelHistory(update)
}
