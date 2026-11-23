package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.util.system.toast
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Folder
import mihon.icons.materialsymbols.rounded.Palette
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.icons.materialsymbols.rounded.Security
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val ScreenPadding = 20.dp
private val HeroShape = RoundedCornerShape(30.dp)

private class StepHeader(
    val icon: ImageVector,
    val title: StringResource,
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val slideDistance = rememberSlideDistance()
    val context = LocalContext.current

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            ThemeStep(),
            StorageStep(),
            PermissionStep(),
            GuidesStep(),
        )
    }
    val headers = remember {
        listOf(
            StepHeader(MaterialSymbols.Rounded.Palette, MR.strings.onboarding_heading),
            StepHeader(MaterialSymbols.Rounded.Folder, MR.strings.onboarding_title_storage),
            StepHeader(MaterialSymbols.Rounded.Security, MR.strings.onboarding_title_permissions),
            StepHeader(MaterialSymbols.Rounded.RocketLaunch, MR.strings.onboarding_title_guides),
        )
    }
    val isLastStep = currentStep == steps.lastIndex
    val canAccept = steps[currentStep].isComplete

    // Back is held while a restore runs, like the Next button
    BackHandler(enabled = currentStep != 0) {
        if (canAccept) currentStep--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 104.dp),
            transitionSpec = {
                materialSharedAxisX(
                    forward = targetState > initialState,
                    slideDistance = slideDistance,
                )
            },
            label = "stepContent",
        ) { index ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 20.dp, start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Step counter and a segmented progress bar
                Text(
                    text = stringResource(MR.strings.onboarding_step_counter, index + 1, steps.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                StepProgress(current = index, total = steps.size)
                Spacer(Modifier.height(32.dp))

                // Hero icon: a soft halo behind a rounded tile
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = HeroShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(88.dp),
                    ) {
                        Icon(
                            imageVector = headers[index].icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(headers[index].title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (index == 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(MR.strings.onboarding_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                Spacer(Modifier.height(28.dp))
                steps[index].Content()
            }
        }

        // Bottom bar: Back and a wide labelled button over a soft scrim
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    enabled = canAccept,
                    shape = OnboardingButtonShape,
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                        contentDescription = stringResource(MR.strings.onboarding_action_back),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Button(
                onClick = {
                    if (!canAccept) {
                        // Say why nothing happened: no folder yet, or a restore still running.
                        context.toast(steps[currentStep].incompleteMessage)
                        return@Button
                    }
                    if (isLastStep) onComplete() else currentStep++
                },
                shape = OnboardingButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .alpha(if (canAccept) 1f else 0.38f),
            ) {
                Text(
                    text = stringResource(
                        if (isLastStep) MR.strings.onboarding_action_finish else MR.strings.onboarding_action_next,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLastStep) {
                        MaterialSymbols.Rounded.Check
                    } else {
                        MaterialSymbols.AutoMirroredRounded.ArrowForward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** One rounded segment per step; the ones up to the current step fill with the primary colour. */
@Composable
private fun StepProgress(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                targetValue = if (index <= current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                },
                label = "segment_$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
