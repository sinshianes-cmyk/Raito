package raito.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState

/** The incognito switch the overflow menus show, backed by the same preference the More tab flips. */
class IncognitoToggle(val enabled: Boolean, val toggle: () -> Unit)

@Composable
fun rememberIncognitoToggle(): IncognitoToggle {
    val context = LocalContext.current
    val preference = remember { context.appGraph.basePreferences.incognitoMode }
    val enabled by preference.collectAsState()
    return IncognitoToggle(enabled = enabled, toggle = { preference.set(!enabled) })
}
