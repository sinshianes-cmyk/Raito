package raito.presentation.browse

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import raito.presentation.browse.source.sectionSources
import raito.presentation.recommendation.relatedDestination
import tachiyomi.core.common.Constants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuItem
import tachiyomi.presentation.core.components.TonalFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import eu.kanade.presentation.components.ExpressiveMenu
import eu.kanade.presentation.components.ExpressiveMenuDivider
import eu.kanade.presentation.components.ExpressiveMenuHeader
import eu.kanade.presentation.components.ExpressiveMenuItem
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.roundedfilled.Extension
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.roundedfilled.Keep
import raito.domain.library.ContentType
import raito.domain.source.SourceKey
import raito.novel.source.NovelSource
import raito.presentation.browse.catalogue.EntryCatalogueScreen
import raito.presentation.browse.components.NovelSourceIcon
import raito.presentation.browse.globalsearch.EntryGlobalSearchScreen
import raito.presentation.browse.source.BrowseSourceRow
import raito.presentation.browse.source.MangaSourcesProvider
import raito.presentation.browse.source.NovelSourcesProvider
import raito.presentation.browse.source.EntrySourcesFilterScreen
import raito.presentation.browse.source.NovelSourcesViewModel
import raito.presentation.browse.source.SourcesEngine
import raito.presentation.browse.source.SourcesList
import raito.presentation.browse.source.SourcesListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.screens.LoadingScreen

private val NavBarClearance = 112.dp

/**
 * The Explore page: a search launcher, shortcuts, and every enabled source as an icon grid, with a
 * Manga / Novels switch over the list. Extension management, Migrate and Feed sit behind Manage.
 * Assembly, search and the long-press sheet stay in [SourcesEngine].
 */
@Composable
fun Screen.RaitoExplore(browseViewModel: RaitoBrowseViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val mangaModel = metroViewModel<SourcesViewModel>()
    val novelModel = metroViewModel<NovelSourcesViewModel>()
    val providers = remember(mangaModel, novelModel) {
        listOf(
            MangaSourcesProvider(mangaModel, context.appGraph.extensionManager.loadedExtensionsFlow),
            NovelSourcesProvider(novelModel),
        )
    }
    val engine = assistedMetroViewModel<SourcesEngine, SourcesEngine.Factory> {
        create(providers, browseViewModel.searchQuery)
    }
    val state by engine.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val openManage = { navigator.push(BrowseManageScreen()) }
    val extensionsModel = metroViewModel<ExtensionsViewModel>()
    val isRefreshing by extensionsModel.isRefreshing.collectAsStateWithLifecycle()
    val updates by browseViewModel.totalUpdatesCount.collectAsStateWithLifecycle()
    val isGrid by browseViewModel.exploreGrid.collectAsStateWithLifecycle()
    val suggestionsEnabled by browseViewModel.suggestionsEnabled.collectAsStateWithLifecycle()
    val suggestionsState by browseViewModel.suggestions.collectAsStateWithLifecycle()
    val columns by browseViewModel.exploreColumns.collectAsStateWithLifecycle()
    // One engine over both kinds: the pager filters per page, so a swipe never waits on a reload.
    LaunchedEffect(state.contentType) {
        if (state.contentType != ContentType.ALL) browseViewModel.setContentType(ContentType.ALL)
    }
    // Manga only: the Novels switch is gone from this page, so there is one page
    val pagerState = rememberPagerState(initialPage = 0) { 1 }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { browseViewModel.setExploreNovels(it == 1) }
    }
    val scope = rememberCoroutineScope()
    val kindType = ContentType.MANGA

    val header: @Composable () -> Unit = {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExploreTile(
                    MaterialSymbols.Rounded.Download,
                    stringResource(MR.strings.label_download_queue),
                    Modifier.weight(1f),
                ) { navigator.push(DownloadQueueScreen) }
            }
            if (suggestionsEnabled) {
                SuggestionsRow(
                    state = suggestionsState,
                    onClick = { candidate ->
                        scope.launch {
                            val id = browseViewModel.resolveSuggestion(candidate)
                            navigator.push(relatedDestination(candidate, id))
                        }
                    },
                    onRefresh = browseViewModel::refreshSuggestions,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(MR.strings.label_extensions), style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { openManage() }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.action_manage),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (updates > 0) Badge()
                }
            }
        }
    }

    val allRows = state.items.filterIsInstance<SourcesListItem.Row>().map { it.row }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ExploreSearchRow(
                isGrid = isGrid,
                columns = columns,
                onSettings = { navigator.push(SettingsScreen()) },
                onSearch = { navigator.push(EntryGlobalSearchScreen()) },
                onFilter = { navigator.push(EntrySourcesFilterScreen(kindType)) },
                onDisplay = browseViewModel::setExploreGrid,
                onColumns = browseViewModel::setExploreColumns,
                onManage = openManage,
            )
            PullRefresh(
                refreshing = isRefreshing,
                enabled = true,
                onRefresh = extensionsModel::findAvailableExtensions,
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val type = if (page == 1) ContentType.NOVELS else ContentType.MANGA
                    val rows = allRows.filter { it.key.contentType == type }
                    // The grid drops section headings and the Last used copy; the list keeps both.
                    val gridRows = rows.filterNot { it.isUsedLast }
                    when {
                        state.isLoading -> Column { header(); LoadingScreen() }
                        rows.isEmpty() -> Column { header(); EmptyScreen(stringRes = MR.strings.source_empty_screen) }
                        isGrid -> LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = NavBarClearance),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) { header() }
                            items(gridRows, key = { "${it.key}" }) { row ->
                                SourceCell(
                                    row = row,
                                                                        onClick = { navigator.push(EntryCatalogueScreen(row.key)) },
                                    onLongClick = { engine.showDialog(row) },
                                )
                            }
                        }
                        else -> Column {
                            header()
                            SourcesList(
                                items = sectionSources(rows),
                                // Detailed list: the kind badge and each source's packaging are always shown
                                showContentType = true,
                                showsFormat = true,
                                showLatest = state.showLatest,
                                listState = rememberLazyListState(),
                                contentPadding = PaddingValues(bottom = NavBarClearance),
                                onClickItem = { row, latest ->
                                    navigator.push(
                                EntryCatalogueScreen(
                                    row.key,
                                    startLatest = latest ||
                                        (row.supportsLatest && context.appGraph.raitoSourcePreferences.defaultListingLatest.get()),
                                ),
                            )
                                },
                                onClickPin = engine::togglePin,
                                onLongClickItem = engine::showDialog,
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = NavBarClearance))
    }

    state.dialog?.let { dialog ->
        EntrySourceOptionsDialog(
            title = dialog.row.name,
            isPinned = dialog.row.isPinned,
            showToggleDisable = dialog.canDisable,
            showToggleIncognito = dialog.canToggleIncognito,
            isIncognito = dialog.isIncognito,
            onClickPin = {
                engine.togglePin(dialog.row)
                engine.closeDialog()
            },
            onClickToggleDisable = {
                engine.toggleDisable(dialog.row)
                engine.closeDialog()
            },
            onClickToggleIncognito = { engine.toggleIncognito(dialog) },
            onDismiss = engine::closeDialog,
            onClickShortcut = {
                pinSourceShortcut(context, dialog.row)
                engine.closeDialog()
            },
        )
    }

    val internalErrString = stringResource(MR.strings.internal_error)
    LaunchedEffect(Unit) {
        mangaModel.events.collectLatest { event ->
            when (event) {
                SourcesViewModel.Event.FailedFetchingSources ->
                    scope.launch { snackbarHostState.showSnackbar(internalErrString) }
            }
        }
    }
}

@Composable
private fun ExploreSearchRow(
    isGrid: Boolean,
    columns: Int,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
    onDisplay: (grid: Boolean) -> Unit,
    onColumns: (Int) -> Unit,
    onManage: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var displayOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onSettings) {
            Icon(MaterialSymbols.Rounded.Settings, contentDescription = stringResource(MR.strings.label_settings))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onSearch),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(MaterialSymbols.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = stringResource(MR.strings.action_global_search),
                modifier = Modifier.padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(MaterialSymbols.Rounded.MoreVert, contentDescription = null)
            }
            // RK --> DropSauce expressive menu with Browse's own entries: filter sources, display
            // (an in-menu page, like DropSauce's sub-menus) and manage extensions
            ExpressiveMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false; displayOpen = false },
            ) {
                if (displayOpen) {
                    ExpressiveMenuHeader(
                        title = stringResource(MR.strings.explore_menu_display),
                        onBack = { displayOpen = false },
                    )
                    ExpressiveMenuItem(
                        text = stringResource(MR.strings.explore_display_grid),
                        checked = isGrid,
                        radio = true,
                        onClick = { menuOpen = false; displayOpen = false; onDisplay(true) },
                    )
                    ExpressiveMenuItem(
                        text = stringResource(MR.strings.explore_display_detailed),
                        checked = !isGrid,
                        radio = true,
                        onClick = { menuOpen = false; displayOpen = false; onDisplay(false) },
                    )
                    ExpressiveMenuDivider()
                    // Always shown: picking a count from list mode switches to the grid with it
                    listOf(3, 4, 5).forEach { n ->
                        ExpressiveMenuItem(
                            text = "${stringResource(MR.strings.explore_menu_sources_per_row)}: $n",
                            checked = isGrid && n == columns,
                            radio = true,
                            onClick = {
                                menuOpen = false
                                displayOpen = false
                                if (!isGrid) onDisplay(true)
                                onColumns(n)
                            },
                        )
                    }
                } else {
                    ExpressiveMenuItem(
                        text = stringResource(MR.strings.explore_menu_filter_sources),
                        leadingIcon = MaterialSymbols.Rounded.FilterList,
                        onClick = { menuOpen = false; onFilter() },
                    )
                    ExpressiveMenuItem(
                        text = stringResource(MR.strings.explore_menu_display),
                        leadingIcon = MaterialSymbols.RoundedFilled.ViewModule,
                        hasSubmenu = true,
                        onClick = { displayOpen = true },
                    )
                    ExpressiveMenuItem(
                        text = stringResource(MR.strings.explore_menu_manage_extensions),
                        leadingIcon = MaterialSymbols.RoundedFilled.Extension,
                        onClick = { menuOpen = false; onManage() },
                    )
                }
            }
            // RK <--
        }
    }
}

@Composable
private fun ExploreTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceCell(row: BrowseSourceRow, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            when (row.key) {
                is SourceKey.Manga -> SourceIcon(source = row.source as Source, modifier = Modifier.size(44.dp))
                is SourceKey.Novel -> NovelSourceIcon(iconUrl = (row.source as NovelSource).iconUrl, size = 44.dp)
            }
        }
        Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (row.isPinned) {
                Icon(
                    MaterialSymbols.RoundedFilled.Keep,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 2.dp).size(12.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            SourceName(row.name)
        }
    }
}

@Composable
private fun SourceName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Asks the launcher to pin a shortcut that opens [row]'s catalogue, read back by `MainActivity`. */
private fun pinSourceShortcut(context: Context, row: BrowseSourceRow) {
    val intent = Intent(context, MainActivity::class.java)
        .setAction(Constants.SHORTCUT_SOURCE)
        .putExtra(Constants.SOURCE_EXTRA, row.key.serialize())
    val shortcut = ShortcutInfoCompat.Builder(context, "source-${row.key.serialize()}")
        .setShortLabel(row.name)
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(intent)
        .build()
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}
