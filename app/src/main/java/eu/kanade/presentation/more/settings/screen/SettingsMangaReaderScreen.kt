package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.PreviewDrawing
import eu.kanade.presentation.more.settings.widget.previewArrow
import eu.kanade.presentation.more.settings.widget.previewBlock
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import mihon.app.di.appGraph
import raito.domain.novel.NovelTapLayout
import raito.domain.reader.ChapterTitleFormat
import raito.presentation.reader.NovelTapZones
import raito.presentation.reader.ReaderFlash
import raito.presentation.reader.ReaderRanges
import raito.presentation.reader.autoScrollIntervalPreference
import raito.presentation.reader.autoScrollSpeedPreference
import raito.presentation.reader.hideThresholdPreference
import raito.presentation.reader.navigation.MangaTapLayouts
import raito.presentation.reader.settings.TapGridEditor // RK
import raito.presentation.reader.navigation.setTapLayout
import raito.presentation.reader.railHeightPreference
import raito.presentation.reader.railOnLeftPreference
import raito.presentation.reader.readerBottomButtonsPreference
import raito.presentation.reader.readerFlashGroup
import raito.presentation.reader.readerOrientationChoices
import raito.presentation.reader.volumeKeyScrollPreference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

/**
 * Manga reader settings, a top-level Settings entry beside [SettingsNovelReaderScreen].
 *
 * Every setting here applies to the manga reader only. Where the novel reader has a same-named setting
 * (keep screen on, the volume keys, skip and mark-read-on-skip) the two are deliberately separate values,
 * not twin debt: docs/dev/subsystems/architecture.md. Load ahead only on Wi-Fi has no novel twin either
 * (docs/dev/subsystems/reader.md).
 */
object SettingsMangaReaderScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_manga_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val readerPref = remember { context.appGraph.readerPreferences }

        // RK: the two renderers read different width controls, so the group offers whichever one is
        // live. Side padding is inert under the high quality renderer and Min width under the old one, and
        // so is the volume-key scroll amount, since that renderer's long strip turns whole pages.
        val highQualityRenderer by remember { context.appGraph.basePreferences.highQualityRenderer }
            .collectAsState()
        val loadAheadOnlyOverWifi by readerPref.loadAheadOnlyOverWifi.collectAsState()

        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.defaultReadingMode,
                entries = ReadingMode.entries.drop(1)
                    .associate { it.flagValue to stringResource(it.stringRes) },
                title = stringResource(MR.strings.pref_viewer_type),
                // RK --> DropSauce preview tiles: pages and the direction they turn
                preview = { flag -> readingModePreview(ReadingMode.fromPreference(flag)) },
                // RK <--
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.doubleTapAnimSpeed,
                entries = mapOf(
                    1 to stringResource(MR.strings.double_tap_anim_speed_0),
                    500 to stringResource(MR.strings.double_tap_anim_speed_normal),
                    250 to stringResource(MR.strings.double_tap_anim_speed_fast),
                ),
                title = stringResource(MR.strings.pref_double_tap_anim_speed),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.showReadingMode,
                title = stringResource(MR.strings.pref_show_reading_mode),
                subtitle = stringResource(MR.strings.pref_show_reading_mode_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.showNavigationOverlayOnStart,
                title = stringResource(MR.strings.pref_show_navigation_mode),
                subtitle = stringResource(MR.strings.pref_show_navigation_mode_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.pageTransitions,
                title = stringResource(MR.strings.pref_page_transitions),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.loadAheadOnlyOverWifi,
                title = stringResource(MR.strings.pref_load_ahead_only_over_wifi),
                subtitle = stringResource(MR.strings.pref_load_ahead_only_over_wifi_summary),
            ),
            // On Wi-Fi, or with the switch above off, the whole chapter loads ahead
            Preference.PreferenceItem.SliderPreference(
                preference = readerPref.preloadSize,
                valueRange = ReaderRanges.preloadPages,
                title = stringResource(MR.strings.pref_reader_preload_size),
                valueText = { pluralStringResource(MR.plurals.pref_pages, it, it) },
                visible = loadAheadOnlyOverWifi,
            ),
            getDisplayGroup(readerPreferences = readerPref),
            getEInkGroup(readerPreferences = readerPref),
            getReadingGroup(readerPreferences = readerPref),
            getPagedGroup(readerPreferences = readerPref),
            getWebtoonGroup(readerPreferences = readerPref, highQualityRenderer = highQualityRenderer), // RK
            getNavigationGroup(readerPreferences = readerPref, highQualityRenderer = highQualityRenderer), // RK
            getActionsGroup(readerPreferences = readerPref),
            getEyeReminderGroup(readerPreferences = readerPref), // RK
        )
    }

    // RK -->
    @Composable
    private fun getEyeReminderGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val enabled by readerPreferences.eyeReminder.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_eye_reminder),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.eyeReminder,
                    title = stringResource(MR.strings.pref_eye_reminder),
                    subtitle = stringResource(MR.strings.pref_eye_reminder_summary),
                ),
                Preference.PreferenceItem.SliderPreference(
                    preference = readerPreferences.eyeReminderInterval,
                    valueRange = 5..60,
                    title = stringResource(MR.strings.pref_eye_reminder_interval),
                    valueText = { stringResource(MR.strings.pref_eye_reminder_minutes, it) },
                    visible = enabled,
                ),
            ),
        )
    }
    // RK <--

    @Composable
    private fun getDisplayGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val fullscreen by readerPreferences.fullscreen.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.defaultOrientationType,
                    entries = readerOrientationChoices // RK: one list for both readers
                        .associate { it.flagValue to stringResource(it.stringRes) },
                    title = stringResource(MR.strings.pref_rotation_type),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.readerTheme,
                    entries = mapOf(
                        1 to stringResource(MR.strings.black_background),
                        2 to stringResource(MR.strings.gray_background),
                        0 to stringResource(MR.strings.white_background),
                        3 to stringResource(MR.strings.automatic_background),
                    ),
                    title = stringResource(MR.strings.pref_reader_theme),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.fullscreen,
                    title = stringResource(MR.strings.pref_fullscreen),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.drawUnderCutout,
                    title = stringResource(MR.strings.pref_cutout_short),
                    visible = LocalView.current.hasDisplayCutout() && fullscreen,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.keepScreenOn,
                    title = stringResource(MR.strings.pref_keep_screen_on),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.showPageNumber,
                    title = stringResource(MR.strings.pref_show_page_number),
                ),
                // RK -->
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.chapterTitleFormat,
                    entries = ChapterTitleFormat.entries.associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_chapter_title_format),
                ),
                // RK <--
            ),
        )
    }

    // The group the novel reader's screen shows too, over the manga reader's own flash settings.
    @Composable
    private fun getEInkGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup =
        readerFlashGroup(ReaderFlash.manga(readerPreferences))

    @Composable
    private fun getReadingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_reading),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.skipRead,
                    title = stringResource(MR.strings.pref_skip_read_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.skipFiltered,
                    title = stringResource(MR.strings.pref_skip_filtered_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.skipDupe,
                    title = stringResource(MR.strings.pref_skip_dupe_chapters),
                ),
                // RK --> content-type reader prefs inserted before the upstream chapter-transition row
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.openInNewTask,
                    title = stringResource(MR.strings.pref_reader_new_task),
                    subtitle = stringResource(MR.strings.pref_reader_new_task_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.markReadOnSkip,
                    title = stringResource(MR.strings.pref_mark_read_on_skip),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.autoWebtoonMode,
                    title = stringResource(MR.strings.pref_auto_webtoon_mode),
                    subtitle = stringResource(MR.strings.pref_auto_webtoon_mode_summary),
                ),
                readerBottomButtonsPreference(ReaderBottomButton.BarPreferences.manga(readerPreferences)),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.preserveReadingPosition,
                    title = stringResource(MR.strings.pref_preserve_reading_position),
                    subtitle = stringResource(MR.strings.pref_preserve_reading_position_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.autoScrollOnOpen,
                    title = stringResource(MR.strings.pref_auto_scroll_on_open),
                ),
                autoScrollIntervalPreference(readerPreferences.autoScrollInterval),
                autoScrollSpeedPreference(
                    readerPreferences.autoScrollSpeed,
                    subtitle = stringResource(MR.strings.pref_volume_keys_scroll_amount_long_strip),
                ),
                // RK <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.alwaysShowChapterTransition,
                    title = stringResource(MR.strings.pref_always_show_chapter_transition),
                ),
            ),
        )
    }

    @Composable
    private fun getPagedGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val navModePref = readerPreferences.navigationModePager
        val imageScaleTypePref = readerPreferences.imageScaleType
        val dualPageSplitPref = readerPreferences.dualPageSplitPaged
        val rotateToFitPref = readerPreferences.dualPageRotateToFit

        val navMode by navModePref.collectAsState()
        val imageScaleType by imageScaleTypePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pager_viewer),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = navModePref,
                    // RK: Mihon's layouts, then the ones the novel reader brought
                    entries = MangaTapLayouts.names
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_viewer_nav),
                    // RK --> stored through the helper, which also drops an inversion the new layout cannot draw
                    onValueChanged = {
                        readerPreferences.setTapLayout(pager = true, it)
                        false
                    },
                    // RK <--
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.pagerNavInverted,
                    // RK: only the inversions that change the layout
                    entries = MangaTapLayouts.invertModes(navMode)
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                    visible = MangaTapLayouts.offersInvert(navMode), // RK
                ),
                // RK -->
                Preference.PreferenceItem.SliderPreference(
                    preference = readerPreferences.tapBottomZoneHeight,
                    valueRange = NovelTapZones.BOTTOM_ZONE_PERCENT,
                    title = stringResource(MR.strings.pref_tap_bottom_zone_height),
                    valueText = { "$it%" },
                    visible = MangaTapLayouts.added(navMode) == NovelTapLayout.BOTTOM,
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_tap_grid),
                    visible = MangaTapLayouts.added(navMode) == NovelTapLayout.CUSTOM,
                ) {
                    TapGridEditor(readerPreferences.tapCustomGrid)
                },
                // RK <--
                Preference.PreferenceItem.ListPreference(
                    preference = imageScaleTypePref,
                    entries = ReaderPreferences.ImageScaleType
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_image_scale_type),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.zoomStart,
                    entries = ReaderPreferences.ZoomStart
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_zoom_start),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.cropBorders,
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.landscapeZoom,
                    title = stringResource(MR.strings.pref_landscape_zoom),
                    visible = imageScaleType == 1,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.navigateToPan,
                    title = stringResource(MR.strings.pref_navigate_pan),
                    visible = navMode != 5,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = dualPageSplitPref,
                    title = stringResource(MR.strings.pref_dual_page_split),
                    onValueChanged = {
                        rotateToFitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageInvertPaged,
                    title = stringResource(MR.strings.pref_dual_page_invert),
                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                    visible = dualPageSplit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = rotateToFitPref,
                    title = stringResource(MR.strings.pref_page_rotate),
                    onValueChanged = {
                        dualPageSplitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageRotateToFitInvert,
                    title = stringResource(MR.strings.pref_page_rotate_invert),
                    visible = rotateToFit,
                ),
            ),
        )
    }

    @Composable
    private fun getWebtoonGroup(
        readerPreferences: ReaderPreferences,
        highQualityRenderer: Boolean, // RK
    ): Preference.PreferenceGroup {
        val numberFormat = remember { NumberFormat.getPercentInstance() }

        val navModePref = readerPreferences.navigationModeWebtoon
        val dualPageSplitPref = readerPreferences.dualPageSplitWebtoon
        val rotateToFitPref = readerPreferences.dualPageRotateToFitWebtoon

        val navMode by navModePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.webtoon_viewer),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = navModePref,
                    // RK: Mihon's layouts, then the ones the novel reader brought
                    entries = MangaTapLayouts.names
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_viewer_nav),
                    // RK --> stored through the helper, which also drops an inversion the new layout cannot draw
                    onValueChanged = {
                        readerPreferences.setTapLayout(pager = false, it)
                        false
                    },
                    // RK <--
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.webtoonNavInverted,
                    // RK: only the inversions that change the layout
                    entries = MangaTapLayouts.invertModes(navMode)
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                    visible = MangaTapLayouts.offersInvert(navMode), // RK
                ),
                // RK -->
                Preference.PreferenceItem.SliderPreference(
                    preference = readerPreferences.tapBottomZoneHeight,
                    valueRange = NovelTapZones.BOTTOM_ZONE_PERCENT,
                    title = stringResource(MR.strings.pref_tap_bottom_zone_height),
                    valueText = { "$it%" },
                    visible = MangaTapLayouts.added(navMode) == NovelTapLayout.BOTTOM,
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_tap_grid),
                    visible = MangaTapLayouts.added(navMode) == NovelTapLayout.CUSTOM,
                ) {
                    TapGridEditor(readerPreferences.tapCustomGrid)
                },
                // RK <--
                // RK --> Min width under the high quality renderer, upstream's side padding otherwise
                if (highQualityRenderer) {
                    Preference.PreferenceItem.SliderPreference(
                        preference = readerPreferences.continuousMinWidth,
                        valueRange = 1..100,
                        title = stringResource(MR.strings.pref_continuous_minwidth),
                        valueText = { numberFormat.format(it / 100f) },
                    )
                } else {
                    Preference.PreferenceItem.SliderPreference(
                        preference = readerPreferences.webtoonSidePadding,
                        valueRange = ReaderPreferences.let {
                            it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX
                        },
                        title = stringResource(MR.strings.pref_webtoon_side_padding),
                        valueText = { numberFormat.format(it / 100f) },
                    )
                },
                // RK <--
                Preference.PreferenceItem.SliderPreference(
                    preference = readerPreferences.webtoonPageGap,
                    valueRange = 0..24,
                    title = stringResource(MR.strings.pref_webtoon_page_gap),
                    valueText = { "$it dp" },
                ),
                hideThresholdPreference(readerPreferences.readerHideThreshold), // RK: the row novels share
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.cropBordersWebtoon,
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = dualPageSplitPref,
                    title = stringResource(MR.strings.pref_dual_page_split),
                    onValueChanged = {
                        rotateToFitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageInvertWebtoon,
                    title = stringResource(MR.strings.pref_dual_page_invert),
                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                    visible = dualPageSplit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = rotateToFitPref,
                    title = stringResource(MR.strings.pref_page_rotate),
                    onValueChanged = {
                        dualPageSplitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageRotateToFitInvertWebtoon,
                    title = stringResource(MR.strings.pref_page_rotate_invert),
                    visible = rotateToFit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.webtoonDoubleTapZoomEnabled,
                    title = stringResource(MR.strings.pref_double_tap_zoom),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.webtoonDisableZoomOut,
                    title = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
                ),
            ),
        )
    }

    @Composable
    private fun getNavigationGroup(
        readerPreferences: ReaderPreferences,
        highQualityRenderer: Boolean, // RK
    ): Preference.PreferenceGroup {
        val readWithVolumeKeysPref = readerPreferences.readWithVolumeKeys
        val readWithVolumeKeys by readWithVolumeKeysPref.collectAsState()

        val verticalNavigator by readerPreferences.verticalNavigator.collectAsState()
        val showNavigator by readerPreferences.showNavigator.collectAsState() // RK

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_navigation),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readWithVolumeKeysPref,
                    title = stringResource(MR.strings.pref_read_with_volume_keys),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.readWithVolumeKeysInverted,
                    title = stringResource(MR.strings.pref_read_with_volume_keys_inverted),
                    visible = readWithVolumeKeys,
                ),
                // RK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.invertDirectionalKeys,
                    title = stringResource(MR.strings.pref_invert_directional_keys),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.ergonomicDirectionalKeys,
                    title = stringResource(MR.strings.pref_ergonomic_directional_keys),
                    subtitle = stringResource(MR.strings.pref_ergonomic_directional_keys_summary),
                ),
                // RK <--
                // RK: volume-key scroll amount (long-strip viewers), novel-reader parity
                volumeKeyScrollPreference(
                    readerPreferences.readWithVolumeKeysScrollAmount,
                    visible = readWithVolumeKeys && !highQualityRenderer,
                    subtitle = stringResource(MR.strings.pref_volume_keys_scroll_amount_long_strip),
                ),
                // RK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.showNavigator,
                    title = stringResource(MR.strings.pref_show_progress_navigator),
                    subtitle = stringResource(MR.strings.pref_show_progress_navigator_summary),
                ),
                // RK <--
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = readerPreferences.verticalNavigator,
                    entries = ReadingMode.entries.filter { it != ReadingMode.DEFAULT }
                        .associate { it to stringResource(it.stringRes) },
                    title = stringResource(MR.strings.pref_vertical_navigator),
                    visible = showNavigator, // RK
                ),
                // RK --> the rail rows novels share
                railOnLeftPreference(
                    readerPreferences.verticalNavigatorOnLeft,
                    visible = showNavigator && verticalNavigator.isNotEmpty(),
                ),
                railHeightPreference(
                    readerPreferences.verticalNavigatorHeight,
                    visible = showNavigator && verticalNavigator.isNotEmpty(),
                ),
                // RK <--
            ),
        )
    }

    @Composable
    private fun getActionsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        // RK --> default folder for saved pages
        val context = LocalContext.current
        val saveDir by readerPreferences.pageSaveDirectory.collectAsState()
        val pickDir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                readerPreferences.pageSaveDirectory.set(uri.toString())
            }
        }
        // RK <--
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_actions),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.readWithLongTap,
                    title = stringResource(MR.strings.pref_read_with_long_tap),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.folderPerManga,
                    title = stringResource(MR.strings.pref_create_folder_per_manga),
                    subtitle = stringResource(MR.strings.pref_create_folder_per_manga_summary),
                ),
                // RK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.pageSaveAsk,
                    title = stringResource(MR.strings.pref_page_save_ask),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_page_save_directory),
                    subtitle = saveDir.ifEmpty { stringResource(MR.strings.pref_page_save_directory_default) },
                    onClick = { pickDir.launch(null) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_page_save_directory_reset),
                    visible = saveDir.isNotEmpty(),
                    onClick = { readerPreferences.pageSaveDirectory.delete() },
                ),
                // RK <--
            ),
        )
    }
}

// RK --> DropSauce's reader-mode drawings, one per mode (it draws four; Mihon has a fifth, the gapped strip)
private fun readingModePreview(mode: ReadingMode): PreviewDrawing = { palette ->
    val page = palette.page
    val accent = palette.accent
    when (mode) {
        ReadingMode.LEFT_TO_RIGHT, ReadingMode.RIGHT_TO_LEFT, ReadingMode.DEFAULT -> {
            previewBlock(page, 0.12f, 0.2f, 0.35f, 0.6f)
            previewBlock(page, 0.53f, 0.2f, 0.35f, 0.6f)
            if (mode == ReadingMode.RIGHT_TO_LEFT) {
                previewArrow(accent, 0.7f, 0.5f, 0.3f, 0.5f)
            } else {
                previewArrow(accent, 0.3f, 0.5f, 0.7f, 0.5f)
            }
        }
        ReadingMode.VERTICAL -> {
            previewBlock(page, 0.22f, 0.1f, 0.56f, 0.37f)
            previewBlock(page, 0.22f, 0.53f, 0.56f, 0.37f)
            previewArrow(accent, 0.5f, 0.3f, 0.5f, 0.7f)
        }
        ReadingMode.WEBTOON -> {
            // One endless strip, no gaps between pages.
            previewBlock(page, 0.27f, 0f, 0.46f, 1f, radius = 0f)
            previewArrow(accent, 0.5f, 0.3f, 0.5f, 0.7f)
        }
        ReadingMode.CONTINUOUS_VERTICAL -> {
            // The same strip, with a gap between the pages.
            previewBlock(page, 0.27f, 0f, 0.46f, 0.46f, radius = 0f)
            previewBlock(page, 0.27f, 0.54f, 0.46f, 0.46f, radius = 0f)
            previewArrow(accent, 0.5f, 0.3f, 0.5f, 0.7f)
        }
    }
}
// RK <--
