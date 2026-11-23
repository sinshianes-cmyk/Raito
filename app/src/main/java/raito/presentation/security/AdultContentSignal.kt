package raito.presentation.security

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * True while a reader shows adult-source content. The secure-screen "Block on NSFW" mode reads it,
 * so screenshots are blocked only while such an entry is open. Readers set it and clear it on close.
 */
object AdultContentSignal {
    val active = MutableStateFlow(false)
}
