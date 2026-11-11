package net.sigmabeta.sage.sample.perfbench

/**
 * The canonical UI scenarios the device benchmark measures.
 *
 * The [id] is the scenario key stored in a [UiResult] and matched by a baseline diff; it is stable
 * across runs and devices. Each scenario scrolls one core surface, reached from the app's own deep
 * link so the benchmark starts on the screen rather than driving the navigation UI first.
 */
enum class UiScenario(val id: String) {
    HOME_SCROLL("ui.home_scroll"),
    LIST_ITEMS_SCROLL("ui.list_items_scroll"),
    GRID_OF_IMAGES_SCROLL("ui.grid_of_images_scroll"),
    CONTENT_DETAIL_SCROLL("ui.content_detail_scroll"),
    ;

    companion object {
        /** Every scenario, in the order the harness reports them. */
        val all: List<UiScenario> = entries.toList()

        /** Resolves an [id] such as `ui.home_scroll`, or `null` if it names no scenario. */
        fun fromId(id: String): UiScenario? = entries.firstOrNull { it.id == id }
    }
}
