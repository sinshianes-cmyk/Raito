package raito.presentation.debug

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.source.service.SourceManager

enum class StageStatus(val label: String) { PASSED("PASS"), FAILED("FAIL"), BLOCKED("BLOCK"), SKIPPED("SKIP") }

enum class SourceStatus(val label: String) {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    PASSED("PASSED"),
    BLOCKED("BLOCKED"),
    ERROR("ERROR"),
}

@Immutable
data class StageResult(val name: String, val status: StageStatus, val message: String?, val millis: Long) {
    fun line() = "${status.label.padEnd(5)} ${name.padEnd(9)} ${millis}ms${message?.let { "  $it" }.orEmpty()}"
}

@Immutable
data class SourceTestResult(
    val sourceId: Long,
    val sourceName: String,
    val lang: String,
    val status: SourceStatus,
    val stages: List<StageResult> = emptyList(),
)

@Immutable
data class SourceTestState(val results: List<SourceTestResult> = emptyList(), val running: Boolean = false) {
    fun summary() = SourceStatus.entries.joinToString("  ") { s -> "${s.label} ${results.count { it.status == s }}" }

    fun report() = results.joinToString("\n\n") { r ->
        "${r.status.label} ${r.sourceName} (${r.lang})\n" + r.stages.joinToString("\n") { it.line() }
    }
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SourceTestViewModel(
    private val sourceManager: SourceManager,
) : ViewModel() {

    val state: StateFlow<SourceTestState>
        field = MutableStateFlow<SourceTestState>(SourceTestState())

    private var job: Job? = null

    fun toggleRun() {
        if (job?.isActive == true) {
            job?.cancel()
            state.update { it.copy(running = false) }
            return
        }
        job = viewModelScope.launch {
            val sources = sourceManager.getOnlineSources().filterIsInstance<CatalogueSource>()
            state.value = SourceTestState(
                results = sources.map { SourceTestResult(it.id, it.name, it.lang, SourceStatus.PENDING) },
                running = true,
            )
            sources.forEach { runSource(it) }
            state.update { it.copy(running = false) }
        }
    }

    private suspend fun runSource(source: CatalogueSource) {
        val stages = mutableListOf<StageResult>()
        publish(source, SourceStatus.RUNNING, stages)

        var manga: SManga? = null
        var chapters: List<SChapter> = emptyList()

        stage(stages, "Popular") {
            val page = source.getPopularManga(1)
            manga = page.mangas.firstOrNull()
            check(manga != null) { "empty list" }
        }.also { publish(source, SourceStatus.RUNNING, stages) }

        if (source.supportsLatest) {
            stage(stages, "Latest") { check(source.getLatestUpdates(1).mangas.isNotEmpty()) { "empty list" } }
        } else {
            stages += StageResult("Latest", StageStatus.SKIPPED, "not supported", 0)
        }

        val first = manga
        if (first == null) {
            listOf("Search", "Details", "Chapters", "Pages").forEach {
                stages += StageResult(it, StageStatus.SKIPPED, "no entry to test", 0)
            }
        } else {
            stage(stages, "Search") {
                val query = first.title.take(SEARCH_QUERY_LENGTH)
                check(source.getSearchManga(1, query, source.getFilterList()).mangas.isNotEmpty()) { "no results for '$query'" }
            }
            stage(stages, "Details") {
                val update = source.getMangaUpdate(first, emptyList(), fetchDetails = true, fetchChapters = true)
                chapters = update.chapters
                check(update.manga.title.isNotBlank()) { "blank title" }
            }
            stage(stages, "Chapters") { check(chapters.isNotEmpty()) { "no chapters" } }
            val chapter = chapters.firstOrNull()
            if (chapter == null) {
                stages += StageResult("Pages", StageStatus.SKIPPED, "no chapter to test", 0)
            } else {
                stage(stages, "Pages") { check(source.getPageList(chapter).isNotEmpty()) { "no pages" } }
            }
        }

        val status = when {
            stages.any { it.status == StageStatus.FAILED } -> SourceStatus.ERROR
            stages.any { it.status == StageStatus.BLOCKED } -> SourceStatus.BLOCKED
            else -> SourceStatus.PASSED
        }
        publish(source, status, stages)
    }

    private suspend fun stage(out: MutableList<StageResult>, name: String, block: suspend () -> Unit) {
        val start = System.currentTimeMillis()
        val (status, message) = try {
            withTimeout(STAGE_TIMEOUT_MS) { block() }
            StageStatus.PASSED to null
        } catch (_: TimeoutCancellationException) {
            StageStatus.FAILED to "timed out"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val text = e.message.orEmpty()
            val blocked = BLOCK_MARKERS.any { text.contains(it, ignoreCase = true) }
            (if (blocked) StageStatus.BLOCKED else StageStatus.FAILED) to (text.ifBlank { e::class.simpleName })
        }
        out += StageResult(name, status, message?.take(MESSAGE_LENGTH), System.currentTimeMillis() - start)
    }

    private fun publish(source: CatalogueSource, status: SourceStatus, stages: List<StageResult>) {
        state.update { current ->
            current.copy(
                results = current.results.map {
                    if (it.sourceId == source.id) it.copy(status = status, stages = stages.toList()) else it
                },
            )
        }
    }

    private companion object {
        const val STAGE_TIMEOUT_MS = 30_000L
        const val SEARCH_QUERY_LENGTH = 12
        const val MESSAGE_LENGTH = 120
        val BLOCK_MARKERS = listOf("cloudflare", "HTTP error 403", "HTTP error 503")
    }
}
