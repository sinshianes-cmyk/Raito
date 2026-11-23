package raito.presentation.reader.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import raito.domain.reader.TapGrid
import tachiyomi.core.common.preference.Preference
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** The Custom layout's nine cells; a tap moves a cell through previous, next and menu. Each says so in words as well as colour. */
@Composable
fun TapGridEditor(gridPref: Preference<String>, modifier: Modifier = Modifier) {
    val stored by gridPref.collectAsState()
    val grid = TapGrid.normalize(stored)
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    TapGridCell(grid[index]) { gridPref.set(TapGrid.cycle(grid, index)) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TapGridCell(cell: Char, onClick: () -> Unit) {
    val region = when (cell) {
        'P' -> NavigationRegion.PREV
        'N' -> NavigationRegion.NEXT
        else -> NavigationRegion.MENU
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(region.color))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(region.nameRes),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black,
        )
    }
}
