package eu.kanade.presentation.manga.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import exh.debug.HiddenCover
import exh.debug.LocalCoverImagesHidden

enum class MangaCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    Grid(13f / 18f), // DropSauce grid card: height/width 18:13
    ;

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
        // RK: scale + load callback let the panorama display mode letterbox wide covers and measure
        // each cover's real aspect ratio at load (see MangaComfortableGridItem's usePanoramaCover).
        scale: ContentScale = ContentScale.Crop,
        onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    ) {
        // RK --> the frame is shared with the debug menu's hidden cover, which draws in the image's place
        // RK: tonal placeholder plus a hairline outline so covers sit softly on any surface
        val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
        val outline = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        val frame = modifier
            .aspectRatio(ratio)
            .clip(shape)
            .border(outline, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
        if (LocalCoverImagesHidden.current) {
            HiddenCover(background = placeholderColor, modifier = frame)
            return
        }
        // RK <--
        AsyncImage(
            model = data,
            placeholder = ColorPainter(placeholderColor),
            error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
            contentDescription = contentDescription,
            modifier = frame, // RK
            contentScale = scale,
            // RK: report the loaded image so the caller can measure its aspect ratio
            onSuccess = onSuccess,
        )
    }
}
