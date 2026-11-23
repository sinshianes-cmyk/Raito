package raito.domain.novel

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import raito.domain.library.ContentType
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.merge.MergeGroupRepository
import raito.domain.merge.MergedChapterUnitRepository.GroupStitch
import raito.domain.merge.MergedGroupStitcher
import raito.domain.merge.rankingStamps
import raito.domain.merge.storedUnitsOf
import raito.domain.novel.model.NovelChapter

/**
 * The novel half of keeping a merge group's stored stitch current. Twin of
 * [raito.domain.manga.MangaGroupStitcher], pinned by MergedTrunkConformanceTest, the shared
 * [MergedGroupStitcher] contract and the one reconciliation that drives both; the novel stitch reads no scanlator filter and has no
 * gallery notion, so this side only differs in where the chapters and the source ranking come from.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class NovelGroupStitcher(
    private val mergeGroupRepository: MergeGroupRepository,
    private val novelRepository: NovelRepository,
    private val chapterRepository: NovelChapterRepository,
    private val mergeManager: NovelMergeManager,
    private val raitoLibraryPreferences: RaitoLibraryPreferences,
) : MergedGroupStitcher {

    override val contentType = ContentType.NOVELS

    override suspend fun stitch(groupId: Long): GroupStitch {
        val group = load(groupId) ?: return GroupStitch(emptyList(), emptyMap())
        val merged = NovelChapterAggregation.merge(
            group.chaptersByNovel,
            group.sourceIdByNovel,
            raitoLibraryPreferences.preferredNovelSources.get(),
            group.memberRanking,
        )
        val units = storedUnitsOf(
            chapters = group.chaptersByNovel.values.flatten(),
            merged = merged,
            id = { it.id },
            name = { it.name },
            number = { it.chapterNumber },
            order = { it.sourceOrder },
        )
        // No counts: the novel stitch ranks members on row count, which the library row already carries.
        return GroupStitch(units, recognizedCounts = emptyMap())
    }

    override suspend fun trunkNumbers(groupId: Long): Map<Long, Double> {
        val group = load(groupId) ?: return emptyMap()
        val trunk = NovelChapterAggregation.trunkId(
            group.chaptersByNovel,
            group.sourceIdByNovel,
            raitoLibraryPreferences.preferredNovelSources.get(),
            group.memberRanking,
        )
        return group.chaptersByNovel[trunk].orEmpty().associate { it.id to it.chapterNumber }
    }

    override suspend fun rankings(): Map<Long, String> =
        rankingStamps(
            mergeGroupRepository.getRankedNovelMembers(),
            raitoLibraryPreferences.preferredNovelSources.get(),
        )

    private class Inputs(
        val chaptersByNovel: Map<Long, List<NovelChapter>>,
        val sourceIdByNovel: Map<Long, String>,
        val memberRanking: List<Long>,
    )

    private suspend fun load(groupId: Long): Inputs? {
        val members = mergeGroupRepository.getFavoriteMembers(contentType, groupId)
        if (members.isEmpty()) return null
        return Inputs(
            chaptersByNovel = members.associateWith { chapterRepository.getByNovelId(it) },
            sourceIdByNovel = members.associateWith { novelRepository.getById(it)?.source.orEmpty() },
            // Any member resolves the group for its per-group ranking override (empty = none).
            memberRanking = mergeManager.overrideRankingMemberIds(members.first()),
        )
    }
}
