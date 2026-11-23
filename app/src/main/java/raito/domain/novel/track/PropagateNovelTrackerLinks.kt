package raito.domain.novel.track

import dev.zacsweers.metro.Inject
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.novel.NovelRepository
import raito.domain.novel.interactor.GetNovelTracks
import raito.domain.novel.interactor.UpsertNovelTrack
import raito.domain.novel.model.NovelTrack
import raito.domain.track.handOutGroupTrackers

/** The novel side of [handOutGroupTrackers], gated by [RaitoLibraryPreferences.syncTrackerLinksGrouped]. */
@Inject
class PropagateNovelTrackerLinks(
    private val preferences: RaitoLibraryPreferences,
    private val novelRepository: NovelRepository,
    private val getNovelTracks: GetNovelTracks,
    private val upsertNovelTrack: UpsertNovelTrack,
) {

    /** Ensure every favorited member of [groupIds] carries each tracker bound anywhere in the group. */
    suspend fun distribute(groupIds: List<Long>) = handOutGroupTrackers(
        enabled = preferences.syncTrackerLinksGrouped.get(),
        groupIds = groupIds,
        isFavorite = { novelRepository.getById(it)?.favorite == true },
        tracksOf = { getNovelTracks.await(it) },
        trackerId = NovelTrack::trackerId,
        remoteId = NovelTrack::remoteId,
        lastChapterRead = NovelTrack::lastChapterRead,
        copyTo = { track, novelId -> track.copy(novelId = novelId) },
        writeAll = { upsertNovelTrack.awaitAll(it) },
    )
}
