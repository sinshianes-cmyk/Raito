package raito.presentation.widget

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import raito.domain.manga.MangaMergeManager
import raito.domain.merge.onePerSeries
import tachiyomi.domain.updates.model.UpdatesWithRelations

/** [MangaUpdatesPerSeries] over the manga merge groups, the rule the combined widget draws by. */
@Inject
@ContributesBinding(AppScope::class)
class MergedMangaUpdatesPerSeries(private val mangaMergeManager: MangaMergeManager) : MangaUpdatesPerSeries {
    override suspend fun onePerSeries(updates: List<UpdatesWithRelations>): List<UpdatesWithRelations> =
        mangaMergeManager.onePerSeries(updates) { it.mangaId }
}
