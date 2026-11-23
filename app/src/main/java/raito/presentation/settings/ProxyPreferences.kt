package raito.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PROXY_DISABLED
import eu.kanade.tachiyomi.network.PROXY_HTTP
import eu.kanade.tachiyomi.network.PROXY_SOCKS
import eu.kanade.tachiyomi.network.ProxyProvider
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.util.concurrent.TimeUnit

/** Proxy, SSL bypass and image-proxy rows for Settings -> Advanced -> Network. */
@Composable
fun proxyPreferenceItems(networkPreferences: NetworkPreferences): List<Preference.PreferenceItem<out Any, out Any>> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val type by networkPreferences.proxyType.collectAsState()
    val address by networkPreferences.proxyAddress.collectAsState()
    val port by networkPreferences.proxyPort.collectAsState()
    val enabled = type != PROXY_DISABLED
    val provider = remember { ProxyProvider(networkPreferences) }
    val invalidText = stringResource(MR.strings.proxy_invalid_configuration)
    val okText = stringResource(MR.strings.proxy_connection_ok)

    return listOf(
        Preference.PreferenceItem.ListPreference(
            preference = networkPreferences.proxyType,
            entries = mapOf(
                PROXY_DISABLED to stringResource(MR.strings.disabled),
                PROXY_HTTP to stringResource(MR.strings.proxy_http),
                PROXY_SOCKS to stringResource(MR.strings.proxy_socks),
            ),
            title = stringResource(MR.strings.pref_proxy_type),
        ),
        Preference.PreferenceItem.EditTextPreference(
            preference = networkPreferences.proxyAddress,
            title = stringResource(MR.strings.pref_proxy_address),
            visible = enabled,
        ),
        Preference.PreferenceItem.EditTextPreference(
            preference = networkPreferences.proxyPort,
            title = stringResource(MR.strings.pref_proxy_port),
            visible = enabled,
            onValueChanged = { it.isEmpty() || it.toIntOrNull() in 1..65535 },
        ),
        Preference.PreferenceItem.EditTextPreference(
            preference = networkPreferences.proxyLogin,
            title = stringResource(MR.strings.pref_proxy_username),
            visible = enabled,
        ),
        Preference.PreferenceItem.EditTextPreference(
            preference = networkPreferences.proxyPassword,
            title = stringResource(MR.strings.pref_proxy_password),
            subtitle = null, // never echo the password
            visible = enabled,
        ),
        Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.pref_proxy_test),
            subtitle = if (address.isBlank() || port.toIntOrNull() !in 1..65535) invalidText else "$address:$port",
            visible = enabled,
            onClick = {
                if (provider.currentProxy() == null) {
                    context.toast(invalidText)
                    return@TextPreference
                }
                scope.launch {
                    val error = withContext(Dispatchers.IO) {
                        runCatching {
                            val client = OkHttpClient.Builder()
                                .proxySelector(provider.selector)
                                .proxyAuthenticator(provider.authenticator)
                                .callTimeout(15, TimeUnit.SECONDS)
                                .build()
                            client.newCall(Request.Builder().url("https://www.google.com/generate_204").build())
                                .execute().use { check(it.isSuccessful) { "HTTP ${it.code}" } }
                        }.exceptionOrNull()
                    }
                    context.toast(if (error == null) okText else (error.message ?: error.toString()))
                }
            },
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = networkPreferences.ignoreSslErrors,
            title = stringResource(MR.strings.pref_ignore_ssl_errors),
            subtitle = stringResource(MR.strings.pref_ignore_ssl_errors_summary),
        ),
        Preference.PreferenceItem.ListPreference(
            preference = networkPreferences.imageProxy,
            entries = mapOf(
                -1 to stringResource(MR.strings.disabled),
                0 to "wsrv.nl",
                1 to "0ms.dev",
            ),
            title = stringResource(MR.strings.pref_image_proxy),
            subtitle = stringResource(MR.strings.pref_image_proxy_summary) + "\n%s",
        ),
    )
}
