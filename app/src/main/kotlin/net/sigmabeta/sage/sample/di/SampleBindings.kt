package net.sigmabeta.sage.sample.di

import android.content.Context
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.NoopAnalytics
import net.sigmabeta.sage.android.logging.AndroidHatchet
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.SageScheduler
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.R
import net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.ui.strings.AndroidStringProvider

/**
 * App-side implementations of SAGE's platform seams.
 *
 * `@BindingContainer` + `@ContributesTo(AppScope::class)` is the Metro replacement for Hilt's
 * `@Module` + `@InstallIn(SingletonComponent::class)`. Anything contributed here is visible to
 * [SampleAppGraph] without listing it in the graph.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object SampleBindings {
    /**
     * SAGE features read user-facing text through [StringProvider] and a module-owned
     * [SageStringId] enum, so the feature module stays platform-agnostic. The app is the only
     * place that knows about `R.string`.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStringProvider(context: Context): StringProvider =
        AndroidStringProvider(context.resources) { id ->
            when (id as WelcomeStringId) {
                WelcomeStringId.APP_NAME -> R.string.app_name
                WelcomeStringId.WELCOME_TITLE -> R.string.welcome_title
                WelcomeStringId.WELCOME_ABOUT_HEADER -> R.string.welcome_about_header
                WelcomeStringId.WELCOME_ABOUT_BODY_COMPOSITE_BUILD -> R.string.welcome_about_body_composite_build
                WelcomeStringId.WELCOME_ABOUT_BODY_LIST_STATE -> R.string.welcome_about_body_list_state
            }
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideHatchet(): Hatchet = AndroidHatchet()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(hatchet: Hatchet): Analytics = NoopAnalytics(hatchet)

    @Provides
    @SingleIn(AppScope::class)
    fun provideScheduler(impl: SchedulerImpl): SageScheduler = impl
}
