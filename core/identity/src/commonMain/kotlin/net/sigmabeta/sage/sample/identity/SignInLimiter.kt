package net.sigmabeta.sage.sample.identity

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Whether a Sign in attempt may go on to the password check. */
sealed interface SignInAllowance {
    data object Allowed : SignInAllowance

    /** Refused without checking the password; the same attempt succeeds after [retryAfter]. */
    data class Throttled(val retryAfter: Duration) : SignInAllowance
}

/**
 * Throttles password guessing, in memory, over a sliding [window].
 *
 * Failures are counted twice: against the username tried (exactly as typed) and against the client
 * that tried it (an address or other caller-chosen key). Once either key holds more than
 * [maxFailures] failures inside the window, attempts naming that key are [SignInAllowance.Throttled]
 * until enough of them age out. Call [check] before the password is verified, and record the outcome
 * after. A success clears the username's failures but not the client's, so one right guess does not
 * reset a client that is cycling through usernames.
 */
class SignInLimiter(
    private val clock: Clock = Clock.System,
    private val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val window: Duration = 1.minutes,
) {
    private val mutex = Mutex()
    private val byUsername = mutableMapOf<String, ArrayDeque<Instant>>()
    private val byClient = mutableMapOf<String, ArrayDeque<Instant>>()

    suspend fun check(username: String, client: String): SignInAllowance = mutex.withLock {
        val now = clock.now()
        val waits = listOfNotNull(
            byUsername.waitFor(username, now),
            byClient.waitFor(client, now),
        )
        waits.maxOrNull()?.let { SignInAllowance.Throttled(it) } ?: SignInAllowance.Allowed
    }

    suspend fun recordFailure(username: String, client: String) = mutex.withLock {
        val now = clock.now()
        // Sweep every key so usernames and clients that stop trying do not pile up.
        byUsername.sweep(now)
        byClient.sweep(now)
        byUsername.getOrPut(username, ::ArrayDeque).addLast(now)
        byClient.getOrPut(client, ::ArrayDeque).addLast(now)
    }

    suspend fun recordSuccess(username: String) {
        mutex.withLock { byUsername.remove(username) }
    }

    /** How long until [key] is back to [maxFailures] failures in the window; `null` if it already is. */
    private fun MutableMap<String, ArrayDeque<Instant>>.waitFor(key: String, now: Instant): Duration? {
        val failures = this[key] ?: return null
        failures.dropAgedOut(now)
        if (failures.isEmpty()) remove(key)
        if (failures.size <= maxFailures) return null
        // Once the failures before this one age out, exactly maxFailures remain.
        return failures[failures.size - maxFailures - 1] + window - now
    }

    private fun MutableMap<String, ArrayDeque<Instant>>.sweep(now: Instant) {
        values.forEach { it.dropAgedOut(now) }
        values.removeAll { it.isEmpty() }
    }

    private fun ArrayDeque<Instant>.dropAgedOut(now: Instant) {
        while (isNotEmpty() && first() + window <= now) removeFirst()
    }

    companion object {
        const val DEFAULT_MAX_FAILURES = 5
    }
}
