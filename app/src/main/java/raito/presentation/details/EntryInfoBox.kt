package raito.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.debug.LocalCoverImagesHidden
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Person
import mihon.icons.materialsymbols.roundedfilled.Brush
import raito.data.coil.withCoverFallbacks
import raito.domain.novel.model.Novel
import raito.domain.novel.model.NovelCover
import raito.domain.novel.model.asNovelCover
import raito.presentation.browse.components.MissingSourceIcon
import raito.presentation.components.entryStatusIcon
import raito.presentation.components.entryStatusRes
import raito.presentation.theme.Dimens
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha

/**
 * Content-agnostic header data for the shared [EntryInfoBox]. [coverModel] is a coil model (a `Manga`
 * or a [NovelCover]), so each content type feeds its own object. [coverFallbacks] are the other members'
 * models on a merged series' All view, empty otherwise; [cover] is what the header draws.
 */
data class EntryHeaderUi(
    val coverModel: Any,
    val title: String,
    val author: String?,
    val artist: String?,
    val status: Long,
    val sourceName: String,
    /** The viewed member's source; the download gate reads it too (EntryDetailsScreenState.Loaded). */
    val sourceState: EntrySourceState,
    /** The library query for the viewed source's entries; null where [sourceName] labels a merged group. */
    val sourceQuery: String?,
    val coverFallbacks: List<Any> = emptyList(),
) {
    val cover: Any = withCoverFallbacks(coverModel, coverFallbacks)
}

fun Manga.toEntryHeader(
    sourceName: String,
    sourceState: EntrySourceState,
    sourceQuery: String?,
    coverFallbacks: List<Manga>,
) = EntryHeaderUi(
    coverModel = this,
    title = title,
    author = author,
    artist = artist,
    status = status,
    sourceName = sourceName,
    sourceState = sourceState,
    sourceQuery = sourceQuery,
    coverFallbacks = coverFallbacks,
)

fun Novel.toEntryHeader(
    sourceName: String,
    sourceState: EntrySourceState,
    sourceQuery: String?,
    coverFallbacks: List<Novel>,
) = EntryHeaderUi(
    coverModel = asNovelCover(),
    title = title,
    author = author,
    artist = artist,
    status = status,
    sourceName = sourceName,
    sourceState = sourceState,
    sourceQuery = sourceQuery,
    coverFallbacks = coverFallbacks.map { it.asNovelCover() },
)

/**
 * Shared details header (blurred cover backdrop + cover + title / author / artist / status / source)
 * for manga and novels. Replaces MangaInfoBox + NovelInfoBox. Status codes match between the two
 * (see NovelStatusCode), so the status icon + label render from one switch. Tapping the title / author
 * / artist runs [onGlobalSearch] and tapping the source browses it; long-press offers library search,
 * global search and copy (Browse, library search and copy on the source).
 */
@Composable
fun EntryInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    header: EntryHeaderUi,
    onCoverClick: () -> Unit,
    onGlobalSearch: (query: String) -> Unit,
    librarySearch: (query: String) -> Unit,
    onBrowseSource: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // DropSauce backdrop: the cover blown up and blurred, fading into the surface
        val surface = MaterialTheme.colorScheme.background
        // The backdrop is the cover image too, so hiding covers from the debug menu drops it.
        if (!LocalCoverImagesHidden.current) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(header.cover)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(28.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                0f to surface.copy(alpha = 0.18f),
                                0.45f to surface.copy(alpha = 0.50f),
                                0.78f to surface.copy(alpha = 0.94f),
                                1f to surface,
                            ),
                        )
                    },
            )
        }

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                EntryTitlesSmall(
                    appBarPadding = appBarPadding,
                    header = header,
                    onCoverClick = onCoverClick,
                    onGlobalSearch = onGlobalSearch,
                    librarySearch = librarySearch,
                    onBrowseSource = onBrowseSource,
                )
            } else {
                EntryTitlesLarge(
                    appBarPadding = appBarPadding,
                    header = header,
                    onCoverClick = onCoverClick,
                    onGlobalSearch = onGlobalSearch,
                    librarySearch = librarySearch,
                    onBrowseSource = onBrowseSource,
                )
            }
        }
    }
}

@Composable
private fun EntryTitlesLarge(
    appBarPadding: Dp,
    header: EntryHeaderUi,
    onCoverClick: () -> Unit,
    onGlobalSearch: (query: String) -> Unit,
    librarySearch: (query: String) -> Unit,
    onBrowseSource: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MangaCover.Book(
            modifier = Modifier.fillMaxWidth(0.65f),
            data = header.cover,
            contentDescription = stringResource(MR.strings.manga_cover),
            shape = Dimens.CoverShapeLarge,
            onClick = onCoverClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
        EntryContentInfo(
            header = header,
            onGlobalSearch = onGlobalSearch,
            librarySearch = librarySearch,
            onBrowseSource = onBrowseSource,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EntryTitlesSmall(
    appBarPadding: Dp,
    header: EntryHeaderUi,
    onCoverClick: () -> Unit,
    onGlobalSearch: (query: String) -> Unit,
    librarySearch: (query: String) -> Unit,
    onBrowseSource: (() -> Unit)?,
) {
    // Hero: a lifted cover beside the title block, so the text gets the width a centred stack wasted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = appBarPadding + 12.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MangaCover.Book(
            modifier = Modifier
                .width(136.dp)
                .shadow(18.dp, Dimens.CoverShapeXL),
            data = header.cover,
            contentDescription = stringResource(MR.strings.manga_cover),
            shape = Dimens.CoverShapeXL,
            onClick = onCoverClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            EntryContentInfo(
                header = header,
                onGlobalSearch = onGlobalSearch,
                librarySearch = librarySearch,
                onBrowseSource = onBrowseSource,
                textAlign = TextAlign.Start,
                horizontal = Alignment.Start,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.EntryContentInfo(
    header: EntryHeaderUi,
    onGlobalSearch: (query: String) -> Unit,
    librarySearch: (query: String) -> Unit,
    onBrowseSource: (() -> Unit)?,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
    horizontal: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    val context = LocalContext.current
    val title = header.title
    val author = header.author
    val artist = header.artist

    // One menu for all four rows, as the tag chips do it: it anchors to this column rather
    // than to the row under the finger, which is the cost of not building four of them.
    // Deliberately remember, not rememberSaveable: an open menu should not survive a rotation
    // and reopen against a stale anchor.
    var showMenu by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf("") }
    var menuIsSource by remember { mutableStateOf(false) }

    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        // The source row searches the library by the source's key rather than its name, which two
        // sources can share, and offers no search where the name labels a merged group.
        if (menuIsSource) {
            if (onBrowseSource != null) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.browse)) },
                    onClick = {
                        onBrowseSource()
                        showMenu = false
                    },
                )
            }
            header.sourceQuery?.let { query ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_library_search)) },
                    onClick = {
                        librarySearch(query)
                        showMenu = false
                    },
                )
            }
        } else {
            DropdownMenuItem(
                text = { Text(text = stringResource(MR.strings.action_library_search)) },
                onClick = {
                    librarySearch(menuTarget)
                    showMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(MR.strings.action_global_search)) },
                onClick = {
                    onGlobalSearch(menuTarget)
                    showMenu = false
                },
            )
        }
        DropdownMenuItem(
            text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
            onClick = {
                context.copyToClipboard(menuTarget, menuTarget)
                showMenu = false
            },
        )
    }

    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 28.sp),
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickableNoIndication(
            onLongClick = {
                if (title.isNotBlank()) {
                    menuTarget = title
                    menuIsSource = false
                    showMenu = true
                }
            },
            onClick = { if (title.isNotBlank()) onGlobalSearch(title) },
        ),
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(
            MaterialTheme.padding.extraSmall,
            horizontal,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = author?.takeIf { it.isNotBlank() } ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickableNoIndication(
                onLongClick = {
                    if (!author.isNullOrBlank()) {
                        menuTarget = author
                        menuIsSource = false
                        showMenu = true
                    }
                },
                onClick = { if (!author.isNullOrBlank()) onGlobalSearch(author) },
            ),
            textAlign = textAlign,
        )
    }

    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.secondaryItemAlpha().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(
                MaterialTheme.padding.extraSmall,
                horizontal,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Brush,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickableNoIndication(
                    onLongClick = {
                        menuTarget = artist
                        menuIsSource = false
                        showMenu = true
                    },
                    onClick = { onGlobalSearch(artist) },
                ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Status and source as two separate chips, each with its own explicit text style (a shared
    // ProvideTextStyle left the dot separator and the source name on mismatched sizes)
    val chipText = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, horizontal),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = entryStatusIcon(header.status),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(16.dp),
            )
            Text(
                text = stringResource(entryStatusRes(header.status)),
                style = chipText,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickableNoIndication(
                    onLongClick = {
                        menuTarget = header.sourceName
                        menuIsSource = true
                        showMenu = true
                    },
                    onClick = { onBrowseSource?.invoke() },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (header.sourceState == EntrySourceState.Missing) {
                MissingSourceIcon(
                    Modifier
                        .padding(end = 6.dp)
                        .size(16.dp),
                )
            }
            Text(
                text = header.sourceName,
                style = chipText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}
