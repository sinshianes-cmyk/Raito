package raito.data.work

import android.app.Application
import androidx.work.ListenableWorker
import eu.kanade.domain.track.service.DelayedTrackingUpdateWorker
import eu.kanade.tachiyomi.data.backup.create.BackupCreateWorker
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreWorker
import eu.kanade.tachiyomi.data.download.DownloadWorker
import eu.kanade.tachiyomi.data.library.LibraryUpdateWorker
import eu.kanade.tachiyomi.data.library.MetadataUpdateWorker
import exh.favorites.EhFavoritesBackupWorker
import exh.md.MangaDexSyncWorker
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import mihon.app.di.AppGraph
import mihon.core.metro.GraphProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import raito.data.novel.update.LnPluginUpdateWorker
import raito.data.novel.update.NovelUpdateWorker
import raito.data.recommendation.taste.TrackerLibraryRefreshWorker
import raito.data.track.TrackerRefreshWorker
import raito.domain.novel.track.NovelDelayedTrackingUpdateWorker
import raito.novel.download.NovelDownloadWorker
import java.lang.reflect.Modifier

/** Work an older build queued names the retired `*Job` class, and still runs as the renamed worker. */
class RenamedWorkersTest {

    private val graph = mockk<AppGraph>(relaxed = true).also {
        // These two read an injected field while they are built.
        every { it.inject(any<NovelUpdateWorker>()) } answers { injectMocks(firstArg()) }
        every { it.inject(any<NovelDownloadWorker>()) } answers { injectMocks(firstArg()) }
    }
    private val app = mockk<Application>(relaxed = true, moreInterfaces = arrayOf(GraphProvider::class)).also {
        every { it.applicationContext } returns it
        @Suppress("UNCHECKED_CAST")
        every { (it as GraphProvider<AppGraph>).graph } returns graph
    }

    private fun injectMocks(worker: Any) = worker.javaClass.declaredFields
        .filter { !Modifier.isStatic(it.modifiers) && !it.type.isPrimitive }
        .onEach { it.isAccessible = true }
        .filter { it.get(worker) == null }
        .forEach { it.set(worker, mockkClass(it.type.kotlin, relaxed = true)) }

    @ParameterizedTest
    @MethodSource("retiredNames")
    fun `work stored under a retired name builds its renamed worker`(
        retiredName: String,
        renamed: Class<out ListenableWorker>,
    ) {
        val worker = RenamedWorkers.createWorker(app, retiredName, mockk(relaxed = true))

        worker?.javaClass shouldBe renamed
    }

    @Test
    fun `a current name is left to WorkManager's own lookup`() {
        val worker = RenamedWorkers.createWorker(app, DownloadWorker::class.java.name, mockk(relaxed = true))

        worker.shouldBeNull()
    }

    companion object {
        @JvmStatic
        fun retiredNames() = listOf(
            "eu.kanade.domain.track.service.DelayedTrackingUpdateJob" to DelayedTrackingUpdateWorker::class.java,
            "eu.kanade.tachiyomi.data.backup.create.BackupCreateJob" to BackupCreateWorker::class.java,
            "eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob" to BackupRestoreWorker::class.java,
            "eu.kanade.tachiyomi.data.download.DownloadJob" to DownloadWorker::class.java,
            "eu.kanade.tachiyomi.data.library.LibraryUpdateJob" to LibraryUpdateWorker::class.java,
            "eu.kanade.tachiyomi.data.library.MetadataUpdateJob" to MetadataUpdateWorker::class.java,
            "exh.favorites.EhFavoritesBackupJob" to EhFavoritesBackupWorker::class.java,
            "exh.md.MangaDexSyncJob" to MangaDexSyncWorker::class.java,
            "raito.data.novel.update.LnPluginUpdateJob" to LnPluginUpdateWorker::class.java,
            "raito.data.novel.update.NovelUpdateJob" to NovelUpdateWorker::class.java,
            "raito.data.recommendation.taste.TrackerLibraryRefreshJob" to TrackerLibraryRefreshWorker::class.java,
            "raito.data.track.TrackerRefreshJob" to TrackerRefreshWorker::class.java,
            "raito.domain.novel.track.NovelDelayedTrackingUpdateJob" to NovelDelayedTrackingUpdateWorker::class.java,
            "raito.novel.download.NovelDownloadJob" to NovelDownloadWorker::class.java,
        ).map { (name, worker) -> Arguments.of(name, worker) }
    }
}
