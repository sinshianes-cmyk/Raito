package raito.presentation.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import raito.domain.category.isHidden
import raito.domain.category.withoutSystemCategory
import raito.domain.entry.EntryId
import raito.domain.entry.signedKey
import raito.domain.library.CategorySortOrder
import raito.domain.library.ContentType
import raito.domain.library.LibrarySortFields
import raito.domain.library.librarySortComparator
import raito.domain.library.sortForCategory
import raito.domain.library.toSortMode
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibrarySort
import java.util.concurrent.ConcurrentHashMap

/**
 * The assembly inputs besides the rows and categories. One value each: these describe the list, not a
 * content type, per the All-first rule.
 */
data class LibraryAssemblyInputs(
    val globalSort: LibrarySort,
    val randomSeed: Long,
    val showHiddenCategories: Boolean,
    /** Applied via [raitoSortCategories]. */
    val categorySortOrder: CategorySortOrder,
)

/**
 * One list assembly for every chip: bucket into categories, order them, sort within each (per-category
 * override or the global sort), then apply the empty-category rules. The chips only change what [rows]
 * and [categories] contain; the algorithm is chip-blind, and buckets before sorting because the sort is
 * per category. Values are [LibraryItem]s, never raw ids: a manga and a novel can share a raw table id,
 * so any Long-keyed structure over a mixed list silently cross-wires. The custom-info overlay is NOT
 * applied here. Rules pinned by LibraryAssemblyTest; record: docs/dev/subsystems/library.md.
 */
fun assembleLibrary(
    rows: List<LibraryItem>,
    categories: List<Category>,
    inputs: LibraryAssemblyInputs,
    fields: LibrarySortFields<LibraryItem>,
): List<Pair<LibraryBucket, List<LibraryItem>>> {
    val buckets = HashMap<Long, MutableList<LibraryItem>>()
    rows.forEach { item ->
        val categoryIds = item.libraryManga.categories.withoutSystemCategory()
        if (categoryIds.isEmpty()) {
            buckets.getOrPut(Category.UNCATEGORIZED_ID) { mutableListOf() }.add(item)
        } else {
            categoryIds.forEach { buckets.getOrPut(it) { mutableListOf() }.add(item) }
        }
    }

    return categories
        .distinctBy { it.id }
        .filter { inputs.showHiddenCategories || !it.isHidden }
        .let { raitoSortCategories(it, inputs.categorySortOrder) }
        .mapNotNull { category ->
            val bucket = buckets[category.id] ?: return@mapNotNull null
            val sort = sortForCategory(category, inputs.globalSort)
            val comparator =
                librarySortComparator(sort.type.toSortMode(), sort.isAscending, inputs.randomSeed, fields)
            LibraryBucket.Real(category) to bucket.sortedWith(comparator)
        }
}

/**
 * Every entry an assembly kept, which is what a selection is pruned to. Taken from the buckets rather
 * than the rows they were built from, because a filter is not the only thing that can drop an entry: a
 * hidden category, a bucket left empty and a lagging dynamic group all do, and each of those leaves the
 * entry sitting in the rows, where a verb would still find and act on something nobody can see.
 */
fun presentIdsOf(assembled: List<Pair<LibraryBucket, List<LibraryItem>>>): Set<EntryId> =
    assembled.flatMapTo(HashSet()) { (_, items) -> items.map { it.entryId } }

/**
 * The assembled list the tab renders: the ordered buckets, the per-bucket display read as a function
 * so the per-type custom-info overlay is applied at read time, only for what is rendered, and the one
 * count rule: the chip-filtered bucket size, shown when the count preference is on or a search is
 * active, on every chip.
 * [chip] is the view this output was assembled for: the flow lags a chip flip by one emission, so the
 * tab renders an assembly only when its chip matches, falling back to the provider's own list meanwhile.
 */
class LibraryAssembled(
    val chip: ContentType,
    val buckets: List<LibraryBucket>,
    /**
     * Everything this assembly kept, which is what a selection is pruned to. Held as a plain set
     * rather than read back through [items], because that read applies the display overlay, and the
     * overlay must never reach a decision about which entries exist.
     */
    val presentIds: Set<EntryId>,
    private val items: (LibraryBucket) -> List<LibraryItem>,
    private val counts: (LibraryBucket) -> Int? = { null },
) {
    // The toolbar and the grid read these every frame, so each bucket's overlay pass runs once. Safe
    // because an overlay edit re-emits a new assembly (the provider's overlay key is an assembly input).
    private val itemsByKey = ConcurrentHashMap<String, List<LibraryItem>>()

    fun itemsFor(bucket: LibraryBucket): List<LibraryItem> = itemsByKey.getOrPut(bucket.key) { items(bucket) }
    fun countFor(bucket: LibraryBucket): Int? = counts(bucket)
}

/**
 * Sort fields for a mixed list: [libraryItemSortFields] with a type-unique id key. The comparator's only
 * id use is the Random rank, `Random(seed + id)`, and a manga and a novel sharing a raw id would rank
 * identically, gluing the pair together under Random. [signedKey] keeps the two apart; it perturbs only
 * the Random rank, never an identity.
 */
fun mixedLibraryItemSortFields(trackerMean: (LibraryItem) -> Double?): LibrarySortFields<LibraryItem> {
    val base = libraryItemSortFields(trackerMean)
    return LibrarySortFields(
        id = { it.entryId.signedKey() },
        title = base.title,
        lastRead = base.lastRead,
        lastUpdate = base.lastUpdate,
        unreadCount = base.unreadCount,
        totalChapters = base.totalChapters,
        latestUpload = base.latestUpload,
        chapterFetchedAt = base.chapterFetchedAt,
        dateAdded = base.dateAdded,
        downloadCount = base.downloadCount,
        trackerMean = base.trackerMean,
    )
}
