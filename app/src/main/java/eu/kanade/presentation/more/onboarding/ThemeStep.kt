package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import androidx.compose.foundation.BorderStroke
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal class ThemeStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }

        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingSectionLabel(stringResource(MR.strings.pref_category_theme))
            ThemeCard {
                AppThemeModePreferenceWidget(
                    value = themeMode,
                    onItemClick = {
                        themeModePref.set(it)
                        setAppCompatDelegateThemeMode(it)
                    },
                )
            }

            OnboardingSectionLabel(stringResource(MR.strings.pref_app_theme), Modifier.padding(top = 8.dp))
            ThemeCard {
                AppThemePreferenceWidget(
                    value = appTheme,
                    amoled = amoled,
                    onItemClick = { appThemePref.set(it) },
                )
            }
        }
    }

    // The widgets bring their own padding, so the card here only supplies the surface.
    @Composable
    private fun ThemeCard(content: @Composable () -> Unit) {
        Surface(
            shape = OnboardingCardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
        }
    }
}
