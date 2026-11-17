package net.sigmabeta.sage.sample.feature.login

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldChanged
import net.sigmabeta.sage.sample.ui.components.models.TextFieldDone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val DEMO_USER = User(id = "user-1", username = "demo")

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on an empty form with Sign in disabled and the Demo account hint`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        val actual = viewModel.uiStateActual.value
        val items = actual.listItems

        assertEquals("LOGIN_TITLE", actual.title.title)
        assertFalse(actual.title.shouldShowBack)

        assertEquals(
            TextFieldListModel(
                id = USERNAME_FIELD_ID,
                label = "LOGIN_USERNAME_LABEL",
                text = "",
            ),
            username(items),
        )
        assertEquals(
            TextFieldListModel(
                id = PASSWORD_FIELD_ID,
                label = "LOGIN_PASSWORD_LABEL",
                text = "",
                masked = true,
                submitOnDone = true,
            ),
            password(items),
        )
        assertEquals("LOGIN_SUBMIT", button(items).label)
        assertFalse(button(items).enabled)
        assertEquals(listOf("LOGIN_DEMO_ACCOUNT_HINT" to FormMessageTone.INFO), messages(items))
        // Fields, then the button, then the hint under the form.
        assertEquals(
            listOf(TextFieldListModel::class, TextFieldListModel::class, ButtonListModel::class, FormMessageListModel::class),
            items.map { it::class },
        )
    }

    @Test
    fun `typing into both fields fills the form and enables Sign in`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.sendAction(TextFieldChanged(USERNAME_FIELD_ID, "demo"))
        assertFalse("a username alone is not enough", button(items(viewModel)).enabled)

        viewModel.sendAction(TextFieldChanged(PASSWORD_FIELD_ID, "sage"))

        val items = items(viewModel)
        assertEquals("demo", username(items).text)
        assertEquals("sage", password(items).text)
        assertTrue(button(items).enabled)
    }

    @Test
    fun `a password alone does not enable Sign in`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.sendAction(TextFieldChanged(PASSWORD_FIELD_ID, "sage"))

        assertFalse(button(items(viewModel)).enabled)
    }

    @Test
    fun `emptying a filled field disables Sign in again`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.fill()

        viewModel.sendAction(TextFieldChanged(USERNAME_FIELD_ID, ""))

        assertFalse(button(items(viewModel)).enabled)
    }

    @Test
    fun `Sign in sends the exact credentials and locks the form while the attempt is in flight`() =
        runTest(dispatcher) {
            val repository = FakeIdentityRepository(gate = CompletableDeferred())
            val viewModel = createViewModel(repository)
            viewModel.fill(username = " demo", password = "sage ")

            viewModel.sendAction(LoginAction.SubmitClicked)

            assertEquals(listOf(" demo" to "sage "), repository.signInCalls)
            val items = items(viewModel)
            assertFalse(username(items).enabled)
            assertFalse(password(items).enabled)
            assertEquals("LOGIN_SUBMITTING", button(items).label)
            assertFalse(button(items).enabled)
            assertEquals(listOf("LOGIN_DEMO_ACCOUNT_HINT" to FormMessageTone.INFO), messages(items))
        }

    @Test
    fun `a second Sign in while one is in flight is ignored`() = runTest(dispatcher) {
        val repository = FakeIdentityRepository(gate = CompletableDeferred())
        val viewModel = createViewModel(repository)
        viewModel.fill()

        viewModel.sendAction(LoginAction.SubmitClicked)
        viewModel.sendAction(LoginAction.SubmitClicked)
        viewModel.sendAction(TextFieldDone(PASSWORD_FIELD_ID))

        assertEquals(1, repository.signInCalls.size)
    }

    @Test
    fun `Sign in on an incomplete form does nothing`() = runTest(dispatcher) {
        val repository = FakeIdentityRepository()
        val viewModel = createViewModel(repository)
        viewModel.sendAction(TextFieldChanged(USERNAME_FIELD_ID, "demo"))

        viewModel.sendAction(LoginAction.SubmitClicked)
        viewModel.sendAction(TextFieldDone(PASSWORD_FIELD_ID))

        assertEquals(emptyList<Pair<String, String>>(), repository.signInCalls)
        assertEquals("LOGIN_SUBMIT", button(items(viewModel)).label)
    }

    @Test
    fun `Done on the password field signs in`() = runTest(dispatcher) {
        val repository = FakeIdentityRepository(gate = CompletableDeferred())
        val viewModel = createViewModel(repository)
        viewModel.fill()

        viewModel.sendAction(TextFieldDone(PASSWORD_FIELD_ID))

        assertEquals(listOf("demo" to "sage"), repository.signInCalls)
    }

    @Test
    fun `Done on the username field does not sign in`() = runTest(dispatcher) {
        val repository = FakeIdentityRepository()
        val viewModel = createViewModel(repository)
        viewModel.fill()

        viewModel.sendAction(TextFieldDone(USERNAME_FIELD_ID))

        assertEquals(emptyList<Pair<String, String>>(), repository.signInCalls)
    }

    @Test
    fun `each refused outcome shows its message above Sign in and unlocks the form`() = runTest(dispatcher) {
        val table = listOf(
            SignInOutcome.Rejected to "LOGIN_REJECTED",
            SignInOutcome.Throttled(retryAfter = 30.seconds) to "LOGIN_THROTTLED",
            SignInOutcome.Throttled(retryAfter = Duration.ZERO) to "LOGIN_THROTTLED",
            SignInOutcome.Unreachable to "LOGIN_UNREACHABLE",
        )

        table.forEach { (outcome, message) ->
            val viewModel = createViewModel(FakeIdentityRepository(outcome = outcome))
            viewModel.fill()

            viewModel.sendAction(LoginAction.SubmitClicked)

            val items = items(viewModel)
            assertEquals(
                "$outcome",
                listOf(message to FormMessageTone.ERROR, "LOGIN_DEMO_ACCOUNT_HINT" to FormMessageTone.INFO),
                messages(items),
            )
            assertEquals(
                "$outcome: the failure sits between the fields and the button",
                listOf(
                    TextFieldListModel::class,
                    TextFieldListModel::class,
                    FormMessageListModel::class,
                    ButtonListModel::class,
                    FormMessageListModel::class,
                ),
                items.map { it::class },
            )
            assertTrue("$outcome", username(items).enabled)
            assertTrue("$outcome", password(items).enabled)
            assertEquals("$outcome: typed text is kept", "demo" to "sage", username(items).text to password(items).text)
            assertEquals("$outcome", "LOGIN_SUBMIT", button(items).label)
            assertTrue("$outcome", button(items).enabled)
        }
    }

    @Test
    fun `a failing identity source shows unreachable`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeIdentityRepository(failure = IllegalStateException("boom")))
        viewModel.fill()

        viewModel.sendAction(LoginAction.SubmitClicked)

        assertEquals("LOGIN_UNREACHABLE" to FormMessageTone.ERROR, messages(items(viewModel)).first())
        assertTrue(button(items(viewModel)).enabled)
    }

    @Test
    fun `editing either field after a failure clears the message`() = runTest(dispatcher) {
        listOf(USERNAME_FIELD_ID, PASSWORD_FIELD_ID).forEach { fieldId ->
            val viewModel = createViewModel(FakeIdentityRepository(outcome = SignInOutcome.Rejected))
            viewModel.fill()
            viewModel.sendAction(LoginAction.SubmitClicked)

            viewModel.sendAction(TextFieldChanged(fieldId, "changed"))

            assertEquals(
                "field $fieldId",
                listOf("LOGIN_DEMO_ACCOUNT_HINT" to FormMessageTone.INFO),
                messages(items(viewModel)),
            )
        }
    }

    @Test
    fun `a refused attempt can be retried`() = runTest(dispatcher) {
        val repository = FakeIdentityRepository(outcome = SignInOutcome.Rejected)
        val viewModel = createViewModel(repository)
        viewModel.fill(password = "wrong")
        viewModel.sendAction(LoginAction.SubmitClicked)

        repository.outcome = SignInOutcome.Accepted(DEMO_USER)
        viewModel.sendAction(TextFieldChanged(PASSWORD_FIELD_ID, "sage"))
        viewModel.sendAction(LoginAction.SubmitClicked)

        assertEquals(listOf("demo" to "wrong", "demo" to "sage"), repository.signInCalls)
    }

    @Test
    fun `an accepted attempt emits SignedIn, never navigates, and clears the form`() = runTest(dispatcher) {
        val repository = FakeIdentityRepository(outcome = SignInOutcome.Accepted(DEMO_USER))
        val viewModel = createViewModel(repository)
        val events = mutableListOf<SageEvent>()
        val collector = launch { viewModel.uiEvents.collect { events += it } }
        viewModel.fill()
        val keyBefore = username(items(viewModel)).resetKey

        viewModel.sendAction(LoginAction.SubmitClicked)

        assertEquals(listOf<SageEvent>(LoginEvent.SignedIn(DEMO_USER)), events)
        assertEquals(IdentityState.SignedIn(DEMO_USER), repository.state.value)

        val items = items(viewModel)
        assertEquals("" to "", username(items).text to password(items).text)
        assertTrue("both fields drop what was typed", username(items).resetKey != keyBefore)
        assertEquals(username(items).resetKey, password(items).resetKey)
        assertEquals(listOf("LOGIN_DEMO_ACCOUNT_HINT" to FormMessageTone.INFO), messages(items))
        assertEquals("LOGIN_SUBMIT", button(items).label)
        assertFalse(button(items).enabled)
        collector.cancel()
    }

    private fun LoginViewModel.fill(username: String = "demo", password: String = "sage") {
        sendAction(TextFieldChanged(USERNAME_FIELD_ID, username))
        sendAction(TextFieldChanged(PASSWORD_FIELD_ID, password))
    }

    private fun items(viewModel: LoginViewModel): List<ListModel> = viewModel.uiStateActual.value.listItems

    private fun createViewModel(
        repository: FakeIdentityRepository = FakeIdentityRepository(),
    ) = LoginViewModel(
        stringProvider = FakeStringProvider,
        analytics = FakeAnalytics,
        dispatchers = SageDispatchers(
            computation = dispatcher,
            disk = dispatcher,
            network = dispatcher,
            main = dispatcher,
        ),
        delayManager = object : SampleDelayManager {
            override fun delayMillis() = 0L
        },
        repository = repository,
    )

    private fun username(items: List<ListModel>): TextFieldListModel =
        items.filterIsInstance<TextFieldListModel>().single { it.id == USERNAME_FIELD_ID }

    private fun password(items: List<ListModel>): TextFieldListModel =
        items.filterIsInstance<TextFieldListModel>().single { it.id == PASSWORD_FIELD_ID }

    private fun button(items: List<ListModel>): ButtonListModel =
        items.filterIsInstance<ButtonListModel>().single()

    /** Every message row as (text, tone), in order. */
    private fun messages(items: List<ListModel>): List<Pair<String, FormMessageTone>> =
        items.filterIsInstance<FormMessageListModel>().map { it.text to it.tone }
}
