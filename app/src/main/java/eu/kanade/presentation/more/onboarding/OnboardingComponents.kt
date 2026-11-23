package eu.kanade.presentation.more.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check

/** Shared building blocks so every onboarding step speaks the same card, button and check-mark language. */

internal val OnboardingCardShape = RoundedCornerShape(28.dp)
internal val OnboardingButtonShape = RoundedCornerShape(18.dp)
private val IconBubbleShape = RoundedCornerShape(16.dp)

/** A soft elevated card with a hairline border, the surface every step's content sits on. */
@Composable
internal fun OnboardingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = OnboardingCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** A small label above a group of cards. */
@Composable
internal fun OnboardingSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 4.dp),
    )
}

/** A rounded-square tile carrying a step's or row's icon. */
@Composable
internal fun OnboardingIconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(IconBubbleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** The one check mark: a filled primary disc with a tick once [checked], an empty ring before. */
@Composable
internal fun OnboardingCheck(
    checked: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 30.dp,
) {
    val fill by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        label = "checkFill",
    )
    val tick by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale",
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = 2.dp,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Check,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(size * 0.62f)
                .scale(tick),
        )
    }
}

internal enum class OnboardingButtonStyle { Primary, Tonal }

/** A full-width pill-ish button, the one button shape used across the steps. */
@Composable
internal fun OnboardingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OnboardingButtonStyle = OnboardingButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = when (style) {
        OnboardingButtonStyle.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        OnboardingButtonStyle.Tonal -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        shape = OnboardingButtonShape,
        colors = colors,
        elevation = null,
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** An icon, a title and a body, optionally with a trailing check, then room for the card's actions. */
@Composable
internal fun OnboardingActionCard(
    icon: ImageVector,
    body: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    checked: Boolean? = null,
    actions: @Composable ColumnScope.() -> Unit,
) {
    OnboardingCard(modifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OnboardingIconBubble(icon)
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (checked != null) OnboardingCheck(checked = checked)
        }
        actions()
    }
}

/** A tappable row with the onboarding check mark in front of its label, for options that can be on or off. */
@Composable
internal fun OnboardingCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OnboardingCheck(checked = checked, size = 26.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
