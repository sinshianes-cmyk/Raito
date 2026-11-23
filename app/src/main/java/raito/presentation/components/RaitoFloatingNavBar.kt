package raito.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val BarShape = RoundedCornerShape(50)

/**
 * DropSauce's floating bottom bar: a bordered pill with 12dp side margins, the selected tab
 * shown as a tonal pill carrying its label, the rest icon-only. Plain tap and reselect only.
 */
@Composable
fun RaitoFloatingNavBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = BarShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
fun RaitoFloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    onReselect: () -> Unit,
    icon: Painter,
    label: String,
    badge: (@Composable () -> Unit)? = null,
    alwaysShowLabel: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.secondaryContainer else scheme.secondaryContainer.copy(alpha = 0f),
        label = "navItemContainer",
    )
    val content by animateColorAsState(
        if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        label = "navItemContent",
    )
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(BarShape)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            .background(container)
            .combinedClickable(onClick = { if (selected) onReselect() else onClick() })
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconContent: @Composable () -> Unit = {
            Icon(
                painter = icon,
                contentDescription = if (selected || alwaysShowLabel) null else label,
                tint = content,
                modifier = Modifier.size(24.dp),
            )
        }
        if (badge != null) BadgedBox(badge = { badge() }, content = { iconContent() }) else iconContent()
        AnimatedVisibility(
            visible = selected || alwaysShowLabel,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            Text(
                text = label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
