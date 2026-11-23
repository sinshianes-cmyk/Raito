package eu.kanade.tachiyomi.ui.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Tab
import eu.kanade.presentation.util.rememberAnimatedVectorPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import raito.presentation.browse.BrowseManageScreen
import raito.presentation.browse.RaitoBrowseViewModel
import raito.presentation.browse.RaitoExplore
import raito.presentation.browse.globalsearch.EntryGlobalSearchScreen
import raito.presentation.components.cachedAnimatedVector
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val enter = cachedAnimatedVector(R.drawable.animated_browse_enter) // RK
            val exit = cachedAnimatedVector(R.drawable.animated_browse_exit) // RK
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(enter, exit, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(EntryGlobalSearchScreen()) // RK: the shared manga + novel global search
    }

    private val switchToExtensionTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToExtensionTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // RK: Explore page, the shared content-type filter and the LN update badge come from here
        val browseViewModel = metroViewModel<RaitoBrowseViewModel>()
        RaitoExplore(browseViewModel)

        LaunchedEffect(Unit) {
            switchToExtensionTabChannel.receiveAsFlow()
                .collectLatest { navigator.push(BrowseManageScreen()) }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
