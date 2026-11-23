package eu.kanade.domain.ui

import android.text.format.DateFormat
import com.materialkolor.PaletteStyle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.ui.model.AppDisplaySize
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.util.system.logcat

@Inject
@SingleIn(AppScope::class)
class UiPreferences(
    preferenceStore: PreferenceStore,
) {

    val appDisplaySize: Preference<AppDisplaySize> = preferenceStore.getEnum("pref_app_display_size", AppDisplaySize.MEDIUM) // RK

    val themeMode: Preference<ThemeMode> = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    val appTheme: Preference<AppTheme> = preferenceStore.getEnum(
        "pref_app_theme",
        AppTheme.DS_TOTORO, // RK: DropSauce's default palette
    )

    val themeDarkAmoled: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    val relativeTime: Preference<Boolean> = preferenceStore.getBoolean("relative_time_v2", true)

    val dateFormat: Preference<String> = preferenceStore.getString("app_date_format", "")

    val timeFormat: Preference<TimeFormat> = preferenceStore.getEnum("app_time_format", TimeFormat.FOLLOW_LANGUAGE)

    val tabletUiMode: Preference<TabletUiMode> = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    val imagesInDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_render_images_description", true)

    // RK --> bottom bar tabs a user can hide; they only apply while Updates and History are separate
    val showUpdatesTab: Preference<Boolean> = preferenceStore.getBoolean("pref_show_updates_tab", true)
    val showHistoryTab: Preference<Boolean> = preferenceStore.getBoolean("pref_show_history_tab", true)
    val showLibraryTab: Preference<Boolean> = preferenceStore.getBoolean("pref_show_library_tab", true) // RK
    val showBrowseTab: Preference<Boolean> = preferenceStore.getBoolean("pref_show_browse_tab", true) // RK
    // RK <--

    // RK --> the order of the floating bar's tabs, as comma-separated keys (see HomeTabOrder)
    val tabOrder: Preference<String> = preferenceStore.getString("pref_tab_order", "library,updates,history,browse")
    // RK <--

    // RK --> Updates and History as one Recents tab, off so nothing moves until it is asked for
    // RK: a floating button on the library that reopens the series read last
    val showContinueButton: Preference<Boolean> = preferenceStore.getBoolean("pref_show_continue_button", false)

    val combinedRecentsTab: Preference<Boolean> = preferenceStore.getBoolean("pref_combined_recents_tab", false)
    // RK <--

    // RK --> cover-based theming
    val themeCoverBased: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_cover_based_key", true)

    val themeCoverBasedStyle: Preference<PaletteStyle> =
        preferenceStore.getEnum("pref_theme_cover_based_style_key", PaletteStyle.Vibrant)

    val coverVibrantColors: Preference<Set<String>> = preferenceStore.getStringSet("cover_vibrant_colors", emptySet())

    // How many rows of adult-source page-preview thumbnails to show on details (0 = off).
    val previewsRowCount: Preference<Int> = preferenceStore.getInt("pref_previews_row_count", 4)
    // RK <--

    // RK --> DropSauce interface switches
    val hapticFeedback: Preference<Boolean> = preferenceStore.getBoolean("pref_haptic_feedback", true)

    val hideStatusBar: Preference<Boolean> = preferenceStore.getBoolean("pref_hide_status_bar", false)

    /** On: every bottom tab shows its label. Off: only the selected tab does. */
    /** On: content under the floating bar is progressively blurred (Android 13+, a soft fade before). */
    val barBlur: Preference<Boolean> = preferenceStore.getBoolean("pref_bar_blur", true)

    /** On: the classic full-width bottom bar instead of the floating pill. */
    val navLegacy: Preference<Boolean> = preferenceStore.getBoolean("pref_nav_legacy", false)

    /** On: long descriptions open collapsed on the details screen; off opens them in full. */
    val collapseDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_collapse_description", true)

    val navLabels: Preference<Boolean> = preferenceStore.getBoolean("pref_nav_labels", false)

    // RK: up to three launcher shortcuts to the most recently read manga. Off by default, since the
    // launcher shows titles outside the app's own lock.
    val recentShortcuts: Preference<Boolean> = preferenceStore.getBoolean("pref_recent_shortcuts", false)

    // RK: draw the Updates tab badge as a plain dot instead of the unread count
    val updatesBadgeDot: Preference<Boolean> = preferenceStore.getBoolean("pref_updates_badge_dot", false)

    val exitConfirmation: Preference<Boolean> = preferenceStore.getBoolean("pref_exit_confirmation", false)
    // RK <--

    companion object {
        fun dateFormat(format: String, locale: Locale): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            else -> DateTimeFormatter.ofPattern(format, locale)
        }

        fun timeFormat(timeFormat: TimeFormat, locale: Locale, is24HourSystem: Boolean): DateTimeFormatter {
            val use24Hours = when (timeFormat) {
                TimeFormat.FOLLOW_LANGUAGE -> {
                    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
                }

                TimeFormat.FOLLOW_SYSTEM -> is24HourSystem
                TimeFormat.HOURS_24 -> true
                TimeFormat.HOURS_12 -> false
            }

            val skeleton = if (use24Hours) "Hm" else "hm"
            val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
            return try {
                DateTimeFormatter.ofPattern(pattern, locale)
            } catch (e: IllegalArgumentException) {
                val fallbackPattern = if (use24Hours) "HH:mm" else "h:mm a"
                logcat(LogPriority.ERROR, e) {
                    "Failed to apply best pattern ('$pattern'), falling back to '$fallbackPattern'"
                }
                DateTimeFormatter.ofPattern(fallbackPattern, locale)
            }
        }

        fun dateTimeFormat(
            dateFormat: String,
            timeFormat: TimeFormat,
            locale: Locale,
            is24HourSystem: Boolean,
        ): DateTimeFormatter {
            return DateTimeFormatterBuilder()
                .append(dateFormat(dateFormat, locale))
                .appendLiteral(' ')
                .append(timeFormat(timeFormat, locale, is24HourSystem))
                .toFormatter(locale)
        }
    }
}
