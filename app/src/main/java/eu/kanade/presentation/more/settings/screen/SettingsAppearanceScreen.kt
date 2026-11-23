package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.icerock.moko.resources.StringResource
import cafe.adriel.voyager.navigator.currentOrThrow
import com.materialkolor.PaletteStyle
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppDisplaySize
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.util.system.toast
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.appGraph
import raito.domain.category.seedRecentsSurfaceFromUpdates
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsAppearanceScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }

        // RK: the old Display and Interface groups mixed navigation, formats and system toggles, so
        // their rows are redistributed into groups that each hold one kind of setting
        val display = getDisplayGroup(uiPreferences = uiPreferences).preferenceItems
        val interfaceItems = getInterfaceGroup(uiPreferences = uiPreferences).preferenceItems
        val all = display + interfaceItems
        // resolved up front: stringResource is composable, so it cannot run inside a plain local function
        val titleOf = listOf(
            MR.strings.pref_screens_order,
            MR.strings.pref_nav_labels,
            MR.strings.pref_bar_blur,
            MR.strings.pref_nav_legacy,
            MR.strings.pref_combined_recents_tab,
            MR.strings.pref_show_updates_tab,
            MR.strings.pref_show_history_tab,
            MR.strings.pref_show_continue_button,
            MR.strings.pref_recent_shortcuts,
            MR.strings.pref_app_language,
            MR.strings.pref_date_format,
            MR.strings.pref_time_format,
            MR.strings.pref_relative_format,
        ).associateWith { stringResource(it) }
        fun pick(vararg titles: StringResource) = titles.mapNotNull { res ->
            all.firstOrNull { it.title == titleOf[res] }
        }
        val navigation = pick(
            MR.strings.pref_screens_order,
            MR.strings.pref_nav_labels,
            MR.strings.pref_bar_blur,
            MR.strings.pref_nav_legacy,
            MR.strings.pref_combined_recents_tab,
            MR.strings.pref_show_updates_tab,
            MR.strings.pref_show_history_tab,
            MR.strings.pref_show_continue_button,
            MR.strings.pref_recent_shortcuts,
        )
        val language = pick(
            MR.strings.pref_app_language,
            MR.strings.pref_date_format,
            MR.strings.pref_time_format,
            MR.strings.pref_relative_format,
        )
        val placed = (navigation + language).toSet()
        val system = all.filter { it !in placed }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            Preference.PreferenceGroup(title = stringResource(MR.strings.pref_category_navigation), preferenceItems = navigation),
            getLibraryListGroup(), // RK: DropSauce's "manga list" group
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_details),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.collapseDescription,
                        title = stringResource(MR.strings.pref_collapse_description),
                        subtitle = stringResource(MR.strings.pref_collapse_description_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(title = stringResource(MR.strings.pref_category_language_formats), preferenceItems = language),
            Preference.PreferenceGroup(title = stringResource(MR.strings.pref_category_interface), preferenceItems = system),
            getPrivacyGroup(),
        )
    }

    // RK --> DropSauce's list group, wired to the library's own display preferences
    @Composable
    private fun getLibraryListGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val libraryPreferences = remember { context.appGraph.libraryPreferences }
        val auto = stringResource(MR.strings.label_auto)
        val columnsText: @Composable (Int) -> String = { if (it == 0) auto else it.toString() }
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_library_list),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.displayMode,
                    entries = mapOf(
                        tachiyomi.domain.library.model.LibraryDisplayMode.CompactGrid to
                            stringResource(MR.strings.action_display_grid),
                        tachiyomi.domain.library.model.LibraryDisplayMode.ComfortableGrid to
                            stringResource(MR.strings.action_display_comfortable_grid),
                        tachiyomi.domain.library.model.LibraryDisplayMode.ComfortableGridPanorama to
                            stringResource(MR.strings.action_display_comfortable_grid_panorama),
                        tachiyomi.domain.library.model.LibraryDisplayMode.List to
                            stringResource(MR.strings.action_display_list),
                        tachiyomi.domain.library.model.LibraryDisplayMode.CoverOnlyGrid to
                            stringResource(MR.strings.action_display_cover_only_grid),
                    ),
                    title = stringResource(MR.strings.action_display_mode),
                ),
                Preference.PreferenceItem.SliderPreference(
                    preference = libraryPreferences.portraitColumns,
                    title = stringResource(MR.strings.pref_library_columns) + " (" +
                        stringResource(MR.strings.portrait) + ")",
                    valueRange = 0..10,
                    valueText = columnsText,
                ),
                Preference.PreferenceItem.SliderPreference(
                    preference = libraryPreferences.landscapeColumns,
                    title = stringResource(MR.strings.pref_library_columns) + " (" +
                        stringResource(MR.strings.landscape) + ")",
                    valueRange = 0..10,
                    valueText = columnsText,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.unreadBadge,
                    title = stringResource(MR.strings.action_display_unread_badge),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.downloadBadge,
                    title = stringResource(MR.strings.action_display_download_badge),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.localBadge,
                    title = stringResource(MR.strings.action_display_local_badge),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.languageBadge,
                    title = stringResource(MR.strings.action_display_language_badge),
                ),
            ),
        )
    }

    @Composable
    private fun getPrivacyGroup(): Preference.PreferenceGroup {
        val navigator = LocalNavigator.currentOrThrow
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_privacy_short),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_category_security),
                    subtitle = stringResource(MR.strings.pref_privacy_link_summary),
                    onClick = { navigator.push(SettingsSecurityScreen) },
                ),
            ),
        )
    }
    // RK <--

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        // RK -->
        val themeCoverBasedPref = uiPreferences.themeCoverBased
        val themeCoverBased by themeCoverBasedPref.collectAsState()
        // RK <--

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
                    Column {
                        AppThemeModePreferenceWidget(
                            value = themeMode,
                            onItemClick = {
                                themeModePref.set(it)
                                setAppCompatDelegateThemeMode(it)
                            },
                        )

                        AppThemePreferenceWidget(
                            value = appTheme,
                            amoled = amoled,
                            onItemClick = { appThemePref.set(it) },
                        )
                    }
                },
                // RK -->
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.appDisplaySize,
                    entries = AppDisplaySize.entries.associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_app_display_size),
                    subtitle = null,
                    segmented = true,
                ),
                // RK <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = amoledPref,
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                    visible = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        true
                    },
                ),
                // RK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = themeCoverBasedPref,
                    title = stringResource(MR.strings.pref_theme_cover_based),
                    subtitle = stringResource(MR.strings.pref_theme_cover_based_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.themeCoverBasedStyle,
                    entries = PaletteStyle.entries.associateWith { it.name },
                    title = stringResource(MR.strings.pref_theme_cover_based_style),
                    visible = themeCoverBased,
                ),
                // RK <--
            ),
        )
    }

    // RK -->
    @Composable
    private fun getInterfaceGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_interface),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.hapticFeedback,
                    title = stringResource(MR.strings.pref_haptic_feedback),
                    subtitle = stringResource(MR.strings.pref_haptic_feedback_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.hideStatusBar,
                    title = stringResource(MR.strings.pref_hide_status_bar),
                    subtitle = stringResource(MR.strings.pref_hide_status_bar_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.barBlur,
                    title = stringResource(MR.strings.pref_bar_blur),
                    subtitle = stringResource(MR.strings.pref_bar_blur_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.navLegacy,
                    title = stringResource(MR.strings.pref_nav_legacy),
                    subtitle = stringResource(MR.strings.pref_nav_legacy_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.navLabels,
                    title = stringResource(MR.strings.pref_nav_labels),
                    subtitle = stringResource(MR.strings.pref_nav_labels_summary),
                ),
                // RK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.recentShortcuts,
                    title = stringResource(MR.strings.pref_recent_shortcuts),
                    subtitle = stringResource(MR.strings.pref_recent_shortcuts_summary),
                ),
                // RK <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.exitConfirmation,
                    title = stringResource(MR.strings.pref_exit_confirmation),
                    subtitle = stringResource(MR.strings.pref_exit_confirmation_summary),
                ),
            ),
        )
    }
    // RK <--

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val locale = LocalLocale.current
        val is24HourSystem = rememberIs24HourSystem()

        val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime() }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val combinedRecents by uiPreferences.combinedRecentsTab.collectAsState() // RK
        val formattedDateNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat, locale.platformLocale).format(now)
        }

        // RK: the combined Recents tab's seed reads and writes this surface's own keys.
        val sourcePreferences = remember { context.appGraph.raitoSourcePreferences }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode,
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat,
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it, locale.platformLocale).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        },
                    title = stringResource(MR.strings.pref_date_format),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.timeFormat,
                    entries = TimeFormat.entries
                        .associateWith {
                            val formattedTime = UiPreferences.timeFormat(
                                it,
                                locale.platformLocale,
                                is24HourSystem,
                            ).format(now)
                            "${stringResource(it.titleRes)} ($formattedTime)"
                        },
                    title = stringResource(MR.strings.pref_time_format),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.relativeTime,
                    title = stringResource(MR.strings.pref_relative_format),
                    subtitle = stringResource(
                        MR.strings.pref_relative_format_summary,
                        stringResource(MR.strings.relative_time_today),
                        formattedDateNow,
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription,
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
                // RK --> Updates and History as one Recents tab. Turning it on hands the combined
                // surface the filter Updates was using, once, so the feed does not quietly widen.
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.combinedRecentsTab,
                    title = stringResource(MR.strings.pref_combined_recents_tab),
                    subtitle = stringResource(MR.strings.pref_combined_recents_tab_summary),
                    onValueChanged = { enabled ->
                        if (enabled) sourcePreferences.seedRecentsSurfaceFromUpdates()
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.showContinueButton,
                    title = stringResource(MR.strings.pref_show_continue_button),
                    subtitle = stringResource(MR.strings.pref_show_continue_button_summary),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_screens_order),
                    subtitle = stringResource(MR.strings.pref_screens_order_summary),
                    onClick = { navigator.push(eu.kanade.presentation.more.settings.screen.appearance.ScreensOrderScreen()) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.showUpdatesTab,
                    title = stringResource(MR.strings.pref_show_updates_tab),
                    visible = !combinedRecents,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.showHistoryTab,
                    title = stringResource(MR.strings.pref_show_history_tab),
                    visible = !combinedRecents,
                ),
                // RK <--
            ),
        )
    }
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)
