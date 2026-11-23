package raito.domain.novel

import raito.domain.library.ContentType
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.merge.EntryMergeManager
import raito.domain.merge.MergeGroupRepository

/**
 * Novel source-grouping manager: [EntryMergeManager] fixed to [ContentType.NOVELS] and the novel
 * same-title switch. A distinct type only so injectors resolve the novel manager by type; all the logic
 * lives in the shared base.
 */
class NovelMergeManager(
    repository: MergeGroupRepository,
    preferences: RaitoLibraryPreferences,
    onMerged: suspend (memberIds: List<Long>) -> Unit,
    onBeforeDissolve: suspend (memberIds: List<Long>) -> Unit,
) : EntryMergeManager(
    ContentType.NOVELS,
    repository,
    preferences,
    sameTitlePreference = preferences.novelAutoMergeSameTitle,
    onMerged = onMerged,
    onBeforeDissolve = onBeforeDissolve,
)
