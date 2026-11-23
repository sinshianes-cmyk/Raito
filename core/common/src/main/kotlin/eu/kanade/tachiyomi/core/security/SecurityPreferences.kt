package eu.kanade.tachiyomi.core.security

import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR

@Inject
@SingleIn(AppScope::class)
class SecurityPreferences(
    preferenceStore: PreferenceStore,
) {

    val useAuthenticator: Preference<Boolean> = preferenceStore.getBoolean("use_biometric_lock", false)

    // RK --> custom PIN lock. Only a salted PBKDF2 hash is stored, under private keys so backups skip them.
    val appPinHash: Preference<String> = preferenceStore.getString(Preference.privateKey("app_pin_hash"), "")

    val appPinSalt: Preference<String> = preferenceStore.getString(Preference.privateKey("app_pin_salt"), "")

    fun hasPin() = appPinHash.get().isNotEmpty()

    fun setPin(pin: String) {
        val salt = PinHasher.newSalt()
        appPinSalt.set(salt)
        appPinHash.set(PinHasher.hash(pin, salt))
    }

    fun clearPin() {
        appPinHash.delete()
        appPinSalt.delete()
    }

    fun checkPin(pin: String) = hasPin() && PinHasher.hash(pin, appPinSalt.get()) == appPinHash.get()
    // RK <--

    val lockAppAfter: Preference<Int> = preferenceStore.getInt("lock_app_after", 0)

    val secureScreen: Preference<SecureScreenMode> = preferenceStore.getEnum(
        "secure_screen_v2",
        SecureScreenMode.INCOGNITO,
    )

    val hideNotificationContent: Preference<Boolean> = preferenceStore.getBoolean("hide_notification_content", false)

    // RK: hide adult (NSFW-source) titles + covers from notifications and the lock screen. Default on.
    val hideAdultNotificationContent: Preference<Boolean> =
        preferenceStore.getBoolean("hide_adult_notification_content", true)

    /**
     * For app lock. Will be set when there is a pending timed lock.
     * Otherwise, this pref should be deleted.
     */
    val lastAppClosed: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("last_app_closed"),
        0,
    )

    enum class SecureScreenMode(val titleRes: StringResource) {
        ALWAYS(MR.strings.lock_always),
        INCOGNITO(MR.strings.pref_incognito_mode),
        NSFW(MR.strings.screenshots_block_nsfw), // RK: secure only on adult-source content
        NEVER(MR.strings.lock_never),
    }
}
