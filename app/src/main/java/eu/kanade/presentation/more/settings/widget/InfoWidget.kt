package eu.kanade.presentation.more.settings.widget

import tachiyomi.presentation.core.components.material.ScreenHorizontalPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
internal fun InfoWidget(text: String) {
    Column(
        modifier = Modifier
            // RK: info note as a soft tonal card
            .padding(
                horizontal = ScreenHorizontalPadding,
                vertical = MaterialTheme.padding.medium,
            )
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp)
            .secondaryItemAlpha(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Info,
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    TachiyomiPreviewTheme {
        Surface {
            InfoWidget(text = stringResource(MR.strings.download_ahead_info))
        }
    }
}
