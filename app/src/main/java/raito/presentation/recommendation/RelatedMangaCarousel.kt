package raito.presentation.recommendation

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import raito.domain.recommendation.RelatedMangaCandidate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Horizontal "Related" carousel on the manga details screen: a row of cover cards ranked by the
 * recommendation engine, with a skeleton while loading and an in-library badge on titles already
 * saved. Renders nothing when there's no result and nothing loading.
 */
@Composable
fun RelatedMangaCarousel(
    items: List<MangaViewModel.RelatedMangaItem>,
    loading: Boolean,
    totalCount: Int,
    onClick: (RelatedMangaCandidate) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
    topDivider: Boolean = false,
) {
    if (items.isEmpty() && !loading) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Phone layout sets this to fence the carousel off from the tags above it (the bottom
        // divider already fences it from the chapter list).
        if (topDivider) Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Text(
                text = stringResource(MR.strings.related_mangas),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            )
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            // "See all (N)" when the full pool has more than the capped carousel shows.
            if (totalCount > items.size) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(MR.strings.recs_see_all, totalCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        if (items.isEmpty()) {
            // Loading with nothing yet: a skeleton row so the section doesn't pop in abruptly.
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) { SkeletonCard() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.candidate.manga.url }) { item ->
                    // The origin eyebrow (this source, a tracker, or a taste match) makes the mixed,
                    // ranked carousel explainable at a glance.
                    RecommendationGridItem(
                        modifier = Modifier.width(CardWidth),
                        candidate = item.candidate,
                        inLibrary = item.inLibrary,
                        onClick = { onClick(item.candidate) },
                        onLongClick = { onClick(item.candidate) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SkeletonCard() {
    Box(
        modifier = Modifier
            .width(CardWidth)
            .aspectRatio(MangaCover.Book.ratio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

private val CardWidth = 124.dp
