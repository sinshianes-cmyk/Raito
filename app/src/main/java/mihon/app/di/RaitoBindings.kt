package mihon.app.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.track.TrackerManager
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.manga.MangaChapterSettings
import raito.domain.manga.MangaGroupCategories
import raito.domain.manga.MangaMergeManager
import raito.domain.manga.PropagateTrackerLinks
import raito.domain.merge.MergeGroupRepository
import raito.domain.novel.NovelChapterSettings
import raito.domain.novel.NovelGroupCategories
import raito.domain.novel.NovelMergeManager
import raito.domain.novel.track.PropagateNovelTrackerLinks
import raito.domain.recommendation.RaitoRecommendationPreferences
import raito.domain.recommendation.taste.AnilistLibraryFetcher
import raito.domain.recommendation.taste.BangumiLibraryFetcher
import raito.domain.recommendation.taste.KitsuLibraryFetcher
import raito.domain.recommendation.taste.MyAnimeListLibraryFetcher
import raito.domain.recommendation.taste.ShikimoriLibraryFetcher
import raito.domain.recommendation.taste.TrackerLibraryFetcher

/**
 * The Raito-owned bindings that cannot be a plain annotation on the class.
 */
@BindingContainer
object RaitoBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun providesMangaMergeManager(
        repository: MergeGroupRepository,
        preferences: RaitoLibraryPreferences,
        propagate: PropagateTrackerLinks,
        chapterSettings: MangaChapterSettings,
        groupCategories: MangaGroupCategories,
    ): MangaMergeManager = MangaMergeManager(
        repository,
        preferences,
        onMerged = {
            chapterSettings.adoptOwnerSetting(it)
            groupCategories.adoptOwnerCategories(it)
        },
    ) { propagate.distribute(it) }

    // The novel propagator arrives deferred because it reads tracks through GetNovelTracks, which
    // takes this manager, and Metro rejects the cycle. The lambda only ever runs inside a suspend
    // function, never during construction, so deferring it is safe.
    @Provides
    @SingleIn(AppScope::class)
    fun providesNovelMergeManager(
        repository: MergeGroupRepository,
        preferences: RaitoLibraryPreferences,
        propagate: () -> PropagateNovelTrackerLinks,
        chapterSettings: NovelChapterSettings,
        groupCategories: NovelGroupCategories,
    ): NovelMergeManager = NovelMergeManager(
        repository,
        preferences,
        onMerged = {
            chapterSettings.adoptOwnerSetting(it)
            groupCategories.adoptOwnerCategories(it)
        },
    ) { propagate().distribute(it) }

    // Each fetcher wants a concrete tracker, and those are properties of the TrackerManager
    // singleton rather than bindings of their own. Binding them separately would build second
    // tracker instances carrying their own login state, so the list is assembled from the singleton.
    @Provides
    fun providesTrackerLibraryFetchers(
        trackerManager: TrackerManager,
        preferences: RaitoRecommendationPreferences,
    ): List<TrackerLibraryFetcher> = listOf(
        AnilistLibraryFetcher(trackerManager.aniList, preferences),
        MyAnimeListLibraryFetcher(trackerManager.myAnimeList, preferences),
        KitsuLibraryFetcher(trackerManager.kitsu, preferences),
        ShikimoriLibraryFetcher(trackerManager.shikimori, preferences),
        BangumiLibraryFetcher(trackerManager.bangumi, preferences),
    )
}
