package raito.presentation.browse

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import raito.presentation.browse.extension.raitoExtensionsTab
import raito.presentation.browse.feed.raitoFeedTab
import raito.presentation.browse.migrate.raitoMigrateSourceTab
import tachiyomi.i18n.MR

/** Extensions, Migrate and the opt-in Feed, pushed from the Explore page's Manage link. */
class BrowseManageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val browseViewModel = metroViewModel<RaitoBrowseViewModel>()
        val searchQuery by browseViewModel.searchQuery.collectAsStateWithLifecycle()
        val extensionsViewModel = metroViewModel<ExtensionsViewModel>()
        val showFeed by browseViewModel.showFeedTab.collectAsStateWithLifecycle()
        val feedTab = if (showFeed) raitoFeedTab() else null

        val tabs = listOfNotNull(
            raitoExtensionsTab(extensionsViewModel, browseViewModel),
            raitoMigrateSourceTab(browseViewModel),
            feedTab,
        )
        TabbedScreen(
            titleRes = MR.strings.label_extensions,
            tabs = tabs,
            state = rememberPagerState { tabs.size },
            searchQuery = searchQuery,
            onChangeSearchQuery = browseViewModel::search,
            navigateUp = navigator::pop,
        )
    }
}
