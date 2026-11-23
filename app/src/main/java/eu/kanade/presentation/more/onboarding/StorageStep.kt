package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.rounded.Folder
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class StorageStep : OnboardingStep {

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val storagePref = remember { context.appGraph.storagePreferences.baseStorageDirectory }

        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The tick lights up once a folder is chosen, the one thing this step waits for.
            OnboardingActionCard(
                icon = MaterialSymbols.Rounded.Folder,
                checked = _isComplete,
                body = stringResource(
                    MR.strings.onboarding_storage_info,
                    stringResource(MR.strings.app_name),
                    SettingsDataScreen.storageLocationText(storagePref),
                ),
            ) {
                OnboardingButton(
                    text = stringResource(MR.strings.onboarding_storage_action_select),
                    onClick = {
                        try {
                            pickStorageLocation.launch(null)
                        } catch (e: ActivityNotFoundException) {
                            context.toast(MR.strings.file_picker_error)
                        }
                    },
                )
            }

            OnboardingActionCard(
                icon = MaterialSymbols.AutoMirroredRounded.Help,
                body = stringResource(MR.strings.onboarding_storage_help_info, stringResource(MR.strings.app_name)),
            ) {
                OnboardingButton(
                    text = stringResource(MR.strings.onboarding_storage_help_action),
                    onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
                    style = OnboardingButtonStyle.Tonal,
                    icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
                )
            }
        }

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
