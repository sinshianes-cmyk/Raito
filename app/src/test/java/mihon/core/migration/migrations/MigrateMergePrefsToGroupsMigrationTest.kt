package mihon.core.migration.migrations

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.core.migration.MigrationContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import raito.domain.dedupe.MergedDuplicate
import raito.domain.dedupe.MergedDuplicateRepository
import raito.domain.library.ContentType
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.merge.MergeGroupRepository
import raito.domain.novel.NovelRepository
import raito.domain.novel.model.Novel
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga

class MigrateMergePrefsToGroupsMigrationTest {

    // An install from before 189 still holds its groups as prefs of entry ids, and the upgrade's dedupe
    // runs first, so a merge naming a copy it merged away has to reach that copy's survivor
    @ParameterizedTest
    @EnumSource(Type::class)
    fun `a manual merge naming a merged-away duplicate groups the entry it merged into`(type: Type) = runTest {
        val groups = mockk<MergeGroupRepository>(relaxed = true) {
            coEvery { getGroupId(any(), any()) } returns null
        }
        val prefs = RaitoLibraryPreferences(
            InMemoryPreferenceStore(sequenceOf(InMemoryPreference(type.mergesKey, setOf("1,5"), emptySet()))),
        )
        val record = mockk<MergedDuplicateRepository> {
            coEvery { getAll() } returns
                listOf(MergedDuplicate(type.contentType, discardedId = 5, survivorId = 3, discardedTitle = "t"))
        }

        MigrateMergePrefsToGroupsMigration(prefs, groups, mangaFavorites, novelRepository, record)
            .invoke(MigrationContext(dryrun = false, previousVersion = 185))

        coVerify { groups.createGroup(type.contentType, listOf(1L, 3L)) }
    }

    private val mangaFavorites = mockk<GetFavorites> {
        coEvery { await() } returns listOf(
            Manga.create().copy(id = 1, title = "a"),
            Manga.create().copy(id = 3, title = "b"),
        )
    }

    private val novelRepository = mockk<NovelRepository> {
        coEvery { getFavorites() } returns listOf(
            Novel.create().copy(id = 1, title = "a"),
            Novel.create().copy(id = 3, title = "b"),
        )
    }

    enum class Type(val contentType: ContentType, val mergesKey: String) {
        MANGA(ContentType.MANGA, RaitoLibraryPreferences.MANGA_MANUAL_MERGES_KEY),
        NOVEL(ContentType.NOVELS, RaitoLibraryPreferences.NOVEL_MANUAL_MERGES_KEY),
    }
}
