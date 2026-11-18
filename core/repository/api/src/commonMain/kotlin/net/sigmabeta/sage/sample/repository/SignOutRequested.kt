package net.sigmabeta.sage.sample.repository

import net.sigmabeta.sage.appcomm.SageEvent

/**
 * A feature asks the app to end this client's Session deliberately (Settings' Sign out).
 *
 * A feature emits this rather than calling [IdentityRepository.signOut] itself: the app's Sign-in
 * gate has to know the sign-out was deliberate, so the next Sign in opens Home instead of the
 * screen the user signed out from. It lives beside the identity seam because both the features and
 * the app's navigation policy (`:core:nav`) already depend on this module.
 */
data class SignOutRequested(override val source: String) : SageEvent()
