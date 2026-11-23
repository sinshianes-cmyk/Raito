package raito.domain.novel.interactor

import dev.zacsweers.metro.Inject
import raito.domain.novel.model.CustomNovelInfo
import raito.domain.novel.repository.CustomNovelInfoRepository

@Inject
class SetCustomNovelInfo(
    private val repository: CustomNovelInfoRepository,
) {

    /**
     * Persist the override, or clear it when nothing is set (Reset to source). Suspends until the write
     * lands; the observing flow then re-emits and the screen updates on its own.
     */
    suspend fun set(info: CustomNovelInfo) {
        if (info.isEmpty) repository.delete(info.novelId) else repository.set(info)
    }
}
