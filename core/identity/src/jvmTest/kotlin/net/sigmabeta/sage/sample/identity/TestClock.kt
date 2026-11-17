package net.sigmabeta.sage.sample.identity

import kotlin.time.Clock
import kotlin.time.Instant

/** A clock the test moves by hand. */
class TestClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}
