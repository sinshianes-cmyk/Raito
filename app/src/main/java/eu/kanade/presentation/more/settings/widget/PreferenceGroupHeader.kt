package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceGroupHeader(title: String, highlighted: Boolean = false) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .highlightBackground(highlighted)
            .padding(bottom = 8.dp, top = 20.dp),
    ) {
        Text(
            text = title,
            // RK: DropSauce group title, aligned with the text of an icon-less card row (20dp margin + 16dp padding)
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 36.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
