package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.toast
import mihon.app.di.appGraph
import raito.domain.suggestions.SuggestionsPreferences
import raito.presentation.recommendation.SettingsRecommendationsScreen
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Settings -> Services: everything that talks to an outside service in one place. Suggestions (the
 * Browse row), the related-titles carousel with its sources, and every tracker's login and sync.
 * Each part keeps its own definition and is only assembled here, so a toggle changed here is the
 * same preference the old Tracking and Browse screens wrote.
 */
object SettingsServicesScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_services

    @Composable
    override fun RowScope.AppBarAction() {
        with(SettingsTrackingScreen) { AppBarAction() }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val suggestionsPreferences = remember { context.appGraph.suggestionsPreferences }
        return listOf<Preference>(suggestionsGroup(suggestionsPreferences)) +
            SettingsRecommendationsScreen.getPreferences() +
            SettingsTrackingScreen.getPreferences()
    }

    @Composable
    private fun suggestionsGroup(prefs: SuggestionsPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val repository = remember { context.appGraph.suggestionsRepository }
        val scope = rememberCoroutineScope()
        val enabled by prefs.enabled.collectAsState()
        val refreshing = stringResource(MR.strings.pref_suggestions_refreshing)
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_suggestions_group),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = prefs.enabled,
                    title = stringResource(MR.strings.pref_suggestions),
                    subtitle = stringResource(MR.strings.pref_suggestions_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = prefs.hideInLibrary,
                    title = stringResource(MR.strings.pref_suggestions_hide_in_library),
                    visible = enabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = prefs.hideNsfw,
                    title = stringResource(MR.strings.pref_suggestions_hide_nsfw),
                    subtitle = stringResource(MR.strings.pref_suggestions_hide_nsfw_summary),
                    visible = enabled,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_suggestions_refresh),
                    subtitle = stringResource(MR.strings.pref_suggestions_refresh_summary),
                    visible = enabled,
                    onClick = {
                        context.toast(refreshing)
                        scope.launchIO { repository.refresh(force = true) }
                    },
                ),
            ),
        )
    }
}
