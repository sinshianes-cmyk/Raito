package raito.presentation.recents

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadIndicator
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.manga.components.getSwipeAction
import eu.kanade.presentation.manga.components.swipeActionThreshold
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.download.model.Download
import me.saket.swipe.SwipeableActionsBox
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Schedule
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import mihon.icons.materialsymbols.roundedfilled.Circle
import raito.presentation.components.readProgressLabel
import raito.presentation.theme.Dimens
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground

/*
 * The rows only a recents feed draws: a collapsed series group, its children, and the digest's
 * section chrome. RecentsCombinedRow below is the flat row every mode but Updates draws, History
 * included; Updates keeps EntryUpdatesRow, which it shares with the screen it replaced.
 */

/**
 * The height the flat row draws at in every mode but Updates, taken from History so a mixed feed is
 * uniform: a 108dp cover with 4dp above and below it, the History card's own measurements.
 */
val RECENTS_ROW_HEIGHT = 116.dp

/**
 * Collapsed "N new chapters" row for a series with several updates on one day. Only the Updates mode
 * groups, so this sits among EntryUpdatesRow's single rows and takes their height and square cover: a
 * group at a different height than the row beside it is the spliced-lists effect one shape avoids.
 */
@Composable
fun RecentsGroupRow(
    cover: Any?,
    title: String,
    count: Int,
    expanded: Boolean,
    selected: Boolean,
    anyUnread: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val textAlpha = if (anyUnread) 1f else DISABLED_ALPHA
    UpdatesRowShell(
        cover = cover,
        title = title,
        dimmed = !anyUnread,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        onClickCover = onClickCover,
        modifier = modifier,
        subtitle = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (anyUnread) {
                    UnreadDot()
                }
                Text(
                    text = pluralStringResource(MR.plurals.notification_chapters_generic, count, count),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = textAlpha),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = {
            Icon(
                imageVector = if (expanded) {
                    MaterialSymbols.Rounded.KeyboardArrowUp
                } else {
                    MaterialSymbols.Rounded.KeyboardArrowDown
                },
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp),
            )
        },
    )
}

/**
 * The Updates mode's square-cover row, drawn by the flat row and by the collapsed group alike, so the
 * two cannot drift to different heights. [dimmed] greys the title, as a read chapter does.
 */
@Composable
internal fun UpdatesRowShell(
    cover: Any?,
    title: String,
    dimmed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    modifier: Modifier,
    subtitle: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .heightIn(min = Dimens.ListItemMinHeight)
            .padding(horizontal = Dimens.ListCoverMarginStart, vertical = Dimens.ListCoverMarginVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Square(
            modifier = Modifier.size(Dimens.ListCoverSize),
            data = cover,
            shape = Dimens.CoverShape,
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.MarginNormal)
                .weight(1f),
        ) {
            Text(
                text = title,
                maxLines = 2,
                style = MaterialTheme.typography.titleSmall,
                color = LocalContentColor.current.copy(alpha = if (dimmed) DISABLED_ALPHA else 1f),
                overflow = TextOverflow.Ellipsis,
            )
            subtitle()
        }
        trailing()
    }
}

/**
 * Indented, cover-less chapter row shown while a series group is expanded. Swipes like the flat row
 * it stands in for: whether a chapter is behind a group is the display's business, not the gesture's.
 */
@Composable
fun RecentsGroupChildRow(
    chapter: RecentsChapterUi.Named,
    state: RecentsChapterState,
    selected: Boolean,
    download: RecentsDownloadUi?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    chapterSwipeStartAction: ChapterSwipeAction,
    chapterSwipeEndAction: ChapterSwipeAction,
    onChapterSwipe: (ChapterSwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    ChapterSwipeBox(
        read = state.read,
        bookmark = state.bookmark,
        downloadState = download?.state?.invoke() ?: Download.State.NOT_DOWNLOADED,
        startAction = chapterSwipeStartAction,
        endAction = chapterSwipeEndAction,
        onSwipe = onChapterSwipe,
    ) {
        Row(
            modifier = modifier
                .selectedBackground(selected)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        onLongClick()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                )
                .height(48.dp)
                // Indent so children sit under the group title (cover width + paddings).
                .padding(start = 76.dp, end = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChapterStateLine(
                name = chapter.name,
                read = state.read,
                bookmark = state.bookmark,
                progress = readProgressLabel(state.progress),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (download != null) {
                ChapterDownloadIndicator(
                    enabled = onDownloadClick != null,
                    modifier = Modifier.padding(start = 4.dp),
                    downloadStateProvider = download.state,
                    downloadProgressProvider = download.progress.asProvider(),
                    onClick = { onDownloadClick?.invoke(it) },
                )
            }
        }
    }
}

/**
 * One row of a feed that mixes lanes, which the two combined modes both are. Every lane draws through
 * this, because a list that alternates two row heights reads as two lists spliced. Height and cover
 * are History's, matching the surface most of these rows come from, and the room it buys carries the
 * chapter, a progress bar and the time as their own lines. The trailing control still follows the
 * lane, which is the one thing a row cannot honestly share.
 *
 * The card's shape is DropSauce's: a two-line title, the chapter in the accent colour, a thick
 * progress bar with its percent, and the time behind a clock icon.
 */
@Composable
fun RecentsCombinedRow(
    cover: Any?,
    title: String,
    /** The chapter this row is about, absent on the newly added lane, which names no chapter. */
    chapterLine: String?,
    /** The chapter's own title, drawn after [chapterLine] where the lane has one. */
    chapterName: String?,
    /** When the activity happened, already carrying its verb: added, updated or read. */
    timeLine: String,
    /**
     * How far into the chapter reading got, as the bar. Null on a lane with no bar to draw, which then
     * falls back to [progressLine] so a started update row keeps saying where reading stopped.
     */
    progress: RowProgress?,
    /** The same progress written out, drawn only where there is no [progress] bar. */
    progressLine: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    chapterSwipeStartAction: ChapterSwipeAction,
    chapterSwipeEndAction: ChapterSwipeAction,
    onChapterSwipe: (ChapterSwipeAction) -> Unit,
    downloadState: Download.State,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val textAlpha = if (read) DISABLED_ALPHA else 1f
    val displayPrefs = androidx.compose.ui.platform.LocalContext.current.appGraph.raitoSourcePreferences
    val showCover by displayPrefs.recentsRowShowCover.collectAsState()
    val showProgress by displayPrefs.recentsRowShowProgress.collectAsState()
    val showActions by displayPrefs.recentsRowShowActions.collectAsState()
    ChapterSwipeBox(
        read = read,
        bookmark = bookmark,
        downloadState = downloadState,
        startAction = chapterSwipeStartAction,
        endAction = chapterSwipeEndAction,
        onSwipe = onChapterSwipe,
    ) {
        // Redesigned card: a bordered low-surface card, a larger cover with an unread marker, the
        // chapter as an accent chip, a slim progress bar with its percent, and a quiet footer.
        val scheme = MaterialTheme.colorScheme
        Row(
            modifier = modifier
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(scheme.surfaceContainerLow)
                .selectedBackground(selected)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        onLongClick()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                )
                .heightIn(min = RECENTS_ROW_HEIGHT)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCover) {
                Box {
                    MangaCover.Book(
                        modifier = Modifier.height(RECENTS_ROW_HEIGHT - 8.dp),
                        data = cover,
                        shape = RoundedCornerShape(16.dp),
                        onClick = onClickCover,
                    )
                    if (!read) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .size(10.dp)
                                .background(scheme.primary, CircleShape)
                                .border(2.dp, scheme.surfaceContainerLow, CircleShape),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = if (showCover) 12.dp else 6.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = title,
                        maxLines = 2,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LocalContentColor.current.copy(alpha = textAlpha),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                    )
                    if (showActions) trailing()
                }
                if (chapterLine != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.primaryContainer.copy(alpha = if (read) 0.55f else 1f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (bookmark) {
                                Icon(
                                    imageVector = MaterialSymbols.RoundedFilled.Bookmark,
                                    contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                                    modifier = Modifier.size(14.dp),
                                    tint = scheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(3.dp))
                            }
                            Text(
                                text = chapterLine,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onPrimaryContainer.copy(alpha = textAlpha),
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (chapterName != null) {
                            Text(
                                text = chapterName,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant.copy(alpha = textAlpha),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f, fill = false),
                            )
                        }
                    }
                }
                if (progress != null && showProgress) {
                    RowProgressBar(progress = progress, modifier = Modifier.padding(end = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = scheme.onSurfaceVariant.copy(alpha = textAlpha),
                    )
                    Text(
                        text = timeLine,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant.copy(alpha = textAlpha),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .weight(1f, fill = false),
                    )
                    if ((progress == null || !showProgress) && progressLine != null) {
                        Text(
                            text = "  \u00B7  $progressLine",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant.copy(alpha = textAlpha),
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** The digest's header for one lane. */
@Composable
fun RecentsSectionHeader(section: RecentsLaneKind, modifier: Modifier = Modifier) {
    ListGroupHeader(text = stringResource(sectionLabel(section)), modifier = modifier)
}

/** Leaves the digest for the mode that shows one lane in full. Newly added has none to jump to. */
@Composable
fun RecentsSectionFooter(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = ScreenHorizontalPadding, vertical = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = stringResource(MR.strings.recents_section_view_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * The swipe every chapter row on this surface takes: Mihon's details-row actions, so a chapter behind
 * a group, a flat row and a feed row answer one gesture the same way.
 */
@Composable
internal fun ChapterSwipeBox(
    read: Boolean,
    bookmark: Boolean,
    downloadState: Download.State,
    startAction: ChapterSwipeAction,
    endAction: ChapterSwipeAction,
    onSwipe: (ChapterSwipeAction) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.primaryContainer
    fun swipe(action: ChapterSwipeAction) =
        getSwipeAction(action, read, bookmark, downloadState, background, onSwipe = { onSwipe(action) })
    SwipeableActionsBox(
        modifier = Modifier.clipToBounds(),
        startActions = listOfNotNull(swipe(startAction)),
        endActions = listOfNotNull(swipe(endAction)),
        swipeThreshold = swipeActionThreshold,
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = content,
    )
}

/** A chapter's name behind its unread dot and bookmark, with how far reading got where [progress] says. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ChapterStateLine(
    name: String,
    read: Boolean,
    bookmark: Boolean,
    progress: String?,
    style: TextStyle,
    modifier: Modifier = Modifier,
    /** The name's colour where it is not the surrounding content colour, as on the History card. */
    color: Color = Color.Unspecified,
    /** The chapter's own title, in the secondary colour after [name], as the History card shows it. */
    trailingName: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        var textHeight by remember { mutableIntStateOf(0) }
        if (!read) {
            UnreadDot()
        }
        if (bookmark) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Bookmark,
                contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                modifier = Modifier
                    .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = name,
            maxLines = 1,
            style = style,
            color = color.takeOrElse { LocalContentColor.current }.copy(alpha = if (read) DISABLED_ALPHA else 1f),
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textHeight = it.size.height },
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        if (trailingName != null) {
            // Shown whole when it fits; when it does not, it scrolls right to left inside its own slot.
            Text(
                text = trailingName,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (read) DISABLED_ALPHA else 1f),
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(weight = 1f, fill = false)
                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1200, velocity = 30.dp),
            )
        }
        if (progress != null) {
            DotSeparatorText()
            Text(
                text = progress,
                maxLines = 1,
                color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Announced as unread, as upstream's updates row does, since the dot is the only unread marker. */
@Composable
internal fun UnreadDot() {
    Icon(
        imageVector = MaterialSymbols.RoundedFilled.Circle,
        contentDescription = stringResource(MR.strings.unread),
        modifier = Modifier
            .height(8.dp)
            .padding(end = 4.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

private fun sectionLabel(section: RecentsLaneKind) = when (section) {
    RecentsLaneKind.UPDATED -> MR.strings.recents_section_new_chapters
    RecentsLaneKind.READ -> MR.strings.recents_section_continue_reading
    RecentsLaneKind.ADDED -> MR.strings.recents_section_newly_added
}

/** An engine that cannot report progress draws none rather than a zero that reads as a stall. */
internal fun RecentsDownloadProgress.asProvider(): () -> Int = when (this) {
    is RecentsDownloadProgress.Live -> percent
    RecentsDownloadProgress.Unsupported -> NO_PROGRESS
}

/**
 * The "library last updated" line above an updated feed, carried over from Mihon's updates list. It is
 * type-neutral: the engine derives one timestamp over whichever content types the chip is showing.
 */
internal fun LazyListScope.lastUpdatedItem(lastUpdated: Long) {
    item(key = "recents-lastUpdated") {
        LastUpdatedHeader(
            lastUpdated = lastUpdated,
            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
        )
    }
}

/** The same line drawn outside a list, so an empty updated feed still says when the library last updated. */
@Composable
internal fun LastUpdatedHeader(lastUpdated: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = ScreenHorizontalPadding, vertical = MaterialTheme.padding.small),
    ) {
        Text(
            text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
            fontStyle = FontStyle.Italic,
        )
    }
}

internal val NO_PROGRESS: () -> Int = { 0 }
