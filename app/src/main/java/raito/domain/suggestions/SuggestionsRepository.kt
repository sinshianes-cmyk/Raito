package raito.domain.suggestions

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import raito.domain.recommendation.RecommendationOrigin
import raito.domain.recommendation.RelatedMangaCandidate
import raito.domain.recommendation.TitleNormalizer
import raito.domain.manga.AdultContentChecker
import raito.util.isAdultEntry
import raito.util.runCatchingCancellable
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal

data class SuggestionsState(
    val items: List<RelatedMangaCandidate> = emptyList(),
    val loading: Boolean = false,
)

/**
 * Builds the Browse suggestions: the genres the library leans on most are searched on a few enabled
 * sources, and what comes back (minus anything already owned) is the row. Held in memory and rebuilt
 * when stale, so Browse opens instantly on the last result and nothing is written to the database.
 */
@Inject
@SingleIn(AppScope::class)
class SuggestionsRepository(
    private val getLibraryManga: GetLibraryManga,
    private val getEnabledSources: GetEnabledSources,
    private val sourceManager: SourceManager,
    private val preferences: SuggestionsPreferences,
    private val adultContentChecker: AdultContentChecker,
) {

    private val _state = MutableStateFlow(SuggestionsState())
    val state: StateFlow<SuggestionsState> = _state

    private val mutex = Mutex()
    private var builtAt = 0L

    /** Rebuilds when [force]d or when the last result is older than [STALE_AFTER_MS]. */
    suspend fun refresh(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - builtAt < STALE_AFTER_MS && _state.value.items.isNotEmpty()) return
        // A second caller while one build runs would only repeat its requests.
        if (!mutex.tryLock()) return
        try {
            _state.update { it.copy(loading = true) }
            val items = runCatchingCancellable { build() }.getOrDefault(emptyList())
            builtAt = System.currentTimeMillis()
            _state.value = SuggestionsState(items = items, loading = false)
        } finally {
            _state.update { it.copy(loading = false) }
            mutex.unlock()
        }
    }

    fun clear() {
        builtAt = 0L
        _state.value = SuggestionsState()
    }

    /**
     * Streams: each source's finds are merged and ranked into the row the moment that source answers, so
     * the first covers show after one fast source instead of after the slowest one. Tags are drawn by
     * library weight rather than shuffled flat, a title found under several of the reader's tags ranks
     * higher, and the final order interleaves sources so one site cannot fill the row.
     */
    private suspend fun build(): List<RelatedMangaCandidate> {
        val library = getLibraryManga.await()
        if (library.isEmpty()) return emptyList()
        val hideNsfw = preferences.hideNsfw.get()

        val counts = library
            .flatMap { it.manga.genre.orEmpty() }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && !(hideNsfw && isAdultEntry(false, null, listOf(it))) }
            .groupingBy { it }
            .eachCount()
        val tags = counts.entries.sortedByDescending { it.value }.take(TOP_TAGS)
        val tagWeight = tags.associate { it.key to it.value.toFloat() / (tags.firstOrNull()?.value ?: 1) }
        val ownedKeys = if (preferences.hideInLibrary.get()) {
            library.flatMapTo(HashSet()) { listOfNotNull(TitleNormalizer.key(it.manga.title)) }
        } else {
            emptySet()
        }
        val ownedUrls = library.mapTo(HashSet()) { it.manga.source to it.manga.url }

        val candidates = getEnabledSources.subscribe().first()
            .filterNot { it.isUsedLast || it.isLocal() }
            .distinctBy { it.id }
        val adultIds = if (hideNsfw) {
            adultContentChecker.libraryAdultMangaSources(candidates.mapTo(HashSet()) { it.id })
        } else {
            emptySet()
        }
        val sources = candidates
            .filter { it.id !in adultIds }
            .shuffled()
            .take(MAX_SOURCES)
            .mapNotNull { sourceManager.get(it.id) }

        // url key -> candidate with the summed weight of the tags that found it
        val pool = LinkedHashMap<String, Pair<RelatedMangaCandidate, Float>>()
        val poolLock = Mutex()
        suspend fun publish(found: List<Pair<RelatedMangaCandidate, Float>>) = poolLock.withLock {
            found.forEach { (c, w) ->
                if ((c.sourceId to c.manga.url) in ownedUrls) return@forEach
                if (c.titleKeys().any { it in ownedKeys }) return@forEach
                if (hideNsfw && isAdultEntry(false, null, c.manga.getGenres())) return@forEach
                val key = c.titleKeys().firstOrNull() ?: "${c.sourceId}:${c.manga.url}"
                val prev = pool[key]
                pool[key] = if (prev == null) c to w else prev.first to (prev.second + w)
            }
            _state.update { it.copy(items = rank(pool.values.toList()), loading = true) }
        }

        val semaphore = Semaphore(PARALLELISM)
        coroutineScope {
            sources.map { source ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        withTimeoutOrNull(SOURCE_TIMEOUT_MS) {
                            val picks = pickTags(tags.map { it.key }, tagWeight)
                            // a source's searches run side by side, not one after another
                            val tagged = coroutineScope {
                                picks.map { tag ->
                                    async {
                                        runCatchingCancellable { source.getSearchManga(1, tag, FilterList()).mangas }
                                            .getOrDefault(emptyList())
                                            .take(PER_SEARCH)
                                            .map {
                                                RelatedMangaCandidate(
                                                    source.id, it, origin = RecommendationOrigin.TagSearch(tag),
                                                ) to (tagWeight[tag] ?: 0.5f)
                                            }
                                    }
                                }.awaitAll().flatten()
                            }
                            // A source with no usable search still lends its popular page.
                            tagged.ifEmpty {
                                runCatchingCancellable { source.getPopularManga(1).mangas }
                                    .getOrDefault(emptyList())
                                    .take(PER_SEARCH)
                                    .map {
                                        RelatedMangaCandidate(
                                            source.id, it, origin = RecommendationOrigin.SourceNative(source.name),
                                        ) to 0.3f
                                    }
                            }
                        }.orEmpty().let { publish(it) }
                    }
                }
            }.awaitAll()
        }
        return poolLock.withLock { rank(pool.values.toList()) }
    }

    /** Weighted draw without repeats: heavier library tags are likelier, yet each refresh differs. */
    private fun pickTags(tags: List<String>, weight: Map<String, Float>): List<String> {
        val left = tags.toMutableList()
        val out = mutableListOf<String>()
        while (out.size < TAGS_PER_SOURCE && left.isNotEmpty()) {
            var r = kotlin.random.Random.nextFloat() * left.sumOf { (weight[it] ?: 0.1f).toDouble() }.toFloat()
            val pick = left.firstOrNull { r -= (weight[it] ?: 0.1f); r <= 0f } ?: left.last()
            left.remove(pick)
            out += pick
        }
        return out
    }

    /** Best matches first within each source, then the sources taken in turns. */
    private fun rank(scored: List<Pair<RelatedMangaCandidate, Float>>): List<RelatedMangaCandidate> {
        val perSource = scored
            .groupBy { it.first.sourceId }
            .values
            .map { list -> list.sortedByDescending { it.second + kotlin.random.Random.nextFloat() * 0.15f }.map { it.first } }
        val out = ArrayList<RelatedMangaCandidate>()
        var i = 0
        while (out.size < MAX_RESULTS && perSource.any { i < it.size }) {
            perSource.forEach { list -> if (i < list.size && out.size < MAX_RESULTS) out += list[i] }
            i++
        }
        return out.distinctBy { it.sourceId to it.manga.url }
    }

    private companion object {
        const val STALE_AFTER_MS = 6 * 60 * 60 * 1000L
        const val SOURCE_TIMEOUT_MS = 9_000L
        const val TOP_TAGS = 8
        const val TAGS_PER_SOURCE = 3
        const val PER_SEARCH = 8
        const val MAX_SOURCES = 6
        const val PARALLELISM = 6
        const val MAX_RESULTS = 40
    }
}
