package eu.kanade.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialExpressiveTheme
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.colorscheme.AmberColorScheme
import eu.kanade.presentation.theme.colorscheme.AsukaColorScheme
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.ItsukaColorScheme
import eu.kanade.presentation.theme.colorscheme.KanadeColorScheme
import eu.kanade.presentation.theme.colorscheme.LimeColorScheme
import eu.kanade.presentation.theme.colorscheme.MamimiColorScheme
import eu.kanade.presentation.theme.colorscheme.MikuColorScheme
import eu.kanade.presentation.theme.colorscheme.MionColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import eu.kanade.presentation.theme.colorscheme.RikkaColorScheme
import eu.kanade.presentation.theme.colorscheme.SakuraColorScheme
import eu.kanade.presentation.theme.colorscheme.ShanaColorScheme
import eu.kanade.presentation.theme.colorscheme.SkyColorScheme
import eu.kanade.presentation.theme.colorscheme.TotoroColorScheme
import mihon.app.di.appGraph
import raito.presentation.theme.RaitoTypography // RK
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember { context.appGraph.uiPreferences }
    AppDisplayScale {
        BaseTachiyomiTheme(
            appTheme = appTheme ?: uiPreferences.appTheme.get(),
            isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
            content = {
                WindowBackgroundSync()
                content()
            },
        )
    }
}

// RK --> the XML themes only cover the legacy palettes, so paint the window with the live scheme's
// background: no wrong-coloured flash behind transitions, keyboards or overscroll, in any theme.
@Composable
private fun WindowBackgroundSync() {
    val background = MaterialTheme.colorScheme.background
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        var ctx: Context? = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        (ctx as? Activity)?.window?.setBackgroundDrawable(ColorDrawable(background.toArgb()))
    }
}
// RK <--

// RK --> cover-based theming: when a seed color is given, build a dynamic scheme from it,
// otherwise fall back to the normal app theme.
@Composable
fun TachiyomiTheme(
    seedColor: Color?,
    amoled: Boolean? = null,
    typography: Typography = RaitoTypography, // RK
    content: @Composable () -> Unit,
) {
    if (seedColor == null) {
        TachiyomiTheme(appTheme = null, amoled = amoled, content = content)
    } else {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }
        AppDisplayScale {
            DynamicMaterialExpressiveTheme(
                seedColor = seedColor,
                isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
                style = uiPreferences.themeCoverBasedStyle.get(),
                typography = typography,
                animate = true,
                content = content,
            )
        }
    }
}
// RK <--

/**
 * Applies the "App display size" setting. The factor is applied to the device's own density rather than
 * the current LocalDensity, so nesting themes (cover-based theming inside a screen) never scales twice.
 * Text follows because sp resolves through the same density.
 */
@Composable
fun AppDisplayScale(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sizePref = remember { context.appGraph.uiPreferences.appDisplaySize }
    val size by sizePref.collectAsState()
    val base = context.resources.displayMetrics.density
    val fontScale = context.resources.configuration.fontScale
    CompositionLocalProvider(
        LocalDensity provides Density(density = base * size.scale, fontScale = fontScale),
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(appTheme, isAmoled, content)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    MaterialExpressiveTheme(
        colorScheme = remember(appTheme, isDark, isAmoled) {
            getThemeColorScheme(
                context = context,
                appTheme = appTheme,
                isDark = isDark,
                isAmoled = isAmoled,
            )
        },
        shapes = DropSauceShapes,
        typography = RaitoTypography, // RK
        content = content,
    )
}

// DropSauce corner tokens: small 8, medium 16, large 24, extraLarge 28
private val DropSauceShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TotoroColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TotoroColorScheme, // RK
    AppTheme.DS_AMBER to AmberColorScheme, // RK
    AppTheme.DS_ASUKA to AsukaColorScheme, // RK
    AppTheme.DS_ITSUKA to ItsukaColorScheme, // RK
    AppTheme.DS_KANADE to KanadeColorScheme, // RK
    AppTheme.DS_LIME to LimeColorScheme, // RK
    AppTheme.DS_MAMIMI to MamimiColorScheme, // RK
    AppTheme.DS_MIKU to MikuColorScheme, // RK
    AppTheme.DS_MION to MionColorScheme, // RK
    AppTheme.DS_RIKKA to RikkaColorScheme, // RK
    AppTheme.DS_SAKURA to SakuraColorScheme, // RK
    AppTheme.DS_SHANA to ShanaColorScheme, // RK
    AppTheme.DS_SKY to SkyColorScheme, // RK
    AppTheme.DS_TOTORO to TotoroColorScheme, // RK
)
