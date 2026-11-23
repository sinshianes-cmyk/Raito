package eu.kanade.tachiyomi.ui.home

/** The reorderable slots of the floating bar. The combined Recents tab takes the earlier of Updates / History. */
object HomeTabOrder {
    val DEFAULT = listOf("library", "updates", "history", "browse")

    /** Parses a stored order, dropping unknown keys and appending any missing ones. */
    fun parse(raw: String): List<String> {
        val seen = raw.split(',').map { it.trim() }.filter { it in DEFAULT }.distinct()
        return seen + DEFAULT.filter { it !in seen }
    }

    fun serialize(order: List<String>): String = order.joinToString(",")
}
