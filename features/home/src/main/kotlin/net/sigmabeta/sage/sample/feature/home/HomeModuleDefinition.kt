package net.sigmabeta.sage.sample.feature.home

/**
 * A Home row implementation registered with the app graph.
 *
 * The graph contributes one definition per row type through Metro's `@Multibinds Set` +
 * `@Binds @IntoSet` (see [HomeModuleBindings]); adding a row is a single binding, not a change to
 * a factory switch. [HomeModuleFactory] turns the registered definitions into a per-screen plan:
 * stable mode instances every definition once in [priority] order, randomized mode fills 2–10
 * plan slots, repeating definitions as needed.
 */
interface HomeModuleDefinition {
    /** Stable type key; instance ids are built as `"$typeId_$instanceNumber"`. */
    val typeId: String

    /** Lower values render earlier when Home builds its stable plan. */
    val priority: Int

    /**
     * Builds one instance at a 1-based plan position.
     *
     * [instanceNumber] identifies the instance uniquely within its plan, so repeated types get
     * distinct ids and section titles. [dataIdBase] offsets this instance's item ids, keeping
     * repeated module types out of each other's range.
     */
    fun create(instanceNumber: Int, dataIdBase: Long): HomeModule
}
