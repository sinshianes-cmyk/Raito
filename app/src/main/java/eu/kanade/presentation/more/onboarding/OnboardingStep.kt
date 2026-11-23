package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

internal interface OnboardingStep {

    val isComplete: Boolean

    /** What the toast says when the person taps Next while [isComplete] is false. */
    val incompleteMessage: StringResource get() = MR.strings.onboarding_storage_selection_required

    @Composable
    fun Content()
}
