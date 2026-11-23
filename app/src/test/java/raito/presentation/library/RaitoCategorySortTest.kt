package raito.presentation.library

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import raito.domain.library.CategorySortOrder
import tachiyomi.domain.category.model.Category

class RaitoCategorySortTest {

    // Manual, A to Z and Z to A each give a different order, so no sort can pass for another.
    private val system = Category(id = 0, name = "", order = -1, flags = 0)
    private val alpha = Category(id = 1, name = "alpha", order = 2, flags = 0)
    private val beta = Category(id = 2, name = "Beta", order = 3, flags = 0)
    private val charlie = Category(id = 3, name = "charlie", order = 1, flags = 0)

    @Test
    fun `manual order follows each category's order, not the list's`() {
        raitoSortCategories(
            listOf(alpha, system, beta, charlie),
            sortOrder = CategorySortOrder.MANUAL,
        ) shouldContainExactly
            listOf(system, charlie, alpha, beta)
    }

    @Test
    fun `A to Z keeps the system category on top`() {
        raitoSortCategories(
            listOf(beta, charlie, alpha, system),
            sortOrder = CategorySortOrder.A_TO_Z,
        ) shouldContainExactly
            listOf(system, alpha, beta, charlie)
    }

    @Test
    fun `Z to A keeps the system category on top`() {
        raitoSortCategories(
            listOf(alpha, system, beta, charlie),
            sortOrder = CategorySortOrder.Z_TO_A,
        ) shouldContainExactly
            listOf(system, charlie, beta, alpha)
    }
}
