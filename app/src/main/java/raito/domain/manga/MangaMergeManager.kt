package raito.domain.manga

import raito.domain.library.ContentType
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.merge.EntryMergeManager
import raito.domain.merge.MergeGroupRepository

/**
 * Manga source-grouping manager: [EntryMergeManager] fixed to [ContentType.MANGA] and the manga
 * same-title switch. A distinct type only so injectors resolve the manga manager by type; all the logic
 * lives in the shared base.
 */
class MangaMergeManager(
    repository: MergeGroupRepository,
    preferences: RaitoLibraryPreferences,
    onMerged: suspend (memberIds: List<Long>) -> Unit,
    onBeforeDissolve: suspend (memberIds: List<Long>) -> Unit,
) : EntryMergeManager(
    ContentType.MANGA,
    repository,
    preferences,
    sameTitlePreference = preferences.autoMergeSameTitle,
    onMerged = onMerged,
    onBeforeDissolve = onBeforeDissolve,
)
