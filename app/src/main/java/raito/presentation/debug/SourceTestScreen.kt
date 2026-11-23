package raito.presentation.debug

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.Autorenew
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.util.plus

/** Runs each installed manga source through popular, latest, search, details, chapters and pages. */
class SourceTestScreen : Screen() {

    companion object {
        const val TITLE = "Source tests"
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<SourceTestViewModel>()
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = TITLE,
                    navigateUp = navigator::pop,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = if (state.running) "Stop" else "Run tests",
                                    icon = MaterialSymbols.Rounded.Autorenew,
                                    onClick = viewModel::toggleRun,
                                ),
                                AppBar.Action(
                                    title = "Copy report",
                                    icon = MaterialSymbols.Rounded.ContentCopy,
                                    onClick = { context.copyToClipboard(TITLE, state.report()) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            LazyColumn(contentPadding = contentPadding + PaddingValues(horizontal = ScreenHorizontalPadding)) {
                item {
                    Text(
                        text = state.summary(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(state.results, key = { it.sourceId }) { result ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = "${result.status.label}  ${result.sourceName} (${result.lang})",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        result.stages.forEach { stage ->
                            Text(
                                text = stage.line(),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}
