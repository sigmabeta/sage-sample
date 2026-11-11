package net.sigmabeta.sage.sample.perfbench

import org.junit.Assert.assertTrue

/**
 * JUnit-4 stand-in for `kotlin.test.assertIs`: asserts [value] is a [T] and returns it narrowed.
 *
 * The module uses JUnit 4 only (not kotlin-test), so the sealed-result assertions need this helper.
 */
inline fun <reified T> assertIs(value: Any?): T {
    assertTrue("Expected ${T::class.java.simpleName} but was $value", value is T)
    @Suppress("UNCHECKED_CAST")
    return value as T
}
