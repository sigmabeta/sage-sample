package net.sigmabeta.sage.sample.repository.demo

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.database.SampleDatabase
import net.sigmabeta.sage.sample.identity.RoomSessionStore
import net.sigmabeta.sage.sample.identity.RoomUserDirectory
import net.sigmabeta.sage.sample.identity.SessionStore
import net.sigmabeta.sage.sample.identity.SignInLimiter
import net.sigmabeta.sage.sample.identity.SignInService
import net.sigmabeta.sage.sample.identity.UserDirectory

/**
 * `:core:identity` over the app's [SampleDatabase], for [DemoIdentityRepository].
 *
 * The app provides only the database, because only the app knows where its file lives (Android
 * needs a `Context`; the desktop app keeps it beside its settings file). This module never builds
 * one: it is a JVM library that the Android app also ships, and `:core:database`'s JVM builders do
 * not exist in the Android variant it runs against.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object DemoIdentityBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideUserDirectory(database: SampleDatabase): UserDirectory = RoomUserDirectory(database)

    @Provides
    @SingleIn(AppScope::class)
    fun provideSessionStore(database: SampleDatabase): SessionStore = RoomSessionStore(database)

    /** One limiter per install, in memory, like the server's. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideSignInService(users: UserDirectory, sessions: SessionStore): SignInService =
        SignInService(users, sessions, SignInLimiter())
}
