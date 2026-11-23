package raito.domain.suggestions

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Preferences for the Browse suggestions row, switched from the Services screen. */
@Inject
@SingleIn(AppScope::class)
class SuggestionsPreferences(
    private val preferenceStore: PreferenceStore,
) {

    /** Off by default: the row costs a few source requests, so it is opt-in. */
    val enabled: Preference<Boolean> = preferenceStore.getBoolean("raito_suggestions_enabled", false)

    /** Leave titles already in the library out of the row. */
    val hideInLibrary: Preference<Boolean> =
        preferenceStore.getBoolean("raito_suggestions_hide_in_library", true)

    /** On: adult sources are never searched and adult titles never shown in the row. */
    val hideNsfw: Preference<Boolean> =
        preferenceStore.getBoolean("raito_suggestions_hide_nsfw", true)
}
