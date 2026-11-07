package net.sigmabeta.sage.sample.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Closed-set parsers and defaults: every entry, plus missing and unrecognised values. */
class SettingsModelsTest {
    @Test
    fun `theme mode parses every stored name`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageValue("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageValue("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue("SYSTEM"))
    }

    @Test
    fun `theme mode falls back to system for missing or unknown values`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue("BOGUS"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DEFAULT)
    }

    @Test
    fun `theme mode resolves dark only for dark or a dark system`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertTrue(ThemeMode.DARK.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
    }

    @Test
    fun `font choice parses every stored name`() {
        assertEquals(FontChoice.STANDARD, FontChoice.fromStorageValue("STANDARD"))
        assertEquals(FontChoice.INTER, FontChoice.fromStorageValue("INTER"))
    }

    @Test
    fun `font choice falls back to standard for missing or unknown values`() {
        assertEquals(FontChoice.STANDARD, FontChoice.fromStorageValue(null))
        assertEquals(FontChoice.STANDARD, FontChoice.fromStorageValue(""))
        assertEquals(FontChoice.STANDARD, FontChoice.fromStorageValue("BOGUS"))
        assertEquals(FontChoice.STANDARD, FontChoice.DEFAULT)
    }

    @Test
    fun `simulate delay parses every stored name and keeps its millis`() {
        assertEquals(SimulateDelay.NONE, SimulateDelay.fromStorageValue("NONE"))
        assertEquals(SimulateDelay.ONE_SECOND, SimulateDelay.fromStorageValue("ONE_SECOND"))
        assertEquals(SimulateDelay.TWO_SECONDS, SimulateDelay.fromStorageValue("TWO_SECONDS"))
        assertEquals(SimulateDelay.FIVE_SECONDS, SimulateDelay.fromStorageValue("FIVE_SECONDS"))

        assertEquals(0L, SimulateDelay.NONE.millis)
        assertEquals(1_000L, SimulateDelay.ONE_SECOND.millis)
        assertEquals(2_000L, SimulateDelay.TWO_SECONDS.millis)
        assertEquals(5_000L, SimulateDelay.FIVE_SECONDS.millis)
    }

    @Test
    fun `simulate delay falls back to none for missing or unknown values`() {
        assertEquals(SimulateDelay.NONE, SimulateDelay.fromStorageValue(null))
        assertEquals(SimulateDelay.NONE, SimulateDelay.fromStorageValue(""))
        assertEquals(SimulateDelay.NONE, SimulateDelay.fromStorageValue("BOGUS"))
        assertEquals(SimulateDelay.NONE, SimulateDelay.DEFAULT)
    }

    @Test
    fun `home shows same content parses persisted booleans and defaults to off`() {
        assertTrue(homeShowsSameContentFromStorage("true"))
        assertFalse(homeShowsSameContentFromStorage("false"))
        assertFalse(homeShowsSameContentFromStorage(null))
        assertFalse(homeShowsSameContentFromStorage(""))
        assertFalse(homeShowsSameContentFromStorage("BOGUS"))
        assertFalse(DEFAULT_HOME_SHOWS_SAME_CONTENT)
    }
}
