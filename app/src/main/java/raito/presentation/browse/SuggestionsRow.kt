package raito.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import raito.domain.recommendation.RelatedMangaCandidate
import raito.domain.suggestions.SuggestionsState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import tachiyomi.presentation.core.i18n.stringResource

private const val SKELETON_COUNT = 8
private val CardWidth = 128.dp

/**
 * The Browse "Suggestions" block, ported from DropSauce: a Material 3 multi-browse carousel of rounded
 * cover posters with the title underneath. While loading it shows blank tiles of the same size so the
 * layout doesn't jump. Covers come from the sources the user already has enabled.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SuggestionsRow(
    state: SuggestionsState,
    onClick: (RelatedMangaCandidate) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.label_suggestions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(14.dp))
            } else {
                FilledTonalIconButton(onClick = onRefresh) {
                    Icon(
                        MaterialSymbols.Rounded.Refresh,
                        contentDescription = stringResource(MR.strings.action_webview_refresh),
                    )
                }
            }
        }
        val items = state.items
        when {
            items.isNotEmpty() || state.loading -> {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                androidx.compose.foundation.lazy.LazyRow(
                    state = listState,
                    flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val count = if (items.isNotEmpty()) items.size else SKELETON_COUNT
                    items(count, key = { index -> items.getOrNull(index)?.let { "${it.sourceId}:${it.manga.url}" } ?: "s$index" }) { index ->
                        val candidate = items.getOrNull(index)
                        Column(
                            modifier = Modifier
                                .width(CardWidth)
                                .clip(RoundedCornerShape(18.dp))
                                .let { if (candidate != null) it.clickable { onClick(candidate) } else it },
                        ) {
                            // a fixed 2:3 poster at a fixed width, so a cover is shown whole instead of
                            // being cropped to whatever the carousel squeezed the tile to
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            ) {
                                if (candidate != null) {
                                    AsyncImage(
                                        model = candidate.manga.thumbnail_url,
                                        contentDescription = candidate.manga.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            if (candidate != null) {
                                Text(
                                    text = candidate.manga.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
            else -> Text(
                text = stringResource(MR.strings.suggestions_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp),
            )
        }
    }
}
