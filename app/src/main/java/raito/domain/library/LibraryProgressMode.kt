package raito.domain.library

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/** How a library cover shows reading progress, drawn the same for manga and novels. */
enum class LibraryProgressMode(val titleRes: StringResource) {
    OFF(MR.strings.library_progress_off),
    PERCENT(MR.strings.library_progress_percent),
    CHAPTERS(MR.strings.library_progress_chapters),
}
