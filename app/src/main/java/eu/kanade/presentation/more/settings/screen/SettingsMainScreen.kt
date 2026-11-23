package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import raito.presentation.settings.CardPosition
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import raito.presentation.settings.CategoryPalette
import raito.presentation.settings.SettingsIconBubble
import raito.presentation.settings.preferenceCard
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ChromeReaderMode
import mihon.icons.materialsymbols.rounded.Code
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Palette
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Security
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.rounded.Sync
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()
        // The Novel reader card carries the switch that shows or hides novels across the whole app.
        val context = LocalContext.current
        val novelsPreference = remember { context.appGraph.raitoSourcePreferences.novelsEnabled }
        val novelsEnabled by novelsPreference.collectAsState()

        Scaffold(
            topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState),
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = MaterialSymbols.Rounded.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding ->
                val state = rememberLazyListState()
                val indexSelected = if (twoPane) {
                    items.indexOfFirst { it.screen::class == navigator.items.first()::class }
                        .also {
                            LaunchedEffect(Unit) {
                                state.animateScrollToItem(it)
                                if (it > 0) {
                                    // Lift scroll
                                    topBarState.contentOffset = topBarState.heightOffsetLimit
                                }
                            }
                        }
                } else {
                    null
                }

                LazyColumn(
                    state = state,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 24.dp,
                    ),
                ) {
                    // RK: tiles grouped under four headings, each its own card, so the landing reads in
                    // chunks instead of one eleven-row list
                    sections.forEach { section ->
                        item(key = "header-${section.titleRes}") {
                            PreferenceGroupHeader(title = stringResource(section.titleRes))
                        }
                        itemsIndexed(
                            items = section.items,
                            key = { _, item -> item.hashCode() },
                        ) { index, item ->
                            val selected = indexSelected == items.indexOf(item)
                            val isNovelCard = item.screen::class == SettingsNovelReaderScreen::class
                            val enabled = !isNovelCard || novelsEnabled
                            // RK: DropSauce settings landing, one grouped card of coloured icon tiles
                            SettingsTile(
                                title = stringResource(item.titleRes),
                                subtitle = item.formatSubtitle(),
                                icon = item.icon,
                                paletteKey = item.paletteKey,
                                position = CardPosition(index, section.items.size),
                                selected = selected && enabled,
                                enabled = enabled,
                                onToggle = if (isNovelCard) {
                                    { novelsPreference.set(it) }
                                } else {
                                    null
                                },
                                onClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                        item(key = "gap-${section.titleRes}") { Spacer(Modifier.height(8.dp)) }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    @Composable
    private fun SettingsTile(
        title: String,
        subtitle: String?,
        icon: ImageVector,
        paletteKey: String,
        position: CardPosition,
        selected: Boolean,
        enabled: Boolean,
        onToggle: ((Boolean) -> Unit)?,
        onClick: () -> Unit,
    ) {
        val container = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .preferenceCard(position, container)
                // Off: the card greys out and no longer opens its screen.
                .clickable(enabled = enabled, onClick = onClick)
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBubble(
                icon = icon,
                colors = CategoryPalette.forKey(paletteKey),
                modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f).alpha(if (enabled) 1f else 0.38f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onToggle != null) {
                Spacer(Modifier.width(8.dp))
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
        }
    }

    private data class Item(
        val paletteKey: String,
        val titleRes: StringResource,
        val subtitleRes: StringResource? = null,
        val formatSubtitle: @Composable () -> String? = { subtitleRes?.let { stringResource(it) } },
        val icon: ImageVector,
        val screen: VoyagerScreen,
    )

    private val items = listOf(
        Item(
            paletteKey = "appearance",
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            icon = MaterialSymbols.Rounded.Palette,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            paletteKey = "library",
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary,
            icon = MaterialSymbols.Rounded.CollectionsBookmark,
            screen = SettingsLibraryScreen,
        ),
        // RK --> one reader entry per content type, replacing the single Reader screen. Each is
        // self-contained: every setting on it applies to that reader only, so neither screen has to
        // explain a value shared with the other one.
        Item(
            paletteKey = "reader",
            titleRes = MR.strings.pref_category_manga_reader,
            subtitleRes = MR.strings.pref_category_manga_reader_summary,
            icon = MaterialSymbols.AutoMirroredRounded.ChromeReaderMode,
            screen = SettingsMangaReaderScreen,
        ),
        Item(
            paletteKey = "reader",
            titleRes = MR.strings.pref_category_novel_reader,
            subtitleRes = MR.strings.pref_category_novel_reader_summary,
            icon = MaterialSymbols.AutoMirroredRounded.ChromeReaderMode,
            screen = SettingsNovelReaderScreen,
        ),
        // RK <--
        Item(
            paletteKey = "downloads",
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            icon = MaterialSymbols.Rounded.Download,
            screen = SettingsDownloadScreen,
        ),
        Item(
            paletteKey = "tracker",
            // RK: Services holds Suggestions, Related titles and every tracker
            titleRes = MR.strings.pref_category_services,
            subtitleRes = MR.strings.pref_services_summary,
            icon = MaterialSymbols.Rounded.Sync,
            screen = SettingsServicesScreen,
        ),
        Item(
            paletteKey = "services",
            titleRes = MR.strings.pref_category_browse, // RK: "Browse and sources", which holds source settings
            subtitleRes = MR.strings.pref_browse_summary,
            icon = MaterialSymbols.Rounded.Explore,
            screen = SettingsBrowseScreen,
        ),
        Item(
            paletteKey = "backup",
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            icon = MaterialSymbols.Rounded.Storage,
            screen = SettingsDataScreen,
        ),
        Item(
            paletteKey = "storage",
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            icon = MaterialSymbols.Rounded.Security,
            screen = SettingsSecurityScreen,
        ),
        // RK: E-Hentai and MangaDex are reached from Browse and sources now, in its Source settings
        // group, so every source's settings live in one place rather than two.
        Item(
            paletteKey = "services",
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            icon = MaterialSymbols.Rounded.Code,
            screen = SettingsAdvancedScreen,
        ),
        Item(
            paletteKey = "about",
            titleRes = MR.strings.pref_category_about,
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
            },
            icon = MaterialSymbols.Rounded.Info,
            screen = AboutScreen,
        ),
    )
    private data class Section(val titleRes: StringResource, val items: List<Item>)

    private fun entryFor(screen: VoyagerScreen) = items.first { it.screen::class == screen::class }

    // RK: the order and grouping of the landing page
    private val sections: List<Section> by lazy {
        listOf(
            Section(MR.strings.settings_section_personalize, listOf(entryFor(SettingsAppearanceScreen), entryFor(SettingsLibraryScreen))),
            Section(MR.strings.settings_section_reading, listOf(entryFor(SettingsMangaReaderScreen), entryFor(SettingsNovelReaderScreen))),
            Section(
                MR.strings.settings_section_content,
                listOf(entryFor(SettingsBrowseScreen), entryFor(SettingsDownloadScreen), entryFor(SettingsServicesScreen)),
            ),
            Section(
                MR.strings.settings_section_system,
                listOf(entryFor(SettingsDataScreen), entryFor(SettingsSecurityScreen), entryFor(SettingsAdvancedScreen), entryFor(AboutScreen)),
            ),
        )
    }
}
