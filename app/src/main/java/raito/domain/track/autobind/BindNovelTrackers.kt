package raito.domain.track.autobind

import dev.zacsweers.metro.Inject
import raito.domain.novel.interactor.AddNovelTrack
import raito.domain.novel.model.Novel
import raito.domain.track.supportsContent
import raito.novel.source.NovelSourceManager
import tachiyomi.core.common.util.lang.withIOContext

/**
 * Binds a novel just added to the library to the trackers that know its source. Twin of
 * `AddTracks.bindEnhancedTrackers`, pinned by [bindOnAdd], which both run (AutoBindTest).
 */
@Inject
class BindNovelTrackers(
    private val autoBindTrackers: AutoBindTrackers,
    private val sourceManager: NovelSourceManager,
    private val addNovelTrack: AddNovelTrack,
) {

    suspend fun await(novel: Novel) = withIOContext {
        val trackers = autoBindTrackers.loggedIn().filter { it.tracker.supportsContent(isNovel = true) }
        // Checked first, so an add with nothing to bind never resolves the source and loads plugins.
        if (trackers.isEmpty()) return@withIOContext
        val source = sourceManager.get(novel.source) ?: return@withIOContext
        bindOnAdd(AutoBindEntry.Novel(novel, source), trackers) { candidate, match ->
            addNovelTrack.bind(candidate.tracker, match, novel.id)
        }
    }
}
