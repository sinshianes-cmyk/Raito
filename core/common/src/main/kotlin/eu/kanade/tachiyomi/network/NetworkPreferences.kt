package eu.kanade.tachiyomi.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.interceptor.FLARESOLVERR_URL_KEY
import mihon.core.metro.IsDebugBuild
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

@Inject
@SingleIn(AppScope::class)
class NetworkPreferences(
    preferenceStore: PreferenceStore,
    @IsDebugBuild isDebugBuild: Boolean,
) {

    val redactSensitiveHeaders: Preference<Boolean> = preferenceStore.getBoolean(
        "redact_sensitive_headers",
        true,
    )

    val verboseLogging: Preference<Boolean> = preferenceStore.getBoolean(
        "verbose_logging",
        isDebugBuild,
    )

    val dohProvider: Preference<Int> = preferenceStore.getInt("doh_provider", -1)

    val defaultUserAgent: Preference<String> = preferenceStore.getString(
        "default_user_agent",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36",
    )

    // RK -->
    // 0 Disabled, 1 HTTP, 2 SOCKS. Password is private so a backup does not carry it.
    val proxyType: Preference<Int> = preferenceStore.getInt("proxy_type", 0)

    val proxyAddress: Preference<String> = preferenceStore.getString("proxy_address", "")

    val proxyPort: Preference<String> = preferenceStore.getString("proxy_port", "")

    val proxyLogin: Preference<String> = preferenceStore.getString("proxy_login", "")

    val proxyPassword: Preference<String> = preferenceStore.getString(Preference.privateKey("proxy_password"), "")

    val ignoreSslErrors: Preference<Boolean> = preferenceStore.getBoolean("ignore_ssl_errors", false)

    // -1 none, 0 wsrv.nl, 1 0ms.dev
    val imageProxy: Preference<Int> = preferenceStore.getInt("image_proxy", -1)

    val enableFlareSolverr: Preference<Boolean> = preferenceStore.getBoolean("enable_flaresolverr", false)

    val flareSolverrUrl: Preference<String> = preferenceStore.getString(FLARESOLVERR_URL_KEY, "")

    // A proxy in front of the server may want basic auth. Both keys are private, as the tracker
    // logins are, so a backup does not carry them unless the reader opts into private settings.
    val flareSolverrUsername: Preference<String> =
        preferenceStore.getString(Preference.privateKey("flaresolverr_username"), "")

    val flareSolverrPassword: Preference<String> =
        preferenceStore.getString(Preference.privateKey("flaresolverr_password"), "")

    val enableTurnstileSolver: Preference<Boolean> = preferenceStore.getBoolean("enable_turnstile_solver", false)

    val enableTurnstileBackgroundSolver: Preference<Boolean> =
        preferenceStore.getBoolean("enable_turnstile_background_solver", false)
    // RK <--
}
