package raito.di

import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.data.coil.MangaCoverMetadata
import exh.GalleryAdder
import exh.eh.EHentaiUpdateHelper
import exh.eh.EHentaiUpdateWorker
import exh.favorites.EhFavoritesBackupWorker
import exh.md.MangaDexSyncWorker
import exh.pref.DelegateSourcePreferences
import exh.source.ExhPreferences
import exh.uconfig.EHConfigurator
import exh.ui.login.EhLoginActivity
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.migration.migrations.MergedDuplicateCarryMigration
import raito.data.novel.update.NovelUpdateWorker
import raito.data.track.TrackerRefreshWorker
import raito.domain.category.GetNovelCategories
import raito.domain.extension.ExtensionUpdateCounts
import raito.domain.library.RaitoLibraryPreferences
import raito.domain.manga.MangaMergeManager
import raito.domain.novel.NovelChapterRepository
import raito.domain.novel.NovelMergeManager
import raito.domain.novel.NovelPreferences
import raito.domain.novel.interactor.GetNovelHistory
import raito.domain.novel.interactor.GetNovelTracks
import raito.domain.novel.interactor.RepairNovelDetails
import raito.domain.novel.track.NovelDelayedTrackingUpdateWorker
import raito.domain.recommendation.RaitoRecommendationPreferences
import raito.domain.recommendation.TrackerCandidateResolver
import raito.domain.recommendation.taste.RefreshTrackerLibrary
import raito.domain.recommendation.taste.TasteLibraryRepository
import raito.domain.recommendation.taste.TrackerLibraryFetcher
import raito.domain.source.RaitoSourcePreferences
import raito.domain.suggestions.SuggestionsPreferences
import raito.domain.suggestions.SuggestionsRepository
import raito.domain.track.KitsuEntryIdCopies
import raito.novel.download.NovelDownloadCache
import raito.novel.download.NovelDownloadWorker
import raito.novel.font.NovelFontManager
import raito.novel.network.NovelImageRequests
import raito.novel.source.NovelSourceManager
import raito.novel.source.ireader.IReaderHostServices
import raito.novel.update.LnPluginUpdateChecker
import raito.novel.update.LnPluginUpdateNotifier
import raito.presentation.details.MangaEntryCoverViewModel
import raito.presentation.library.MangaLibraryAdapter
import raito.presentation.library.NovelLibraryAdapter
import raito.presentation.migrate.flow.MigrationAdapters
import raito.presentation.migrate.flow.MigrationPickHandoff
import raito.presentation.novel.details.NovelCoverViewModel
import raito.presentation.recents.MangaRecentsAdapter
import raito.presentation.recents.NovelRecentsAdapter
import raito.presentation.widget.UnifiedUpdatesGlanceWidget
import raito.presentation.widget.UnifiedUpdatesWidgetManager
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFlatMetadata

/**
 * Raito's members of the app graph. `AppGraph` extends this, so each is still read as
 * `context.appGraph.x`; keeping them here leaves `AppGraph` close enough to Mihon's to sync by hunk.
 */
interface RaitoGraph {
    fun inject(novelDownloadWorker: NovelDownloadWorker)
    fun inject(novelUpdateWorker: NovelUpdateWorker)
    fun inject(trackerRefreshWorker: TrackerRefreshWorker)
    fun inject(eHentaiUpdateWorker: EHentaiUpdateWorker)
    fun inject(ehFavoritesBackupWorker: EhFavoritesBackupWorker)
    fun inject(mangaDexSyncWorker: MangaDexSyncWorker)
    fun inject(novelDelayedTrackingUpdateWorker: NovelDelayedTrackingUpdateWorker)

    // Mihon's own widgets inject through PresentationWidgetGraph, contributed from presentation-widget.
    fun inject(unifiedUpdatesGlanceWidget: UnifiedUpdatesGlanceWidget)

    fun inject(ehLoginActivity: EhLoginActivity)

    val preferenceStore: PreferenceStore

    // Read through Context.appGraph by companions, objects and composable bodies, none of which can
    // be member-injected. migrationAdapters is read from the migrate screens, which pick one by
    // content type at runtime.
    val migrationAdapters: MigrationAdapters
    val exhPreferences: ExhPreferences
    val delegateSourcePreferences: DelegateSourcePreferences
    val ehConfigurator: EHConfigurator
    val eHentaiUpdateHelper: EHentaiUpdateHelper

    // Unscoped on purpose: the adder snapshots the enabled-language and disabled-source preferences
    // at construction, so every read has to build a fresh one.
    val galleryAdder: GalleryAdder
    val novelPreferences: NovelPreferences
    val novelSourceManager: NovelSourceManager
    val iReaderHostServices: IReaderHostServices // the extension loader builds IReader sources with it
    val novelImageRequests: NovelImageRequests // novel covers and pictures take their source's headers
    val extensionUpdateCounts: ExtensionUpdateCounts // the Browse badge counts plugin updates too
    val raitoRecommendationPreferences: RaitoRecommendationPreferences
    val lnPluginUpdateChecker: LnPluginUpdateChecker
    val lnPluginUpdateNotifier: LnPluginUpdateNotifier // the plugin update job posts and clears its notice
    val refreshTrackerLibrary: RefreshTrackerLibrary
    val kitsuEntryIdCopies: KitsuEntryIdCopies // Kitsu heals every copy of a Yokai-restored row
    val raitoLibraryPreferences: RaitoLibraryPreferences
    val raitoSourcePreferences: RaitoSourcePreferences
    val suggestionsPreferences: SuggestionsPreferences // Services switches the Browse suggestions row
    val suggestionsRepository: SuggestionsRepository
    val trackerCandidateResolver: TrackerCandidateResolver // opens a tracker recommendation on a source's title page

    val pagePreviewCache: PagePreviewCache
    val mangaCoverMetadata: MangaCoverMetadata
    val novelDownloadCache: NovelDownloadCache // Settings invalidates both download indexes
    val mangaMergeManager: MangaMergeManager
    val novelMergeManager: NovelMergeManager
    val novelChapterRepository: NovelChapterRepository // NovelUpdates finds the release a read links to
    val novelFontManager: NovelFontManager
    val migrationPickHandoff: MigrationPickHandoff
    val unifiedUpdatesWidgetManager: UnifiedUpdatesWidgetManager // the receiver reports widgets placed

    // The two details adapters build their cover model for whichever entry the source chip is showing,
    // so the id arrives at call time and the factory is what the graph can hand over.
    val mangaCoverViewModelFactory: MangaEntryCoverViewModel.Factory
    val novelCoverViewModelFactory: NovelCoverViewModel.Factory

    // The library engine owns exactly one adapter pair, built from the tab's own three models, so the
    // models arrive at call time here too.
    val mangaLibraryAdapterFactory: MangaLibraryAdapter.Factory
    val novelLibraryAdapterFactory: NovelLibraryAdapter.Factory
    val mangaRecentsAdapterFactory: MangaRecentsAdapter.Factory
    val novelRecentsAdapterFactory: NovelRecentsAdapter.Factory
    val tasteLibraryRepository: TasteLibraryRepository
    val trackerLibraryFetchers: List<TrackerLibraryFetcher> // Settings draws each pull switch from its fetcher

    // Interactors are unscoped, so every read builds a fresh instance. That matches the pre-port
    // shape: Injekt registered every one of these with addFactory, never addSingletonFactory.
    val getNovelCategories: GetNovelCategories
    val categoryRepository: CategoryRepository // Settings counts every category of both libraries
    val getNovelTracks: GetNovelTracks // NovelUpdates reads the chapters its track reaches, per the sharing switch

    // The metadata trio backs source-api's MetadataSource contract, which installed extensions
    // implement, so these three are reached through Injekt rather than the graph.
    val getManga: GetManga
    val getFlatMetadataById: GetFlatMetadataById
    val insertFlatMetadata: InsertFlatMetadata
    val getExhFavoriteMangaWithMetadata: GetExhFavoriteMangaWithMetadata
    val toggleIncognito: ToggleIncognito
    val getHistory: GetHistory // the Continue button reads the newest manga read
    val getNovelHistory: GetNovelHistory // and the newest novel read
    val getNextChapters: GetNextChapters
    val repairNovelDetails: RepairNovelDetails

    // A settings action whose writes must outlive the screen, and stay off the main thread, runs here
    @AppCoroutineScope
    val appCoroutineScope: CoroutineScope

    // App retries, at each launch, a download folder merge the upgrade's dedupe left unfinished
    val mergedDuplicateCarryMigration: MergedDuplicateCarryMigration
}
