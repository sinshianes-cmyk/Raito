package raito.presentation.shortcuts

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import raito.presentation.details.mangaDetailsIntent
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.history.repository.HistoryRepository

/**
 * Publishes the last few read manga as dynamic launcher shortcuts. Cleared when the pref is off or
 * incognito is on, so nothing read privately leaks to the launcher.
 */
object RecentShortcuts {

    private const val MAX = 3
    private const val ID_PREFIX = "recent_manga_"

    fun start(
        context: Context,
        scope: CoroutineScope,
        history: HistoryRepository,
        uiPreferences: UiPreferences,
        basePreferences: BasePreferences,
    ) {
        combine(uiPreferences.recentShortcuts.changes(), basePreferences.incognitoMode.changes()) { on, incognito ->
            on && !incognito
        }
            .flatMapLatest { enabled ->
                if (enabled) history.getHistory("") else flowOf(null)
            }
            .onEach { rows ->
                runCatching {
                    if (rows == null) {
                        ShortcutManagerCompat.removeDynamicShortcuts(context, currentIds(context))
                        return@runCatching
                    }
                    val shortcuts = rows
                        .filter { it.readAt != null }
                        .distinctBy { it.mangaId }
                        .take(MAX)
                        .map { row ->
                            ShortcutInfoCompat.Builder(context, "$ID_PREFIX${row.mangaId}")
                                .setShortLabel(row.title)
                                .setLongLabel(row.title)
                                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                                .setIntent(mangaDetailsIntent(context, row.mangaId))
                                .build()
                        }
                    ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
                }.onFailure { logcat(LogPriority.WARN, it) }
            }
            .launchIn(scope)
    }

    private fun currentIds(context: Context): List<String> =
        ShortcutManagerCompat.getDynamicShortcuts(context).map { it.id }.filter { it.startsWith(ID_PREFIX) }

}
