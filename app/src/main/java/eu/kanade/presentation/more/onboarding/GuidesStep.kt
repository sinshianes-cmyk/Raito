package eu.kanade.presentation.more.onboarding

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupViewModel
import eu.kanade.presentation.more.settings.screen.data.restoreErrorMessage
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreWorker
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.roundedfilled.MenuBook
import raito.presentation.browse.repos.DetailLine
import raito.domain.extension.shownSigningKey
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The last step. Starts as the getting-started guide and a Restore card; choosing a backup swaps in the
 * same options, install checks and errors as Settings' Restore screen, then follows the restore to its
 * end right here, so finishing onboarding lands on the library with the backup in place.
 */
internal class GuidesStep : OnboardingStep {

    private enum class Phase { Idle, Options, Running, Done, Failed }

    private var phase by mutableStateOf(Phase.Idle)
    private var pickedUri by mutableStateOf<String?>(null)
    private var restoreId by mutableStateOf<java.util.UUID?>(null)

    // A restore running in the background is the one thing worth holding the person back for.
    override val isComplete: Boolean get() = phase != Phase.Running

    override val incompleteMessage: StringResource get() = MR.strings.restoring_backup

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (phase) {
                Phase.Idle -> IdleCards()
                Phase.Options -> pickedUri?.let { OptionsCards(it) }
                Phase.Running -> RunningCard()
                Phase.Done -> DoneCard()
                Phase.Failed -> FailedCard()
            }
        }

        val id = restoreId
        if (phase == Phase.Running && id != null) {
            val context = LocalContext.current
            LaunchedEffect(id) {
                BackupRestoreWorker.stateOf(context.workManager, id).collect { state ->
                    when (state) {
                        WorkInfo.State.SUCCEEDED -> phase = Phase.Done
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> phase = Phase.Failed
                        else -> Unit
                    }
                }
            }
        }
    }

    @Composable
    private fun IdleCards() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current
        val scope = rememberCoroutineScope()

        val chooseBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                context.toast(MR.strings.file_null_uri_error)
                return@rememberLauncherForActivityResult
            }
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                logcat(LogPriority.ERROR, e)
            }
            pickedUri = uri.toString()
            phase = Phase.Options
        }

        OnboardingActionCard(
            icon = MaterialSymbols.RoundedFilled.MenuBook,
            body = stringResource(MR.strings.onboarding_guides_new_user, stringResource(MR.strings.app_name)),
        ) {
            OnboardingButton(
                text = stringResource(MR.strings.getting_started_guide),
                onClick = { handler.openUri(GETTING_STARTED_URL) },
                icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
            )
        }

        OnboardingActionCard(
            icon = MaterialSymbols.Rounded.Download,
            body = stringResource(MR.strings.onboarding_guides_returning_user, stringResource(MR.strings.app_name)),
        ) {
            OnboardingButton(
                text = stringResource(MR.strings.pref_restore_backup),
                style = OnboardingButtonStyle.Tonal,
                onClick = {
                    scope.launch {
                        if (BackupRestoreWorker.isRunning(context.workManager)) {
                            context.toast(MR.strings.restore_in_progress)
                            return@launch
                        }
                        if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                            context.toast(MR.strings.restore_miui_warning)
                        }
                        chooseBackup.launch(arrayOf("*/*"))
                    }
                },
            )
        }
    }

    @Composable
    private fun OptionsCards(uri: String) {
        val context = LocalContext.current
        val viewModel = assistedMetroViewModel<RestoreBackupViewModel, RestoreBackupViewModel.Factory> {
            create(uri = uri)
        }
        // The model outlives a pick, so point it at the file just chosen.
        LaunchedEffect(uri) { viewModel.reload(uri) }
        val state by viewModel.state.collectAsState()

        if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
            WarningBanner(MR.strings.restore_miui_warning)
        }

        if (state.canRestore) {
            OnboardingCard {
                Text(
                    text = stringResource(MR.strings.pref_restore_backup),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                RestoreOptions.options.forEach { option ->
                    OnboardingCheckRow(
                        label = stringResource(option.label),
                        checked = option.getter(state.options),
                        onCheckedChange = { viewModel.toggle(option.setter, it) },
                    )
                }
            }

            // What the restore installs, and the backup's repos this device lacks, each to trust
            if (state.installs.ready.isNotEmpty()) {
                OnboardingCard {
                    Text(
                        text = stringResource(
                            if (state.options.extensions) MR.strings.restore_installs_ready else MR.strings.restore_installs_off,
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    state.installs.ready.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            state.installs.repos.forEach { repo ->
                OnboardingCard {
                    Text(repo.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        stringResource(if (repo.isPluginRepo) MR.strings.repo_kind_plugins else MR.strings.repo_kind_store),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(MR.strings.restore_repo_untrusted), style = MaterialTheme.typography.bodyMedium)
                    DetailLine(stringResource(MR.strings.repo_address), repo.url)
                    repo.signingKey?.let(::shownSigningKey)?.let {
                        DetailLine(stringResource(MR.strings.repo_signing_key), it)
                    }
                    DetailLine(
                        stringResource(MR.strings.restore_repo_installs),
                        repo.items.joinToString(separator = "\n").ifEmpty { stringResource(MR.strings.restore_repo_nothing) },
                    )
                    OnboardingCheckRow(
                        label = stringResource(MR.strings.restore_repo_trust),
                        checked = repo.url in state.trusted,
                        onCheckedChange = { viewModel.trust(repo, it) },
                    )
                }
            }
        }

        if (state.error != null) {
            OnboardingCard {
                SelectionContainer {
                    Text(text = restoreErrorMessage(state.error), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.canRestore) {
            OnboardingButton(
                text = stringResource(MR.strings.action_restore),
                onClick = {
                    restoreId = viewModel.startRestore()
                    phase = Phase.Running
                },
                enabled = state.options.canRestore(),
            )
        }
        OnboardingButton(
            text = stringResource(MR.strings.action_cancel),
            style = OnboardingButtonStyle.Tonal,
            onClick = {
                pickedUri = null
                phase = Phase.Idle
            },
        )
    }

    @Composable
    private fun RunningCard() {
        OnboardingActionCard(
            icon = MaterialSymbols.Rounded.Download,
            title = stringResource(MR.strings.restoring_backup),
            body = stringResource(MR.strings.onboarding_restore_working_hint),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
            )
        }
    }

    @Composable
    private fun DoneCard() {
        OnboardingActionCard(
            icon = MaterialSymbols.Rounded.Download,
            title = stringResource(MR.strings.restore_completed),
            body = stringResource(MR.strings.onboarding_restore_done_hint),
            checked = true,
        ) {}
    }

    @Composable
    private fun FailedCard() {
        OnboardingActionCard(
            icon = MaterialSymbols.Rounded.Download,
            title = stringResource(MR.strings.restoring_backup_error),
            body = stringResource(MR.strings.onboarding_guides_returning_user, stringResource(MR.strings.app_name)),
            checked = false,
        ) {
            OnboardingButton(
                text = stringResource(MR.strings.action_retry),
                onClick = { phase = Phase.Options },
            )
            OnboardingButton(
                text = stringResource(MR.strings.action_cancel),
                style = OnboardingButtonStyle.Tonal,
                onClick = {
                    pickedUri = null
                    phase = Phase.Idle
                },
            )
        }
    }
}

const val GETTING_STARTED_URL = "${Constants.URL_DOCS}/guides/getting-started" // RK: Raito's docs

@PreviewLightDark
@Composable
private fun GuidesStepPreview() {
    TachiyomiPreviewTheme {
        GuidesStep().Content()
    }
}
