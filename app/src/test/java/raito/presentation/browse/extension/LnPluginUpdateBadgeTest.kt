package raito.presentation.browse.extension

import android.app.NotificationManager
import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import raito.domain.novel.LnInstalledPluginMetadata
import raito.domain.novel.NovelPreferences
import raito.domain.source.RaitoSourcePreferences
import raito.novel.install.LnPluginInstaller
import raito.novel.registry.LnRegistryEntry
import raito.novel.registry.LnRegistryFetcher
import raito.novel.registry.LnRepoRegistries
import raito.novel.registry.LnRepoResult
import raito.novel.source.NovelSourceManager
import raito.novel.update.LnPluginUpdateChecker
import raito.novel.update.LnPluginUpdateNotifier
import raito.presentation.MainDispatcherExtension
import raito.presentation.recents.EmittingPreferenceStore
import raito.util.testAppScope

/** The background check and the Extensions screen read one copy of the registries, the newest. */
class LnPluginUpdateBadgeTest {

    private val dispatcher = StandardTestDispatcher()

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension { dispatcher }

    private val prefs = NovelPreferences(EmittingPreferenceStore()).apply {
        addedRepoUrls().set(setOf(REPO))
        installedPluginUrls().set(setOf(URL))
        installedPluginMetadata().set(mapOf(URL to LnInstalledPluginMetadata(ID, version = INSTALLED)))
    }

    // The repo publishes a new version between the screen's fetch and the background check's.
    private var listed = INSTALLED
    private val fetcher = LnRegistryFetcher { listOf(entry(listed)) }
    private val registries = LnRepoRegistries(fetcher, prefs)

    // The Context only clears the notice once nothing is pending.
    private val context = mockk<Context>(relaxed = true) {
        every { getSystemService(any<String>()) } returns mockk<NotificationManager>(relaxed = true)
    }
    private val notifier = LnPluginUpdateNotifier(context, mockk(), prefs)
    private val checker = LnPluginUpdateChecker(
        fetcher,
        registries,
        LnPluginInstaller(mockk(), mockk(), mockk(relaxed = true), prefs, mockk()),
        prefs,
        notifier,
    )

    private fun model() = main.track(
        LnPluginManagerViewModel(
            mockk<NovelSourceManager> { every { loadedSources() } returns flowOf(emptyList()) },
            mockk<LnPluginInstaller>(relaxed = true) { every { failures } returns MutableStateFlow(emptyMap()) },
            registries,
            prefs,
            notifier,
            testAppScope(),
            RaitoSourcePreferences(EmittingPreferenceStore()),
        ),
    )

    @Test
    fun `a check's fetch replaces the registries the screens kept`() = runTest(dispatcher) {
        registries.results.first()
        listed = NEWER

        checker.checkAndRecord()

        registries.results.first() shouldBe mapOf(REPO to LnRepoResult.Reached(listOf(entry(NEWER))))
    }

    @Test
    fun `a badge a newer check raised survives opening Browse`() = runTest(dispatcher) {
        registries.results.first()
        listed = NEWER
        checker.checkAndRecord()

        val watching = launch { model().state.collect {} }
        advanceUntilIdle()
        watching.cancel()

        prefs.pendingPluginUpdates().get() shouldBe setOf(ID)
    }

    private fun entry(version: String) = LnRegistryEntry(
        id = ID,
        name = ID,
        site = "https://site.test",
        lang = "English",
        version = version,
        url = URL,
        iconUrl = null,
    )

    private companion object {
        const val ID = "plugin"
        const val REPO = "https://repo.test/plugins.min.json"
        const val URL = "https://repo.test/plugin.js"
        const val INSTALLED = "1.0.0"
        const val NEWER = "1.0.1"
    }
}
