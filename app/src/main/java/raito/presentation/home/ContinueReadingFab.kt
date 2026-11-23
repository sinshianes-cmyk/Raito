package raito.presentation.home

import android.content.Context
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Instant

/** Opens the newest read across manga and novels, hidden while incognito or when the setting is off. */
@Composable
fun ContinueReadingFab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val graph = remember { context.appGraph }
    val enabled by remember { graph.uiPreferences.showContinueButton }.collectAsState()
    val incognito by remember { graph.basePreferences.incognitoMode }.collectAsState()
    if (!enabled || incognito) return
    val scope = rememberCoroutineScope()
    FloatingActionButton(
        modifier = modifier,
        onClick = {
            scope.launch {
                val intent = resumeIntent(context)
                if (intent != null) context.startActivity(intent) else context.toast(MR.strings.no_next_chapter)
            }
        },
    ) {
        Icon(MaterialSymbols.RoundedFilled.PlayArrow, contentDescription = stringResource(MR.strings.action_resume))
    }
}

/**
 * The reader intent for the series read last. A manga resumes where the history chapter left off, or
 * moves on to the next one when that chapter is finished; a novel reopens its history chapter.
 */
private suspend fun resumeIntent(context: Context): android.content.Intent? {
    val graph = context.appGraph
    val manga = graph.getHistory.getLast()
    // Novels off: the button only ever resumes a manga.
    val novel = if (graph.raitoSourcePreferences.novelsEnabled.get()) graph.getNovelHistory.getLast() else null
    val novelIsNewer = novel != null && (manga == null || (novel.readAt ?: Instant.fromEpochMilliseconds(0)) >
        (manga.readAt ?: Instant.fromEpochMilliseconds(0)))
    return when {
        novelIsNewer -> ReaderActivity.newNovelIntent(context, novel!!.novelId, novel.chapterId)
        manga != null -> {
            val chapterId = graph.getNextChapters.await(manga.mangaId, manga.chapterId, onlyUnread = false)
                .firstOrNull()?.id ?: manga.chapterId
            ReaderActivity.newIntent(context, manga.mangaId, chapterId)
        }
        else -> null
    }
}
