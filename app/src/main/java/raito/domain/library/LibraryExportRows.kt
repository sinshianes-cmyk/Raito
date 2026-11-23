package raito.domain.library

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import raito.domain.entry.EntryCustomInfo
import raito.domain.entry.EntryId
import raito.domain.entry.GetEntryCustomInfo
import raito.domain.entry.withCustomInfo
import raito.domain.manga.MangaMergeManager
import raito.domain.merge.MergedChapterUnitRepository
import raito.domain.novel.NovelMergeManager
import raito.domain.novel.NovelRepository
import raito.domain.novel.model.Novel
import raito.domain.novel.model.withCustomInfo
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga

/** One library entry as the library list export writes it, whichever content type it is. */
data class LibraryExportRow(val title: String, val author: String?, val artist: String?)

/**
 * Every favourite of both content types, in the rows the library list export writes: manga first, then
 * novels, with a merged series written once, as the member its library row leads on.
 */
@Inject
class GetLibraryExportRows(
    private val getFavorites: GetFavorites,
    private val novelRepository: NovelRepository,
    private val mangaMergeManager: MangaMergeManager,
    private val novelMergeManager: NovelMergeManager,
    private val mergedChapterUnitRepository: MergedChapterUnitRepository,
    private val raitoLibraryPreferences: RaitoLibraryPreferences,
    private val getEntryCustomInfo: GetEntryCustomInfo,
) {

    suspend fun await(): List<LibraryExportRow> {
        val recognizedCounts = mergedChapterUnitRepository.getRecognizedChapterCountsAsFlow().first()
        val mangaSources = raitoLibraryPreferences.preferredMangaSources.get()
        val novelSources = raitoLibraryPreferences.preferredNovelSources.get()
        val novels = novelRepository.getLibraryNovelAsFlow().first()
        return libraryExportRows(
            manga = mangaMergeManager.rankedSeriesBuckets(getFavorites.await(), Manga::id) { members, ranking ->
                mangaLibraryRanking(members, ranking, mangaSources, recognizedCounts) { it }
            }.map { it.members.first() },
            novels = novelMergeManager.rankedSeriesBuckets(novels, { it.novel.id }) { members, ranking ->
                novelLibraryRanking(members, ranking, novelSources)
            }.map { it.members.first().novel },
            customInfo = getEntryCustomInfo.awaitAll(),
        )
    }
}

/** Each entry is written as the library shows it, under its Edit info overrides. */
internal fun libraryExportRows(
    manga: List<Manga>,
    novels: List<Novel>,
    customInfo: Map<EntryId, EntryCustomInfo>,
): List<LibraryExportRow> =
    manga.map { it.withCustomInfo(customInfo[EntryId.Manga(it.id)]) }
        .map { LibraryExportRow(it.title, it.author, it.artist) } +
        novels.map { it.withCustomInfo(customInfo[EntryId.Novel(it.id)]) }
            .map { LibraryExportRow(it.title, it.author, it.artist) }
