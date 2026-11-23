package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import raito.domain.merge.ChapterGap
import raito.domain.merge.toGapNeighbour

fun calculateChapterGap(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Int {
    // RK: the chapter list's rule, so two sources of a merged series are never compared, a number the
    // series carries elsewhere is not missing, and both readers mark the boundaries it does.
    return ChapterGap.atSeam(
        higherReaderChapter?.chapter?.toGapNeighbour(),
        lowerReaderChapter?.chapter?.toGapNeighbour(),
        (higherReaderChapter ?: lowerReaderChapter)?.gapPresent ?: ChapterGap.Present.NONE,
    )
}
