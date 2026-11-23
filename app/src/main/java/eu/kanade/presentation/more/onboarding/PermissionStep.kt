package eu.kanade.presentation.more.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import eu.kanade.tachiyomi.util.system.telemetryIncluded
import kotlinx.coroutines.delay
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.BugReport
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Security
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.roundedfilled.Extension
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal class PermissionStep : OnboardingStep {

    private var installGranted by mutableStateOf(false)
    private var notificationGranted by mutableStateOf(false)
    private var batteryGranted by mutableStateOf(false)

    override val isComplete: Boolean = true

    private fun refresh(context: Context) {
        installGranted = context.packageManager.canRequestPackageInstalls()
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        batteryGranted = context.getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val privacyPreferences = remember { context.appGraph.privacyPreferences }
        val lifecycleOwner = LocalLifecycleOwner.current

        // Check on every resume (returning from a system settings screen or the permission dialog)...
        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) = refresh(context)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            refresh(context)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        // ...and keep polling while visible, because some devices (split-screen, MIUI dialogs) never
        // pause the activity when a permission changes, so no resume event would arrive.
        LaunchedEffect(Unit) {
            while (true) {
                refresh(context)
                delay(500)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionCard(
                icon = MaterialSymbols.RoundedFilled.Extension,
                title = stringResource(MR.strings.onboarding_permission_install_apps),
                subtitle = stringResource(MR.strings.onboarding_permission_install_apps_description),
                granted = installGranted,
                onButtonClick = {
                    context.launchRequestPackageInstallsPermission()
                },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { refresh(context) },
                )
                PermissionCard(
                    icon = MaterialSymbols.Rounded.Security,
                    title = stringResource(MR.strings.onboarding_permission_notifications),
                    subtitle = stringResource(MR.strings.onboarding_permission_notifications_description),
                    granted = notificationGranted,
                    onButtonClick = { permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            PermissionCard(
                icon = MaterialSymbols.Rounded.Download,
                title = stringResource(MR.strings.onboarding_permission_ignore_battery_opts),
                subtitle = stringResource(MR.strings.onboarding_permission_ignore_battery_opts_description),
                granted = batteryGranted,
                onButtonClick = {
                    @SuppressLint("BatteryLife")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                },
            )

            if (!telemetryIncluded) return@Column

            OnboardingSectionLabel(stringResource(MR.strings.onboarding_permissions_privacy), Modifier.padding(top = 8.dp))

            val crashlyticsPref = privacyPreferences.crashlytics
            val crashlytics by crashlyticsPref.collectAsState()
            PermissionSwitchCard(
                icon = MaterialSymbols.Rounded.BugReport,
                title = stringResource(MR.strings.onboarding_permission_crashlytics),
                subtitle = stringResource(MR.strings.onboarding_permission_crashlytics_description),
                checked = crashlytics,
                onToggleChange = crashlyticsPref::set,
            )

            val analyticsPref = privacyPreferences.analytics
            val analytics by analyticsPref.collectAsState()
            PermissionSwitchCard(
                icon = MaterialSymbols.Rounded.Storage,
                title = stringResource(MR.strings.onboarding_permission_analytics),
                subtitle = stringResource(MR.strings.onboarding_permission_analytics_description),
                checked = analytics,
                onToggleChange = analyticsPref::set,
            )
        }
    }

    @Composable
    private fun PermissionCard(
        icon: ImageVector,
        title: String,
        subtitle: String,
        granted: Boolean,
        onButtonClick: () -> Unit,
    ) {
        OnboardingActionCard(
            icon = icon,
            title = title,
            body = subtitle,
            checked = granted,
        ) {
            // Once granted the tick says it all, so the button goes away instead of lingering disabled.
            if (!granted) {
                OnboardingButton(
                    text = stringResource(MR.strings.onboarding_permission_action_grant),
                    onClick = onButtonClick,
                    style = OnboardingButtonStyle.Tonal,
                )
            }
        }
    }

    @Composable
    private fun PermissionSwitchCard(
        icon: ImageVector,
        title: String,
        subtitle: String,
        checked: Boolean,
        onToggleChange: (Boolean) -> Unit,
    ) {
        OnboardingCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OnboardingIconBubble(icon)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = checked, onCheckedChange = onToggleChange)
            }
        }
    }
}
