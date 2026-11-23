package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal abstract class BaseColorScheme {

    abstract val darkScheme: ColorScheme
    abstract val lightScheme: ColorScheme

    // Cannot be pure black as there's content scrolling behind it
    // https://m3.material.io/components/navigation-bar/guidelines#90615a71-607e-485e-9e09-778bfc080563
    private val surfaceContainer = Color(0xFF0C0C0C)
    private val surfaceContainerHigh = Color(0xFF131313)
    private val surfaceContainerHighest = Color(0xFF1B1B1B)

    fun getColorScheme(
        isDark: Boolean,
        isAmoled: Boolean,
        overrideDarkSurfaceContainers: Boolean,
    ): ColorScheme {
        if (!isDark) return lightScheme.accessible()

        if (!isAmoled) return darkScheme.accessible()

        val amoledScheme = darkScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
        )

        if (!overrideDarkSurfaceContainers) return amoledScheme.accessible()

        return amoledScheme.copy(
            surfaceVariant = surfaceContainer, // Navigation bar background (ThemePrefWidget)
            surfaceContainerLowest = surfaceContainer,
            surfaceContainerLow = surfaceContainer,
            surfaceContainer = surfaceContainer, // Navigation bar background
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        ).accessible()
    }
}

// RK --> contrast guard: every theme, light or dark, static or dynamic, ends up with readable text.
// A colour that misses WCAG AA is nudged toward black or white (whichever the background favours),
// so the theme's hue survives and only its lightness moves.
private const val TextContrast = 4.5f
private const val UiContrast = 3f

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** [fg] nudged until it reads on every colour in [bgs] at [min], or returned unchanged if it already does. */
private fun readable(fg: Color, bg: Color, min: Float = TextContrast): Color = readable(fg, listOf(bg), min)

private fun readable(fg: Color, bgs: List<Color>, min: Float = TextContrast): Color {
    fun ok(c: Color) = bgs.all { contrast(c, it) >= min }
    if (ok(fg)) return fg
    val towardBlack = bgs.map { it.luminance() }.average() > 0.4
    val target = if (towardBlack) Color.Black else Color.White
    for (step in 1..20) {
        val candidate = lerp(fg, target, step / 20f).copy(alpha = 1f)
        if (ok(candidate)) return candidate
    }
    return target
}

internal fun ColorScheme.accessible(): ColorScheme {
    // Accent used as text or an icon (links, text buttons, selected labels) must read on the surfaces
    val newPrimary = readable(primary, listOf(surface, surfaceContainer))
    val newSecondary = readable(secondary, surface)
    val newTertiary = readable(tertiary, surface)
    val newError = readable(error, surface)
    return copy(
        primary = newPrimary,
        onPrimary = readable(onPrimary, newPrimary),
        onPrimaryContainer = readable(onPrimaryContainer, primaryContainer),
        secondary = newSecondary,
        onSecondary = readable(onSecondary, newSecondary),
        onSecondaryContainer = readable(onSecondaryContainer, secondaryContainer),
        tertiary = newTertiary,
        onTertiary = readable(onTertiary, newTertiary),
        onTertiaryContainer = readable(onTertiaryContainer, tertiaryContainer),
        error = newError,
        onError = readable(onError, newError),
        onErrorContainer = readable(onErrorContainer, errorContainer),
        onBackground = readable(onBackground, background),
        onSurface = readable(
            onSurface,
            listOf(surface, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest),
        ),
        onSurfaceVariant = readable(
            onSurfaceVariant,
            listOf(surface, surfaceVariant, surfaceContainer, surfaceContainerHigh),
        ),
        inverseOnSurface = readable(inverseOnSurface, inverseSurface),
        // Borders and unchecked controls need 3:1 against the page, text-free so the lower bar applies
        outline = readable(outline, listOf(surface, surfaceContainer), min = UiContrast),
    )
}
// RK <--
