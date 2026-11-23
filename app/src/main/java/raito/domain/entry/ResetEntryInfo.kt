package raito.domain.entry

import dev.zacsweers.metro.Inject
import raito.domain.novel.interactor.SetCustomNovelInfo
import raito.domain.novel.model.CustomNovelInfo
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo

/**
 * Edit info's Reset all: every override goes, so each field tracks the source again. That includes the
 * custom cover, which is a cached file rather than a row field, so clearing the overrides alone would
 * leave it in place and winning.
 */
@Inject
class ResetEntryInfo(
    private val setCustomMangaInfo: SetCustomMangaInfo,
    private val setCustomNovelInfo: SetCustomNovelInfo,
    private val clearCustomCover: ClearCustomCover,
) {

    suspend fun await(id: EntryId) {
        when (id) {
            is EntryId.Manga -> setCustomMangaInfo.set(CustomMangaInfo(mangaId = id.rawId))
            is EntryId.Novel -> setCustomNovelInfo.set(CustomNovelInfo(novelId = id.rawId))
        }
        clearCustomCover.await(id)
    }
}
