package raito.presentation.recommendation

import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import raito.domain.library.ContentType
import raito.domain.recommendation.RelatedMangaCandidate
import raito.presentation.browse.globalsearch.EntryGlobalSearchScreen

/**
 * Where a tapped related card opens, for the carousel and See all alike. [localId] is the candidate
 * resolved to a stored manga; a tracker-origin card has no installed source to resolve to, so it
 * searches for its title instead.
 */
fun relatedDestination(candidate: RelatedMangaCandidate, localId: Long?): Screen =
    if (localId != null) {
        MangaScreen(localId)
    } else {
        EntryGlobalSearchScreen(candidate.manga.title, scopedContentType = ContentType.MANGA)
    }
