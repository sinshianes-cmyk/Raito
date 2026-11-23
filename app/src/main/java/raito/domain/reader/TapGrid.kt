package raito.domain.reader

/**
 * The Custom tap layout: nine cells, row by row, each P (previous), N (next) or M (menu). Both readers keep
 * the one grid, so a layout drawn once reads the same in either.
 */
/** The preference key both readers store the grid under. */
const val TAP_CUSTOM_GRID_KEY = "reader_tap_custom_grid"

object TapGrid {
    const val DEFAULT = "PPPPMNNNN"
    const val SIZE = 9
    private const val ORDER = "PNM"

    /** [grid] when it is nine known cells with a menu among them, else [DEFAULT]: with no menu cell the chrome could not open. */
    fun normalize(grid: String): String =
        if (grid.length == SIZE && grid.all { it in ORDER } && 'M' in grid) grid else DEFAULT

    /** [grid] with the cell at [index] moved to its next action; the last menu cell stays, so the menu stays reachable. */
    fun cycle(grid: String, index: Int): String {
        val cells = normalize(grid).toCharArray()
        val next = ORDER[(ORDER.indexOf(cells[index]) + 1) % ORDER.length]
        if (cells[index] == 'M' && cells.count { it == 'M' } == 1) return String(cells)
        cells[index] = next
        return String(cells)
    }
}
