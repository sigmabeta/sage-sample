package net.sigmabeta.sage.sample.feature.login

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/** Renders every id as its name so tests can identify rows without the real text table. */
internal object FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = string.toString()
    override fun getStringOneArg(string: SageStringId, arg: String): String = string.toString()
    override fun getStringOneInt(string: SageStringId, arg: Int): String = string.toString()
    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        string.toString()
}

internal object FakeAnalytics : Analytics {
    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit
    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) = Unit
    override fun logEvent(event: SageEvent) = Unit
    override fun logAutoRefresh() = Unit
    override fun logError(failedOperationName: String, errorString: String, error: Throwable) = Unit
}

/**
 * A scripted [IdentityRepository]: every [signIn] records its credentials and answers [outcome]
 * (or throws [failure]). [gate], when set, holds each attempt open until the test completes it, so
 * a test can observe the in-flight state.
 */
internal class FakeIdentityRepository(
    var outcome: SignInOutcome = SignInOutcome.Rejected,
    var failure: Throwable? = null,
    var gate: CompletableDeferred<Unit>? = null,
) : IdentityRepository {
    private val internalState = MutableStateFlow<IdentityState>(IdentityState.SignedOut)

    val signInCalls: MutableList<Pair<String, String>> = mutableListOf()

    override val state: StateFlow<IdentityState> = internalState.asStateFlow()

    override suspend fun signIn(username: String, password: String): SignInOutcome {
        signInCalls += username to password
        gate?.await()
        failure?.let { throw it }
        val result = outcome
        if (result is SignInOutcome.Accepted) {
            internalState.value = IdentityState.SignedIn(result.user)
        }
        return result
    }

    override suspend fun signOut() {
        internalState.value = IdentityState.SignedOut
    }
}
