package raito.domain.novel

import dev.zacsweers.metro.Inject
import raito.domain.category.GetNovelCategories
import raito.domain.merge.GroupCategories
import raito.domain.novel.interactor.SetNovelCategories

/** Novels' [GroupCategories], over the novel category read and write. */
@Inject
class NovelGroupCategories(getNovelCategories: GetNovelCategories, setNovelCategories: SetNovelCategories) :
    GroupCategories(
        categoriesOf = { id -> getNovelCategories.awaitByNovelId(id).map { it.id } },
        write = { id, categoryIds -> setNovelCategories.await(id, categoryIds) },
    )
