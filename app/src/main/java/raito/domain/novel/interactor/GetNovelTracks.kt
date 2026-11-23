package raito.domain.novel.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.novel.NovelMergeManager
import raito.domain.novel.NovelTrackRepository
import raito.domain.novel.model.NovelTrack
import raito.domain.track.GroupTrackReader
import tachiyomi.core.common.util.system.logcat

@Inject
class GetNovelTracks(
    private val repository: NovelTrackRepository,
    private val mergeManager: NovelMergeManager,
    preferences: RaitoLibraryPreferences,
) {

    // The group read is shared with the manga side (raito.domain.manga.GetTracksInGroup); only the
    // novel repository reads below it are novel-specific.
    private val groupReader = GroupTrackReader(
        sharingEnabled = { preferences.syncTrackerLinksGrouped.get() },
        relatedIds = { mergeManager.relatedIdsList(it) },
        groupChanges = mergeManager.relatedIdsChanges(),
        readOne = { await(it) },
        observeOne = { subscribe(it) },
        trackerId = NovelTrack::trackerId,
        lastChapterRead = NovelTrack::lastChapterRead,
    )

    suspend fun awaitOne(id: Long): NovelTrack? {
        return try {
            repository.getTrackById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(novelId: Long): List<NovelTrack> {
        return try {
            repository.getTracksByNovelId(novelId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(novelId: Long): Flow<List<NovelTrack>> = repository.getTracksByNovelIdAsFlow(novelId)

    /** Every novel track, grouped by novel id, in one query. Mirrors [GetTracksPerManga.await]. */
    suspend fun awaitAll(): Map<Long, List<NovelTrack>> = repository.getTracks().groupBy(NovelTrack::novelId)

    /** Every novel track in the library, grouped by novel id. Mirrors [GetTracksPerManga.subscribe]. */
    fun subscribeAll(): Flow<Map<Long, List<NovelTrack>>> =
        repository.getTracksAsFlow().map { tracks -> tracks.groupBy(NovelTrack::novelId) }

    /** Tracks bound on any member of [novelId]'s merge group, one per tracker. */
    suspend fun awaitGroup(novelId: Long): List<NovelTrack> = groupReader.await(novelId)

    /** The entries a refresh of [novelId] reaches. */
    suspend fun groupIds(novelId: Long): List<Long> = groupReader.groupIds(novelId)

    /** Reactive [awaitGroup]. */
    fun subscribeGroup(novelId: Long): Flow<List<NovelTrack>> = groupReader.subscribe(novelId)
}
