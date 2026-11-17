package net.sigmabeta.sage.sample.identity

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SignInLimiterTest {
    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = TestClock(start)
    private val limiter = SignInLimiter(clock)

    @Test
    fun `a first attempt is allowed`() = runTest {
        assertEquals(SignInAllowance.Allowed, limiter.check("demo", "client-a"))
    }

    @Test
    fun `five failures for a username are still allowed and the sixth throttles it`() = runTest {
        repeat(5) { limiter.recordFailure("demo", "client-$it") }
        assertEquals(SignInAllowance.Allowed, limiter.check("demo", "client-new"))

        limiter.recordFailure("demo", "client-5")

        assertEquals(SignInAllowance.Throttled(60.seconds), limiter.check("demo", "client-new"))
    }

    @Test
    fun `five failures from a client are still allowed and the sixth throttles it`() = runTest {
        repeat(5) { limiter.recordFailure("user-$it", "client-a") }
        assertEquals(SignInAllowance.Allowed, limiter.check("user-new", "client-a"))

        limiter.recordFailure("user-5", "client-a")

        assertEquals(SignInAllowance.Throttled(60.seconds), limiter.check("user-new", "client-a"))
    }

    @Test
    fun `a throttled username is throttled from every client`() = runTest {
        repeat(6) { limiter.recordFailure("demo", "client-a") }

        assertEquals(SignInAllowance.Throttled(60.seconds), limiter.check("demo", "client-b"))
        assertEquals(SignInAllowance.Throttled(60.seconds), limiter.check("demo2", "client-a"))
        assertEquals(SignInAllowance.Allowed, limiter.check("demo2", "client-b"))
    }

    @Test
    fun `usernames and clients are separate keys`() = runTest {
        repeat(6) { limiter.recordFailure("same", "client-$it") }

        assertEquals(SignInAllowance.Allowed, limiter.check("other", "same"))
    }

    @Test
    fun `username keys match exactly`() = runTest {
        repeat(6) { limiter.recordFailure("demo", "client-$it") }

        assertEquals(SignInAllowance.Allowed, limiter.check("Demo", "client-new"))
        assertEquals(SignInAllowance.Allowed, limiter.check(" demo", "client-new"))
    }

    @Test
    fun `the window slides so the oldest failure ages out after a minute`() = runTest {
        repeat(6) {
            clock.now = start + (it * 10).seconds
            limiter.recordFailure("demo", "client-$it")
        }

        clock.now = start + 60.seconds - 1.milliseconds
        assertEquals(SignInAllowance.Throttled(1.milliseconds), limiter.check("demo", "client-new"))

        clock.now = start + 60.seconds
        assertEquals(SignInAllowance.Allowed, limiter.check("demo", "client-new"))
    }

    @Test
    fun `retry after is the time until enough failures age out`() = runTest {
        repeat(6) {
            clock.now = start + (it * 10).seconds
            limiter.recordFailure("demo", "client-$it")
        }

        clock.now = start + 55.seconds

        assertEquals(SignInAllowance.Throttled(5.seconds), limiter.check("demo", "client-new"))
    }

    @Test
    fun `retry after is the longer wait when both keys are throttled`() = runTest {
        repeat(6) { limiter.recordFailure("demo", "client-$it") }
        clock.now = start + 20.seconds
        repeat(6) { limiter.recordFailure("user-$it", "client-a") }

        clock.now = start + 30.seconds

        assertEquals(SignInAllowance.Throttled(50.seconds), limiter.check("demo", "client-a"))
    }

    @Test
    fun `failures older than the window no longer count`() = runTest {
        repeat(6) { limiter.recordFailure("demo", "client-a") }
        clock.now = start + 61.seconds

        repeat(5) { limiter.recordFailure("demo", "client-a") }

        assertEquals(SignInAllowance.Allowed, limiter.check("demo", "client-a"))
    }

    @Test
    fun `a success clears the username's count but not the client's`() = runTest {
        repeat(6) { limiter.recordFailure("demo", "client-a") }

        limiter.recordSuccess("demo")

        assertEquals(SignInAllowance.Allowed, limiter.check("demo", "client-b"))
        assertEquals(SignInAllowance.Throttled(60.seconds), limiter.check("demo", "client-a"))
    }
}
