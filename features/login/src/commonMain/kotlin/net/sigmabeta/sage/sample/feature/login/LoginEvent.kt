package net.sigmabeta.sage.sample.feature.login

import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.sample.repository.User

/**
 * Login's one-shot effects. They report what happened and never navigate: the Sign-in gate reacts
 * to `IdentityRepository.state`, so a host may ignore these.
 */
sealed class LoginEvent : SageEvent() {
    /** A Sign in attempt was accepted; the client is now Signed in as [user]. */
    data class SignedIn(val user: User) : LoginEvent() {
        override val source: String = "LoginViewModel"
    }
}
