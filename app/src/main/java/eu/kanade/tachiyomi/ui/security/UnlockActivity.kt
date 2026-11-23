package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.view.setComposeContent // RK
import mihon.app.di.appGraph // RK
import raito.presentation.security.PinUnlockScreen // RK
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.startAuthentication
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR

/**
 * Blank activity with a BiometricPrompt.
 */
class UnlockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // RK --> PIN method: show a PIN pad instead of the system prompt
        val securityPreferences = appGraph.securityPreferences
        if (securityPreferences.hasPin()) {
            setComposeContent {
                PinUnlockScreen(
                    check = securityPreferences::checkPin,
                    onUnlocked = {
                        SecureActivityDelegate.unlock()
                        finish()
                    },
                    onCancel = { finishAffinity() },
                )
            }
            return
        }
        // RK <--
        startAuthentication(
            stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
            confirmationRequired = false,
            callback = object : AuthenticatorUtil.AuthenticationCallback() {
                override fun onAuthenticationError(
                    activity: FragmentActivity?,
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(activity, errorCode, errString)
                    logcat(LogPriority.ERROR) { errString.toString() }
                    finishAffinity()
                }

                override fun onAuthenticationSucceeded(
                    activity: FragmentActivity?,
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(activity, result)
                    SecureActivityDelegate.unlock()
                    finish()
                }
            },
        )
    }
}
