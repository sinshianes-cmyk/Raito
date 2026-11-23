package raito.domain.source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import raito.domain.library.ContentType
import tachiyomi.core.common.preference.Preference

/** The type a chip shows: Manga whatever was stored while novels are off. */
fun ContentType.whenNovels(enabled: Boolean): ContentType = if (enabled) this else ContentType.MANGA

/**
 * A stored content-type filter that reads as [ContentType.MANGA] while novels are switched off, so every
 * surface that shows it drops its novel half without each one knowing about the switch. The stored value
 * is left alone, so turning novels back on restores the chip the person last used.
 */
class NovelAwareContentTypePreference(
    private val delegate: Preference<ContentType>,
    private val novelsEnabled: Preference<Boolean>,
) : Preference<ContentType> {

    override fun key() = delegate.key()

    override fun get(): ContentType = delegate.get().whenNovels(novelsEnabled.get())

    override fun set(value: ContentType) {
        // Off: a write cannot pick novels, and the stored chip is kept so turning novels back on restores it.
        if (novelsEnabled.get()) delegate.set(value)
    }

    override fun isSet() = delegate.isSet()

    override fun delete() = delegate.delete()

    override fun defaultValue(): ContentType = delegate.defaultValue()

    override fun changes(): Flow<ContentType> =
        combine(delegate.changes(), novelsEnabled.changes()) { type, enabled -> type.whenNovels(enabled) }
            .distinctUntilChanged()

    override fun stateIn(scope: CoroutineScope): StateFlow<ContentType> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())
}
