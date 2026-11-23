package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType // RK
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback // RK
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast // RK
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import raito.domain.library.ContentType
import raito.presentation.components.RaitoFloatingNavBar // RK
import raito.presentation.components.RaitoFloatingNavItem // RK
import raito.presentation.home.ContinueReadingFab
import raito.presentation.recents.RecentsMode
import raito.presentation.recents.RecentsTab
import raito.presentation.recents.ShowsUpdatesBadge
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.collectAsState
import eu.kanade.presentation.util.Tab as NavTab

object HomeScreen : Screen() {

    // RK: carries the content type beside the query, so a search sent from a series lands on that
    //     series' own chip rather than whichever one the library happens to be showing.
    private val librarySearchEvent = Channel<Pair<String, ContentType>>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    // RK --> the tab set is a value now, not a constant: one setting replaces Updates and History
    // with a single Recents tab holding both. Off by default, so this is the upstream list until
    // someone asks for the other one. `NavTab` is the navigable kind, aliased because this file's
    // own `Tab` is the sealed intent declared at the bottom.
    private fun tabs(
        combinedRecents: Boolean,
        showUpdates: Boolean,
        showHistory: Boolean,
        showLibrary: Boolean,
        showBrowse: Boolean,
        order: List<String>,
    ): List<NavTab> {
        var recentsPlaced = false
        return order.mapNotNull { key ->
            when (key) {
                "library" -> LibraryTab.takeIf { showLibrary }
                "browse" -> BrowseTab.takeIf { showBrowse }
                "updates", "history" -> when {
                    combinedRecents -> RecentsTab.takeIf { !recentsPlaced }?.also { recentsPlaced = true }
                    key == "updates" -> UpdatesTab.takeIf { showUpdates }
                    else -> HistoryTab.takeIf { showHistory }
                }
                else -> null
            }
        }
    }

    /** The tab a recents intent opens, told which mode it meant so the combined tab can honour it. */
    private fun recentsTarget(combinedRecents: Boolean, mode: RecentsMode): NavTab = when {
        combinedRecents -> RecentsTab.also { it.showMode(mode) }
        mode == RecentsMode.HISTORY -> HistoryTab
        else -> UpdatesTab
    }

    /** Where a tab that just left the set sends the user, so nothing is left rendering unselected. */
    private fun replacementFor(
        tab: cafe.adriel.voyager.navigator.tab.Tab,
        combinedRecents: Boolean,
    ): NavTab? = when {
        combinedRecents && (tab is UpdatesTab || tab is HistoryTab) -> RecentsTab
        !combinedRecents && tab is RecentsTab -> UpdatesTab
        else -> null
    }
    // RK <--

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        // RK: read here rather than per bar, so both bars and the fixup below agree within a frame.
        val combinedRecents by remember { context.appGraph.uiPreferences.combinedRecentsTab }.collectAsState()
        // RK -->
        val showUpdatesTab by remember { context.appGraph.uiPreferences.showUpdatesTab }.collectAsState()
        val showHistoryTab by remember { context.appGraph.uiPreferences.showHistoryTab }.collectAsState()
        val tabOrderRaw by remember { context.appGraph.uiPreferences.tabOrder }.collectAsState()
        val showLibraryTab by remember { context.appGraph.uiPreferences.showLibraryTab }.collectAsState()
        val showBrowseTab by remember { context.appGraph.uiPreferences.showBrowseTab }.collectAsState()
        // never empty: the screen-order screen refuses to hide the last tab, this is the backstop
        val tabs = tabs(
            combinedRecents,
            showUpdatesTab,
            showHistoryTab,
            showLibraryTab,
            showBrowseTab,
            HomeTabOrder.parse(tabOrderRaw),
        ).ifEmpty { listOf(LibraryTab) }
        // RK <--
        // RK: the app opens on the first tab of the user's order; later order changes don't move the current tab
        TabNavigator(
            tab = remember { tabs.first() },
            key = TabNavigatorKey,
        ) { tabNavigator ->
            // RK: a tab dropped from the list keeps rendering with nothing selected, since selection
            // is a per-item class comparison rather than a lookup, so it is moved on deliberately.
            LaunchedEffect(combinedRecents) {
                replacementFor(tabNavigator.current, combinedRecents)?.let { tabNavigator.current = it }
            }
            // RK: a tab hidden from the bar while open hands over to the first one that is left
            LaunchedEffect(tabs) {
                if (tabNavigator.current !in tabs) tabNavigator.current = tabs.first()
            }
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                val tabletUi = isTabletUi()
                val navigationSuiteType = if (tabletUi) {
                    NavigationSuiteType.NavigationRail
                } else {
                    NavigationSuiteType.NavigationBar
                }
                val navigationSuiteState = rememberNavigationSuiteScaffoldState()
                var barVisible by remember { androidx.compose.runtime.mutableStateOf(true) } // RK
                LaunchedEffect(navigationSuiteState, tabletUi) {
                    if (tabletUi) navigationSuiteState.show()
                    showBottomNavEvent.receiveAsFlow().collectLatest { show ->
                        barVisible = show // RK
                        if (tabletUi || show) {
                            navigationSuiteState.show()
                        } else {
                            navigationSuiteState.hide()
                        }
                    }
                }

                // RK --> phones get DropSauce's floating pill bar, tablets keep the rail
                val tabBody: @Composable () -> Unit = {
                    AnimatedContent(
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            materialFadeThroughIn(
                                initialScale = 1f,
                                durationMillis = TabFadeDuration,
                            ) togetherWith materialFadeThroughOut(durationMillis = TabFadeDuration)
                        },
                        label = "tabContent",
                    ) {
                        tabNavigator.saveableState(key = "currentTab", it) {
                            it.Content()
                        }
                    }
                }
                val tabContent: @Composable () -> Unit = {
                    // RK --> floating Continue button, on the library only
                    androidx.compose.foundation.layout.Box {
                        tabBody()
                        if (tabNavigator.current == LibraryTab) {
                            ContinueReadingFab(
                                Modifier
                                    .align(androidx.compose.ui.Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 16.dp),
                            )
                        }
                    }
                    // RK <--
                }
                val navLegacy by remember { context.appGraph.uiPreferences.navLegacy }.collectAsState() // RK
                if (tabletUi || navLegacy) {
                    NavigationSuiteScaffold(
                        navigationSuiteType = navigationSuiteType,
                        state = navigationSuiteState,
                        navigationSuiteColors = NavigationSuiteDefaults.colors(
                            navigationRailContainerColor = MaterialTheme.colorScheme
                                .surfaceColorAtElevation(3.dp),
                        ),
                        navigationItemVerticalArrangement = Arrangement.Center,
                        navigationItems = {
                            tabs.fastForEach { NavigationSuiteItem(it, navigationSuiteType) } // RK
                        },
                    ) {
                        tabContent()
                    }
                } else {
                    // Content runs full-height under a transparent floating pill; no band behind it.
                    val barBlur by remember { context.appGraph.uiPreferences.barBlur }.collectAsState()
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    var barHeightPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
                    val navInsetPx = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(density)
                    val barHeight = with(density) { barHeightPx.toDp() }
                    val overlap = if (barVisible) with(density) { (barHeightPx - navInsetPx).coerceAtLeast(0).toDp() } else 0.dp
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(
                            tachiyomi.presentation.core.components.material.LocalFloatingBarOverlap provides overlap,
                        ) {
                            raito.presentation.components.BarBlurContainer(
                                enabled = barBlur && barVisible,
                                zoneHeight = barHeight * 1.6f,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                tabBody()
                            }
                        }
                        if (tabNavigator.current == LibraryTab) {
                            ContinueReadingFab(
                                Modifier
                                    .align(androidx.compose.ui.Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = (if (barVisible) barHeight else 0.dp) + 16.dp),
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = barVisible,
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.BottomCenter)
                                .onSizeChanged { barHeightPx = it.height },
                            enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                        ) {
                            RaitoFloatingNavBar {
                                tabs.fastForEach { FloatingNavItem(it) }
                            }
                        }
                    }
                }
                // RK <--
            }

            val goToLibraryTab = { tabNavigator.current = LibraryTab }
            val homeTab = tabs.first() // RK: back returns to the user's first tab

            BackHandler(enabled = tabNavigator.current != homeTab, onBack = { tabNavigator.current = homeTab })

            // RK --> first tab: back asks for a second press within two seconds before the app closes
            val exitConfirmation by remember { context.appGraph.uiPreferences.exitConfirmation }.collectAsState()
            val exitPressedAt = remember { longArrayOf(0L) }
            BackHandler(enabled = exitConfirmation && tabNavigator.current == homeTab) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - exitPressedAt[0] < 2_000L) {
                    (context as? android.app.Activity)?.finish()
                } else {
                    exitPressedAt[0] = now
                    context.toast(MR.strings.exit_confirmation_toast)
                }
            }
            // RK <--

            LaunchedEffect(Unit) {
                launch {
                    // RK: the query carries its content type, see librarySearchEvent
                    librarySearchEvent.receiveAsFlow().collectLatest { (query, contentType) ->
                        goToLibraryTab()
                        LibraryTab.search(query, contentType)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        // RK: More is no longer a tab, so its intent opens no tab and leaves the current one
                        val target: NavTab? = when (it) {
                            is Tab.Library -> LibraryTab
                            // RK: the two recents intents keep their meaning and change their target.
                            // With one tab holding both, they open it on the matching mode instead.
                            Tab.Updates -> recentsTarget(combinedRecents, RecentsMode.UPDATES)
                            Tab.History -> recentsTarget(combinedRecents, RecentsMode.HISTORY)
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            is Tab.More -> null
                        }
                        target?.let { tab -> tabNavigator.current = tab }

                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NavigationSuiteItem(
        tab: eu.kanade.presentation.util.Tab,
        navigationSuiteType: NavigationSuiteType,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        // RK: read once, since each read of the getter builds the tab's icon painter again
        val options = tab.options
        NavigationSuiteItem(
            navigationSuiteType = navigationSuiteType,
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = {
                Icon(
                    painter = options.icon!!, // RK
                    contentDescription = options.title, // RK
                )
            },
            label = {
                Text(
                    text = options.title, // RK
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            badge = tabBadge(tab),
        )
    }

    // RK -->
    @Composable
    private fun FloatingNavItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val options = tab.options
        val graph = LocalContext.current.appGraph
        val uiPreferences = remember { graph.uiPreferences }
        val navLabels by uiPreferences.navLabels.collectAsState()
        val haptics by uiPreferences.hapticFeedback.collectAsState()
        val haptic = LocalHapticFeedback.current
        RaitoFloatingNavItem(
            selected = tabNavigator.current::class == tab::class,
            onClick = {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                tabNavigator.current = tab
            },
            onReselect = {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                scope.launch { tab.onReselect(navigator) }
            },
            icon = options.icon!!,
            label = options.title,
            badge = tabBadge(tab),
            alwaysShowLabel = navLabels,
        )
    }
    // RK <--

    @Composable
    private fun tabBadge(tab: eu.kanade.presentation.util.Tab): (@Composable () -> Unit)? {
        val context = LocalContext.current
        val count by produceState(initialValue = 0, tab) {
            val graph = context.appGraph
            when (tab) {
                // RK: asks what the tab does rather than which class it is, since the Updates
                // feed can now be drawn by the combined tab instead.
                is ShowsUpdatesBadge -> {
                    combine(
                        graph.libraryPreferences.newShowUpdatesCount.changes(),
                        graph.libraryPreferences.newUpdatesCount.changes(),
                    ) { show, count ->
                        if (show) count else 0
                    }
                        .collectLatest { value = it }
                }

                is BrowseTab -> {
                    // RK: every extension kind, as the Extensions tab's own badge counts
                    graph.extensionUpdateCounts.total
                        .collectLatest { value = it }
                }

                else -> value = 0
            }
        }
        if (count <= 0) return null
        // RK: a dot for the Updates tab when the user prefers it; the extensions badge keeps its count
        val dotPref = remember { context.appGraph.uiPreferences.updatesBadgeDot }
        val asDot by dotPref.collectAsState()
        if (tab is ShowsUpdatesBadge && asDot) return { Badge() }
        return {
            Badge {
                val desc = when (tab) {
                    // RK: same capability check as the count above, so the two cannot disagree.
                    is ShowsUpdatesBadge -> pluralStringResource(
                        MR.plurals.notification_chapters_generic,
                        count = count,
                        count,
                    )

                    is BrowseTab -> pluralStringResource(
                        MR.plurals.update_check_notification_ext_updates,
                        count = count,
                        count,
                    )

                    else -> null
                }
                Text(
                    text = count.toString(),
                    modifier = Modifier.semantics {
                        if (desc != null) contentDescription = desc
                    },
                )
            }
        }
    }

    suspend fun search(query: String, contentType: ContentType) { // RK: the type, see librarySearchEvent
        librarySearchEvent.send(query to contentType)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
