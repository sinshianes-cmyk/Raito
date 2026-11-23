package raito.domain.history

import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class HistoryClearRangeTest {

    private val now = Instant.parse("2026-11-21T20:30:00Z")

    @Test
    fun `last two hours cuts off two hours back`() {
        HistoryClearRange.LAST_TWO_HOURS.since(now, TimeZone.UTC) shouldBe Instant.parse("2026-11-21T18:30:00Z")
    }

    @Test
    fun `today cuts off at the start of the local day`() {
        HistoryClearRange.TODAY.since(now, TimeZone.UTC) shouldBe Instant.parse("2026-11-21T00:00:00Z")
    }

    @Test
    fun `ranges that are not a window have no cut-off`() {
        HistoryClearRange.NOT_IN_LIBRARY.since(now, TimeZone.UTC) shouldBe null
        HistoryClearRange.ALL.since(now, TimeZone.UTC) shouldBe null
    }
}
