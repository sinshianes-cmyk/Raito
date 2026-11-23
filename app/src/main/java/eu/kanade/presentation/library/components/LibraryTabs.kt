package eu.kanade.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import raito.presentation.library.LibraryBucket // RK
import raito.presentation.library.visualLabel // RK
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    // RK: a tab is one section of the assembled library, which is a category or a dynamic group
    buckets: List<LibraryBucket>,
    pagerState: PagerState,
    getItemCountForCategory: (LibraryBucket) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(buckets.lastIndex)
    PrimaryScrollableTabRow(
        selectedTabIndex = currentPageIndex,
        edgePadding = 16.dp,
        containerColor = Color.Transparent, // RK: DropSauce's tabs sit on the screen
        divider = {},
        indicator = {}, // RK: the selected tab is a tonal pill instead of an underline
    ) {
        buckets.forEachIndexed { index, bucket ->
            // RK
            val selected = currentPageIndex == index
            val pill by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                label = "libraryTabPill",
            )
            Tab(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pill),
                selected = selected,
                onClick = { onTabItemClick(index) },
                text = {
                    TabText(
                        text = bucket.visualLabel, // RK
                        badgeCount = getItemCountForCategory(bucket),
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
