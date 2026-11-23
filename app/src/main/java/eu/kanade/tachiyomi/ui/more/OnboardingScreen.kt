package eu.kanade.tachiyomi.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.onboarding.OnboardingScreen
import eu.kanade.presentation.util.Screen
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState

class OnboardingScreen : Screen() {

    @Composable
    override fun Content() {
        // RK: context is read below, after the navigator
        val navigator = LocalNavigator.currentOrThrow

        // RK: placed by Raito's own graph conversion; upstream's order is equivalent
        val context = LocalContext.current
        val basePreferences = remember { context.appGraph.basePreferences }
        val shownOnboardingFlow by basePreferences.shownOnboardingFlow.collectAsState()

        val finishOnboarding: () -> Unit = {
            basePreferences.shownOnboardingFlow.set(true)
            navigator.pop()
        }

        BackHandler(enabled = !shownOnboardingFlow) {
            // Prevent exiting if onboarding hasn't been completed
        }

        OnboardingScreen(
            onComplete = finishOnboarding,
        )
    }
}
