package raito.presentation.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.ui.setting.SettingsScreen

/** What the top bar's gear opens. */
@Composable
fun rememberOpenSettings(): () -> Unit {
    val navigator = LocalNavigator.currentOrThrow
    return remember(navigator) { { navigator.push(SettingsScreen()) } }
}
