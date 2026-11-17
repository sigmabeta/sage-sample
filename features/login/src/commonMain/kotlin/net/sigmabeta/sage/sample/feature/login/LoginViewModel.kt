package net.sigmabeta.sage.sample.feature.login

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.ui.components.models.TextFieldChanged
import net.sigmabeta.sage.sample.ui.components.models.TextFieldDone
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/** Login's view model: the Sign in form over [IdentityRepository]. */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class LoginViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val repository: IdentityRepository,
) : SampleListViewModel<LoginState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    /** The in-flight Sign in attempt, if any. */
    private var submitJob: Job? = null

    override fun initialState(): LoginState = LoginState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is TextFieldChanged -> onFieldChanged(action.id, action.text)
            is TextFieldDone -> if (action.id == PASSWORD_FIELD_ID) submit()
            LoginAction.SubmitClicked -> submit()
            else -> Unit
        }
    }

    /**
     * Starts one Sign in attempt with the form's text, unless the form is incomplete or an attempt
     * is already in flight. [submitJob] is the in-flight guard: it is set synchronously, so a
     * double tap cannot start a second attempt before the state shows Submitting.
     */
    private fun submit() {
        val form = internalUiState.value
        if (submitJob?.isActive == true || !form.canSubmit) {
            return
        }

        updateState { it.copy(status = LoginStatus.SUBMITTING) }

        submitJob = flow { emit(repository.signIn(form.username, form.password)) }
            .runInBackground(scheduler.dispatchers.network)
    }

    private fun onFieldChanged(id: Long, text: String) {
        updateState { state ->
            when (id) {
                USERNAME_FIELD_ID -> state.copy(username = text)
                PASSWORD_FIELD_ID -> state.copy(password = text)
                else -> state
            }
        }
    }
}
