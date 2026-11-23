package raito.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R

/** Google Sans Flex (variable), rounded axis at 100, one instance per weight DropSauce uses. */
private fun gflex(weight: Int) = FontFamily(
    Font(
        resId = R.font.gflex_variable,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("ROND", 100f),
        ),
    ),
)

private val Body = gflex(500)
private val Medium = gflex(600)
private val Heading = gflex(700)
private val Bold = gflex(800)

private fun style(family: FontFamily, weight: Int, size: Int, line: Int, spacing: Float) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.em,
)

/** DropSauce's M3 type scale (styles.xml TextAppearance.Kotatsu.*). */
val RaitoTypography = Typography(
    displayLarge = style(Bold, 800, 48, 56, 0f),
    displayMedium = style(Bold, 800, 36, 44, 0f),
    displaySmall = style(Body, 500, 30, 38, 0f),
    headlineLarge = style(Heading, 700, 32, 40, 0f),
    headlineMedium = style(Heading, 700, 28, 36, 0f),
    headlineSmall = style(Heading, 700, 24, 32, 0f),
    titleLarge = style(Body, 500, 22, 28, 0f),
    titleMedium = style(Medium, 600, 18, 24, 0.00833f),
    titleSmall = style(Medium, 600, 14, 20, 0.00714f),
    bodyLarge = style(Body, 500, 16, 24, 0.03125f),
    bodyMedium = style(Body, 500, 14, 20, 0.01786f),
    bodySmall = style(Body, 500, 12, 16, 0.03333f),
    labelLarge = style(Medium, 600, 16, 20, 0.00625f),
    labelMedium = style(Medium, 600, 14, 16, 0.03571f),
    labelSmall = style(Medium, 600, 11, 16, 0.04545f),
)
