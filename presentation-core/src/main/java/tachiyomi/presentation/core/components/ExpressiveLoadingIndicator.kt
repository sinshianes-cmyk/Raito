package tachiyomi.presentation.core.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * RK: DropSauce's loading look, the Material 3 Expressive morphing-shape indicator in place of the
 * stock spinning ring. Use it for indeterminate waits; determinate progress keeps the ring.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = LoadingIndicatorDefaults.indicatorColor,
) {
    LoadingIndicator(modifier = modifier, color = color)
}
