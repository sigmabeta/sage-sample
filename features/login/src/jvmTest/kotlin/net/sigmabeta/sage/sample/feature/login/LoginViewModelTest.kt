package net.sigmabeta.sage.sample.feature.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldChanged
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
