package raito.data.track

import eu.kanade.tachiyomi.BuildConfig

/** The identifying user agent the trackers are sent. Shikimori IP-bans a missing or browser one. */
const val RAITO_TRACKER_USER_AGENT = "Raito v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
