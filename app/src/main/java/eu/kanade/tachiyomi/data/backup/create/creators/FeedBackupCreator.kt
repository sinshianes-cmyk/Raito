package eu.kanade.tachiyomi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupFeedRow
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import raito.domain.source.FeedSavedSearchRepository
import raito.domain.source.SavedSearchRepository
import raito.domain.source.model.SavedSearch

/**
 * Saved searches and the feed rows built on them. A row carries its search whole rather than by id,
 * so a restore can match it against whatever the new install already has.
 */
@Inject
class FeedBackupCreator(
    private val savedSearchRepository: SavedSearchRepository,
    private val feedRepository: FeedSavedSearchRepository,
) {

    suspend fun savedSearches(): List<BackupSavedSearch> =
        savedSearchRepository.getAll().map(::toBackup)

    suspend fun feedRows(): List<BackupFeedRow> {
        val searches = savedSearchRepository.getAll().associateBy { it.id }
        return feedRepository.getAll().map { row ->
            BackupFeedRow(
                sourceKey = row.sourceKey.serialize(),
                global = row.global,
                savedSearch = row.savedSearchId?.let { searches[it] }?.let(::toBackup),
                feedOrder = row.feedOrder,
            )
        }
    }

    private fun toBackup(search: SavedSearch) = BackupSavedSearch(
        sourceKey = search.sourceKey.serialize(),
        name = search.name,
        query = search.query,
        filtersJson = search.filtersJson,
    )
}
