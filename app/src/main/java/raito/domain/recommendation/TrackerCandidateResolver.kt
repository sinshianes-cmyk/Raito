package raito.domain.recommendation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mihon.domain.manga.model.toDomainManga
import raito.util.runCatchingCancellable
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal
import java.util.concurrent.ConcurrentHashMap

/**
 * Opens a tracker recommendation (AniList, MyAnimeUpdates, MAL, Shikimori) straight on a title page: the
 * tracker's title is searched on the user's enabled sources and the first result whose normalised title
 * (or synonym) matches is stored and returned. Null when no source has it, and the caller then falls back
 * to global search. Matches are remembered for the session so a second tap is instant.
 */
@Inject
@SingleIn(AppScope::class)
class TrackerCandidateResolver(
    private val getEnabledSources: GetEnabledSources,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
) {

    private val resolved = ConcurrentHashMap<String, Long>()

    suspend fun resolve(candidate: RelatedMangaCandidate): Long? {
        if (candidate.sourceId != RECOMMENDS_SOURCE) return null
        resolved[candidate.manga.url]?.let { return it }

        val keys = candidate.titleKeys()
        if (keys.isEmpty()) return null

        val sources = getEnabledSources.subscribe().first()
            .filterNot { it.isLocal() }
            .distinctBy { it.id }
            .mapNotNull { sourceManager.get(it.id) }

        val match = coroutineScope {
            sources.take(MAX_SOURCES).map { source ->
                async(Dispatchers.IO) {
                    withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
                        runCatchingCancellable { source.getSearchManga(1, candidate.manga.title, FilterList()).mangas }
                            .getOrDefault(emptyList())
                            .firstOrNull { found -> TitleNormalizer.key(found.title)?.let { it in keys } == true }
                            ?.let { source.id to it }
                    }
                }
            }.awaitAll().firstNotNullOfOrNull { it }
        } ?: return null

        val (sourceId, manga) = match
        val id = networkToLocalManga(manga.toDomainManga(sourceId)).id
        resolved[candidate.manga.url] = id
        return id
    }

    private companion object {
        const val MAX_SOURCES = 8
        const val SOURCE_TIMEOUT_MS = 10_000L
    }
}
