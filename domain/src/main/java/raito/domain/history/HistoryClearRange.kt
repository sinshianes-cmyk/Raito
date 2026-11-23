package raito.domain.history

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Which read records Clear history removes. Both content types answer every range. */
enum class HistoryClearRange {
    LAST_TWO_HOURS,
    TODAY,
    NOT_IN_LIBRARY,
    ALL,
    ;

    /** The cut-off for a range that is a window of time, null for one that is not. */
    fun since(now: Instant = Clock.System.now(), zone: TimeZone = TimeZone.currentSystemDefault()): Instant? = when (this) {
        LAST_TWO_HOURS -> now - 2.hours
        TODAY -> now.toLocalDateTime(zone).date.atStartOfDayIn(zone)
        NOT_IN_LIBRARY, ALL -> null
    }
}
