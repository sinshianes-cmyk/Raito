package eu.kanade.presentation.more.settings.screen.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.HomeTabOrder
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.roundedfilled.Visibility
import mihon.icons.materialsymbols.roundedfilled.VisibilityOff
import raito.presentation.components.rememberSettledReorder
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * The floating bar's tabs as DropSauce draws its section list: a tonal tile per screen with a drag
 * handle to reorder and an eye button to show or hide it. The last visible tab cannot be hidden.
 */
class ScreensOrderScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val prefs = remember { context.appGraph.uiPreferences }
        val raw by prefs.tabOrder.collectAsState()
        val combinedRecents by prefs.combinedRecentsTab.collectAsState()
        val showLibrary by prefs.showLibraryTab.collectAsState()
        val showBrowse by prefs.showBrowseTab.collectAsState()
        val showUpdates by prefs.showUpdatesTab.collectAsState()
        val showHistory by prefs.showHistoryTab.collectAsState()

        fun isShown(key: String) = when (key) {
            "library" -> showLibrary
            "browse" -> showBrowse
            "updates" -> showUpdates || combinedRecents
            else -> showHistory || combinedRecents
        }

        fun setShown(key: String, shown: Boolean) = when (key) {
            "library" -> prefs.showLibraryTab.set(shown)
            "browse" -> prefs.showBrowseTab.set(shown)
            "updates" -> prefs.showUpdatesTab.set(shown)
            else -> prefs.showHistoryTab.set(shown)
        }

        val order = HomeTabOrder.parse(raw)
        // the combined Recents tab is one tab however many of Updates / History are listed
        val recentsCount = if (combinedRecents) 1 else listOf(showUpdates, showHistory).count { it }
        val visibleCount = listOf(showLibrary, showBrowse).count { it } + recentsCount

        val listState = rememberLazyListState()
        val items = remember(order) { order.toMutableStateList() }
        val contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        val reorderState = rememberSettledReorder(items, listState, { it }, contentPadding) { settled ->
            prefs.tabOrder.set(HomeTabOrder.serialize(settled))
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_screens_order),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(paddingValues),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items.size, key = { items[it] }) { index ->
                    val key = items[index]
                    val shown = isShown(key)
                    ReorderableItem(reorderState, key) {
                        ScreenRow(
                            label = stringResource(labelFor(key)),
                            shown = shown,
                            // Recents folds Updates and History together, so their eyes have nothing to switch
                            canToggle = !(combinedRecents && (key == "updates" || key == "history")) &&
                                (!shown || visibleCount > 1),
                            onToggle = { setShown(key, !shown) },
                        )
                    }
                }
            }
        }
    }

    private fun labelFor(key: String) = when (key) {
        "library" -> MR.strings.label_library
        "updates" -> MR.strings.label_recent_updates
        "history" -> MR.strings.history
        else -> MR.strings.browse
    }
}

@Composable
private fun ReorderableCollectionItemScope.ScreenRow(
    label: String,
    shown: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .alpha(if (shown) 1f else 0.5f),
        )
        FilledTonalIconButton(onClick = onToggle, enabled = canToggle) {
            Icon(
                imageVector = if (shown) MaterialSymbols.RoundedFilled.Visibility else MaterialSymbols.RoundedFilled.VisibilityOff,
                contentDescription = stringResource(if (shown) MR.strings.action_hide else MR.strings.action_show),
            )
        }
        Icon(
            imageVector = MaterialSymbols.Rounded.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .draggableHandle(),
        )
    }
}
