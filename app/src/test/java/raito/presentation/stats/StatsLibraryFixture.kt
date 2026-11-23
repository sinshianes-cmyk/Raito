package raito.presentation.stats

import raito.domain.entry.EntryCustomInfo
import raito.domain.entry.EntryId
import raito.domain.merge.MergeBucket
import raito.domain.novel.model.LibraryNovel
import raito.domain.readinglog.EntryReadDuration
import tachiyomi.domain.library.model.LibraryManga

/** A [StatsLibrary] holding only what a test names, each library entry its own series. */
internal fun statsLibrary(
    manga: List<LibraryManga> = emptyList(),
    novels: List<LibraryNovel> = emptyList(),
    customInfo: Map<EntryId, EntryCustomInfo> = emptyMap(),
    mangaReadDurations: List<EntryReadDuration> = emptyList(),
    novelReadDurations: List<EntryReadDuration> = emptyList(),
) = StatsLibrary(
    manga = StatsSeries(manga.map { MergeBucket(null, listOf(it)) }, emptyMap()) { it.manga.id },
    novels = StatsSeries(novels.map { MergeBucket(null, listOf(it)) }, emptyMap()) { it.novel.id },
    mangaTracks = emptyMap(),
    novelTracks = emptyMap(),
    loggedInTrackers = emptyList(),
    trackerNames = emptyMap(),
    categoryNames = emptyMap(),
    sourceBadges = emptyMap(),
    customInfo = customInfo,
    mangaTitles = emptyList(),
    novelTitles = emptyList(),
    mangaSessions = emptyList(),
    novelSessions = emptyList(),
    mangaReadDurations = mangaReadDurations,
    novelReadDurations = novelReadDurations,
)
