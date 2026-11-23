package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import raito.presentation.theme.Dimens
import tachiyomi.domain.manga.model.Manga

@Composable
fun BaseMangaListItem(
    manga: Manga,
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onClickCover: () -> Unit = onClickItem,
    cover: @Composable RowScope.() -> Unit = { defaultCover(manga, onClickCover) },
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = { defaultContent(manga) },
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickItem)
            .heightIn(min = Dimens.ListItemMinHeight)
            .padding(horizontal = Dimens.ListCoverMarginStart, vertical = Dimens.ListCoverMarginVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        content()
        actions()
    }
}

private val defaultCover: @Composable RowScope.(Manga, () -> Unit) -> Unit = { manga, onClick ->
    MangaCover.Square(
        modifier = Modifier.size(Dimens.ListCoverSize),
        data = manga,
        shape = Dimens.CoverShape,
        onClick = onClick,
    )
}

private val defaultContent: @Composable RowScope.(Manga) -> Unit = {
    Box(modifier = Modifier.weight(1f)) {
        Text(
            text = it.title,
            modifier = Modifier
                .padding(start = Dimens.ListTextMarginStart),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
