package eu.kanade.tachiyomi.ui.more

// RK: the More tab is gone. Its entries moved into the top bars' menus; Settings is the bar's gear.
sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
