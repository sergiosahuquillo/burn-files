package es.sergiosahuquillo.burnfiles.spike.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.sergiosahuquillo.burnfiles.spike.R
import es.sergiosahuquillo.burnfiles.spike.ui.common.DialogTags
import es.sergiosahuquillo.burnfiles.spike.ui.common.MobileConfirmDialog
import es.sergiosahuquillo.burnfiles.spike.ui.common.TvConfirmDialog
import es.sergiosahuquillo.burnfiles.spike.ui.common.destructiveBody
import es.sergiosahuquillo.burnfiles.spike.ui.theme.MobileTheme
import es.sergiosahuquillo.burnfiles.spike.ui.theme.TvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Destructive flow of the spike (constitution, *Flujo de desarrollo y calidad*): the cleanup dialog
 * shows count and size, "Cancel" deletes nothing, and on TV it has the initial focus.
 */
@RunWith(AndroidJUnit4::class)
class CleanupDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var confirmed = 0
    private var dismissed = 0

    @Composable
    private fun Dialog(tv: Boolean) {
        val title = stringResource(R.string.benchmark_clean_title)
        val body = destructiveBody(count = FILES, bytes = BYTES)
        val confirm = stringResource(R.string.action_delete)
        if (tv) {
            TvTheme { TvConfirmDialog(title, body, confirm, { confirmed++ }, { dismissed++ }) }
        } else {
            MobileTheme { MobileConfirmDialog(title, body, confirm, { confirmed++ }, { dismissed++ }) }
        }
    }

    private fun expectedBody(): String {
        val count = java.text.NumberFormat.getIntegerInstance(context.resources.configuration.locales[0]).format(FILES)
        val size = android.text.format.Formatter.formatShortFileSize(context, BYTES)
        return context.resources.getQuantityString(R.plurals.confirm_delete_body, FILES.toInt(), count, size)
    }

    @Test
    fun mobile_showsCountAndSize_andCancelDeletesNothing() {
        compose.setContent { Dialog(tv = false) }
        compose.onNodeWithText(expectedBody()).assertExists()
        compose.onNodeWithTag(DialogTags.CANCEL).performClick()
        assertEquals(1, dismissed)
        assertEquals(0, confirmed)
    }

    @Test
    fun mobile_deleteNeedsTheExplicitButton() {
        compose.setContent { Dialog(tv = false) }
        compose.onNodeWithTag(DialogTags.CONFIRM).performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun tv_cancelHasInitialFocus_andCentreButtonCancels() {
        compose.setContent { Dialog(tv = true) }
        compose.onNodeWithText(expectedBody()).assertExists()
        compose.onNodeWithTag(DialogTags.CANCEL).assertIsFocused()
        compose.onNodeWithTag(DialogTags.CANCEL).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(1, dismissed)
        assertEquals(0, confirmed)
    }

    private companion object {
        const val FILES = 100_000L
        const val BYTES = 209_715_200L
    }
}
