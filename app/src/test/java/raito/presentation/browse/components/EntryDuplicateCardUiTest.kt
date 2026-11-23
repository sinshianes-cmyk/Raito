package raito.presentation.browse.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import raito.domain.novel.model.Novel
import raito.domain.novel.model.NovelWithChapterCount
import raito.domain.novel.model.asNovelCover

class EntryDuplicateCardUiTest {

    @Test
    fun `a novel duplicate card draws the row's own cover`() {
        val novel = Novel.create().copy(id = 4L, source = "src", thumbnailUrl = "https://example.org/4.jpg")

        NovelWithChapterCount(novel, 0L).toDuplicateCard(emptyMap()).coverModel shouldBe novel.asNovelCover()
    }
}
