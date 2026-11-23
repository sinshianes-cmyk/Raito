package raito.presentation.recents

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RowProgressTest {

    @Test
    fun `a series with every chapter read is done`() {
        rowProgress(SeriesProgress(chaptersRead = 12, chapterCount = 12)) shouldBe RowProgress.Done
    }

    @Test
    fun `a row with no series counts draws no bar`() {
        rowProgress(null) shouldBe null
        rowProgress(SeriesProgress(chaptersRead = 0, chapterCount = 0)) shouldBe null
    }

    @Test
    fun `a series part way through draws its share`() {
        rowProgress(SeriesProgress(chaptersRead = 1, chapterCount = 4)) shouldBe RowProgress.Partial(0.25f, 25)
    }

    @Test
    fun `a series nothing has been read from draws an empty bar`() {
        rowProgress(SeriesProgress(chaptersRead = 0, chapterCount = 30)) shouldBe RowProgress.Partial(0f, 0)
    }

    @Test
    fun `a series one chapter short never rounds up to 100 percent`() {
        rowProgress(SeriesProgress(chaptersRead = 299, chapterCount = 300))
            .let { (it as RowProgress.Partial).percent } shouldBe 99
    }

    @Test
    fun `a read count past the total is clamped rather than overflowing the bar`() {
        rowProgress(SeriesProgress(chaptersRead = 9, chapterCount = 5)) shouldBe RowProgress.Done
    }
}
