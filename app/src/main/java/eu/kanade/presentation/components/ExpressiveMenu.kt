package eu.kanade.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import kotlin.math.max
import kotlin.math.min

private val CardCorner = 24.dp
private val CardPadding = 4.dp
private val ItemHeight = 44.dp
private val ItemPadding = 12.dp
private val ScreenMargin = 8.dp
private val BloomStartRadius = 21.dp // half of the 42dp overflow button

private const val ICON_LEAD_MS = 30L
private const val ROW_REVEAL_SHARE = 0.4f

/** What a row needs to time its entrance against the card unrolling over it. */
private class MenuMotion(
    val heightProgress: () -> Float,
    val originY: () -> Float,
    val cardHeight: () -> Float,
)

private val LocalMenuMotion = compositionLocalOf<MenuMotion?> { null }

/**
 * Port of DropSauce's expressive overflow menu: the card blooms out of the anchor button (a dot that
 * stretches into a pill, then unrolls into the card) and folds back into it. Place it inside the same
 * `Box` as the anchor, like a `DropdownMenu`.
 */
@Composable
fun ExpressiveMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val widthProgress = remember { Animatable(0f) }
    val heightProgress = remember { Animatable(0f) }
    val surfaceAlpha = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf<Offset?>(null) }
    var cardHeight by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val margin = with(density) { ScreenMargin.roundToPx() }
    val provider = remember(margin) {
        BloomPositionProvider(margin) { newOrigin, size ->
            if (origin != newOrigin) origin = newOrigin
            if (cardHeight != size.height.toFloat()) cardHeight = size.height.toFloat()
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            visible = true
            coroutineScope {
                launch { surfaceAlpha.animateTo(1f, tween(90)) }
                launch { widthProgress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 800f)) }
                heightProgress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 380f))
            }
        } else if (visible) {
            coroutineScope {
                launch { widthProgress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 900f)) }
                launch {
                    delay(110)
                    surfaceAlpha.animateTo(0f, tween(110))
                }
                heightProgress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 1400f))
            }
            visible = false
        }
    }

    if (!visible) return

    val motion = remember {
        MenuMotion(
            heightProgress = { heightProgress.value },
            originY = { origin?.y ?: 0f },
            cardHeight = { cardHeight },
        )
    }
    val surface = MaterialTheme.colorScheme.surfaceContainer
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        CompositionLocalProvider(LocalMenuMotion provides motion) {
            Box(
                modifier
                    .graphicsLayer {
                        val o = origin
                        if (o == null) {
                            alpha = 0f
                            return@graphicsLayer
                        }
                        shape = BloomShape(
                            origin = o,
                            startRadius = BloomStartRadius.toPx(),
                            widthProgress = widthProgress.value,
                            heightProgress = heightProgress.value,
                            cornerRadius = CardCorner.toPx(),
                        )
                        clip = true
                        shadowElevation = 3.dp.toPx()
                        alpha = surfaceAlpha.value
                    }
                    .background(surface)
                    .animateContentSize(spring(dampingRatio = 1f, stiffness = 380f)),
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { alpha = (heightProgress.value * 2.5f).coerceIn(0f, 1f) }
                        .widthIn(min = 200.dp, max = 320.dp)
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(CardPadding),
                    content = content,
                )
            }
        }
    }
}

/** One row of an [ExpressiveMenu]: 44dp pill, leading icon, then a checkbox, radio or chevron if asked. */
@Composable
fun ExpressiveMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    checked: Boolean? = null,
    radio: Boolean = false,
    hasSubmenu: Boolean = false,
    bold: Boolean = false,
    enabled: Boolean = true,
    reserveIconSpace: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val motion = LocalMenuMotion.current
    var rowTop by remember { mutableFloatStateOf(Float.NaN) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val iconProgress = remember { Animatable(0f) }
    val textProgress = remember { Animatable(0f) }

    // Rows cascade in one after another as the unrolling edge sweeps over them, icon slightly first.
    LaunchedEffect(Unit) {
        if (motion != null) {
            snapshotFlow {
                val h = motion.heightProgress()
                if (rowTop.isNaN() || rowHeight == 0f) return@snapshotFlow false
                val oy = motion.originY()
                val bottom = lerp(oy, motion.cardHeight(), h)
                val top = lerp(oy, 0f, h)
                h >= 0.98f ||
                    (bottom >= rowTop + rowHeight * ROW_REVEAL_SHARE &&
                        top <= rowTop + rowHeight * (1f - ROW_REVEAL_SHARE))
            }.first { it }
        }
        launch { iconProgress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 600f)) }
        delay(ICON_LEAD_MS)
        textProgress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 600f))
    }

    val content = colors.onSurface
    Row(
        modifier = modifier
            .onPlaced { rowHeight = it.size.height.toFloat(); rowTop = it.positionInParent().y }
            .fillMaxWidth()
            .height(ItemHeight)
            .clip(RoundedCornerShape(ItemHeight / 2))
            .background(if (isPressed) content.copy(alpha = 0.27f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = ItemPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null || reserveIconSpace) {
            Box(
                Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        val p = iconProgress.value
                        alpha = p.coerceIn(0f, 1f)
                        scaleX = lerp(0.5f, 1f, p)
                        scaleY = scaleX
                    },
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(ItemPadding))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    val p = textProgress.value
                    alpha = (p * 1.4f).coerceIn(0f, 1f)
                    translationY = (1f - p) * -6.dp.toPx()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (checked != null) {
                Spacer(Modifier.width(ItemPadding))
                if (radio) {
                    RadioButton(selected = checked, onClick = null)
                } else {
                    Checkbox(checked = checked, onCheckedChange = null)
                }
            }
            if (hasSubmenu) {
                Spacer(Modifier.width(ItemPadding))
                Icon(
                    MaterialSymbols.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** The row that opens an in-menu page comes back on top of it with a back arrow, like DropSauce's sub-menus. */
@Composable
fun ColumnScope.ExpressiveMenuHeader(title: String, onBack: () -> Unit) {
    ExpressiveMenuItem(
        text = title,
        onClick = onBack,
        leadingIcon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
        bold = true,
    )
    ExpressiveMenuDivider()
}

@Composable
fun ExpressiveMenuDivider() {
    Box(
        Modifier
            .padding(horizontal = ItemPadding, vertical = 4.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    )
}

private class BloomPositionProvider(
    private val margin: Int,
    private val onPlaced: (origin: Offset, size: IntSize) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val alignEnd = anchorBounds.center.x > windowSize.width / 2
        val rawX = if (alignEnd) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val x = rawX.coerceIn(margin, max(windowSize.width - margin - popupContentSize.width, margin))
        val y = anchorBounds.top.coerceIn(margin, max(windowSize.height - margin - popupContentSize.height, margin))
        onPlaced(
            Offset(anchorBounds.center.x - x.toFloat(), anchorBounds.center.y - y.toFloat()),
            popupContentSize,
        )
        return IntOffset(x, y)
    }
}

/** The card's visible part: a round dot on the anchor that stretches into a pill, then unrolls into the card. */
private class BloomShape(
    private val origin: Offset,
    private val startRadius: Float,
    private val widthProgress: Float,
    private val heightProgress: Float,
    private val cornerRadius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = widthProgress.coerceIn(0f, 1f)
        val h = heightProgress.coerceIn(0f, 1f)
        val r = startRadius
        val ox = origin.x.coerceIn(r, max(size.width - r, r))
        val oy = origin.y.coerceIn(r, max(size.height - r, r))
        val left = lerp(ox - r, 0f, w)
        val right = lerp(ox + r, size.width, w)
        val top = lerp(oy - r, 0f, h)
        val bottom = lerp(oy + r, size.height, h)
        val corner = min(lerp(r, cornerRadius, h), min(right - left, bottom - top) / 2f)
        return Outline.Rounded(RoundRect(left, top, right, bottom, CornerRadius(corner)))
    }
}
