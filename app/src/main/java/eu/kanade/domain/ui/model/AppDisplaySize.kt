package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * Overall size of the app UI: text, buttons, icons and covers scale together because the factor is
 * applied to the Compose density. LARGE is the original size, MEDIUM (the default) is a little tighter.
 */
enum class AppDisplaySize(val scale: Float, val titleRes: StringResource) {
    SMALL(0.85f, MR.strings.display_size_small),
    MEDIUM(0.93f, MR.strings.display_size_medium),
    LARGE(1f, MR.strings.display_size_large),
}
