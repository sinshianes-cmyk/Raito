package raito.domain.recommendation

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.Track

/**
 * Orchestrates the related-mangas carousel from the source's own related list only, deduped into one
 * unranked pool, with intermediate snapshots pushed so the UI renders progressively. Dedup is
 * two-keyed: by [SManga.url] for a source's own duplicate rows, and by normalized title set for one
 * series listed under different titles. The pool carries agreement (times a key was seen) for ranking.
 */
@Inject
class RelatedMangasLoader {

    suspend fun load(
        manga: SManga,
        source: CatalogueSource,
        tracks: List<Track>,
        currentGenres: List<String>,
        onUpdate: suspend (RelatedPool) -> Unit,
    ): RelatedPool {
        val accumulator = Accumulator(manga)

        coroutineScope {
            launch {
                source.getRelatedMangaList(
                    manga = manga,
                    exceptionHandler = { e ->
                        // One keyword search failing must not kill the carousel; the rest still populate
                        // it. A cancellation (screen closed) isn't a real failure, so don't log it.
                        if (e !is CancellationException) {
                            logcat(LogPriority.WARN, e) { "Related-mangas keyword search failed" }
                        }
                    },
                    pushResults = { (_, mangas), _ ->
                        val candidates = mangas.map { m ->
                            RelatedMangaCandidate(
                                sourceId = source.id,
                                manga = m,
                                origin = RecommendationOrigin.SourceNative(source.name),
                            )
                        }
                        accumulator.add(candidates)?.let { onUpdate(it) }
                    },
                )
            }
        }

        return accumulator.snapshot()
    }

    /**
     * Mutex-guarded dedup + agreement bookkeeping shared by both streams. [add] returns a fresh
     * snapshot when it accepted at least one new candidate, else null (nothing changed).
     */
    private class Accumulator(manga: SManga) {
        private val mutex = Mutex()
        private val accumulated = LinkedHashSet<RelatedMangaCandidate>()
        private val seenTitleKeys = HashSet<String>()
        private val agreementByKey = HashMap<String, Int>()
        private val selfUrl = manga.url

        init {
            // Never recommend the manga back to itself.
            TitleNormalizer.key(manga.title)?.let { seenTitleKeys += it }
        }

        suspend fun add(candidates: List<RelatedMangaCandidate>): RelatedPool? =
            mutex.withLock {
                var added = false
                // Count each title key at most once per push, not once per candidate: a stream that
                // lists the same series twice in one batch (e.g. MangaUpdates' recommendations +
                // categoryRecommendations) must not manufacture cross-source agreement the ranker rewards.
                val batchKeys = HashSet<String>()
                for (candidate in candidates) {
                    if (candidate.manga.url == selfUrl) continue
                    val keys = candidate.titleKeys()
                    if (keys.isEmpty()) continue
                    batchKeys += keys
                    if (keys.any { it in seenTitleKeys }) continue
                    if (!accumulated.add(candidate)) continue // url already present
                    seenTitleKeys += keys
                    added = true
                }
                batchKeys.forEach { agreementByKey.merge(it, 1, Int::plus) }
                if (added) poolSnapshot() else null
            }

        suspend fun snapshot(): RelatedPool = mutex.withLock { poolSnapshot() }

        private fun poolSnapshot(): RelatedPool {
            val agreementByUrl = accumulated.associate { c ->
                c.manga.url to (c.titleKeys().maxOfOrNull { agreementByKey[it] ?: 1 } ?: 1)
            }
            return RelatedPool(accumulated.toList(), agreementByUrl)
        }
    }
}
