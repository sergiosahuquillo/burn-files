package es.sergiosahuquillo.burnfiles.spike.access

import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel.FULL
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel.LEGACY_FULL
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel.MEDIA_ONLY
import es.sergiosahuquillo.burnfiles.spike.access.AccessLevel.NONE
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep.ADB
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep.APP_SETTINGS
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep.GENERIC_SETTINGS
import es.sergiosahuquillo.burnfiles.spike.access.AccessStep.RUNTIME_DIALOG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AccessChainTest {
    @Nested
    inner class ModernModel {
        private val chain = AccessChain(modernPermissionModel = true)

        @Test
        fun `starts at the app-specific settings screen`() {
            assertEquals(APP_SETTINGS, chain.start(NONE))
        }

        @Test
        fun `already full access finishes immediately without attempts`() {
            assertNull(chain.start(FULL))
            assertEquals(FULL, chain.finalLevel)
            assertTrue(chain.attempts.isEmpty())
        }

        @Test
        fun `granted in settings is detected on return`() {
            chain.start(NONE)
            chain.onLaunched(resolvable = true)
            assertNull(chain.onResumed(FULL))
            assertEquals(FULL, chain.finalLevel)
            assertEquals(listOf(AccessAttempt(APP_SETTINGS, true, AttemptOutcome.GRANTED, FULL)), chain.attempts)
        }

        @Test
        fun `missing screens fall through to ADB in order`() {
            chain.start(NONE)
            assertEquals(GENERIC_SETTINGS, chain.onLaunchFailed(false, AttemptOutcome.ACTIVITY_NOT_FOUND, NONE))
            assertEquals(ADB, chain.onLaunchFailed(true, AttemptOutcome.SECURITY_EXCEPTION, NONE))
            assertEquals(
                listOf(AttemptOutcome.ACTIVITY_NOT_FOUND, AttemptOutcome.SECURITY_EXCEPTION),
                chain.attempts.map { it.outcome }
            )
        }

        @Test
        fun `a screen that opens but changes nothing is NO_EFFECT and the chain moves on`() {
            chain.start(NONE)
            chain.onLaunched(resolvable = true)
            assertEquals(GENERIC_SETTINGS, chain.onResumed(NONE))
            assertEquals(AttemptOutcome.NO_EFFECT, chain.attempts.single().outcome)
        }

        @Test
        fun `resuming without having launched anything records nothing`() {
            chain.start(NONE)
            assertEquals(APP_SETTINGS, chain.onResumed(NONE))
            assertTrue(chain.attempts.isEmpty())
        }

        @Test
        fun `ADB grant is detected when the app comes back`() {
            toAdb()
            assertEquals(ADB, chain.onResumed(NONE))
            assertNull(chain.onResumed(FULL))
            assertEquals(AccessAttempt(ADB, null, AttemptOutcome.GRANTED, FULL), chain.attempts.last())
        }

        @Test
        fun `skipping ADB ends in limited mode with the media dialog`() {
            toAdb()
            assertEquals(AccessStep.MEDIA_ONLY, chain.skipAdb(NONE))
            assertNull(chain.onRuntimeResult(granted = true, level = MEDIA_ONLY))
            assertEquals(MEDIA_ONLY, chain.finalLevel)
            assertEquals(
                listOf(APP_SETTINGS, GENERIC_SETTINGS, ADB, AccessStep.MEDIA_ONLY),
                chain.attempts.map { it.step }
            )
        }

        @Test
        fun `denying media access finishes with no access`() {
            toAdb()
            chain.skipAdb(NONE)
            chain.onRuntimeResult(granted = false, level = NONE)
            assertEquals(NONE, chain.finalLevel)
            assertEquals(AttemptOutcome.DENIED, chain.attempts.last().outcome)
        }

        @Test
        fun `restarting clears previous attempts`() {
            toAdb()
            chain.start(NONE)
            assertTrue(chain.attempts.isEmpty())
            assertNull(chain.finalLevel)
        }

        private fun toAdb() {
            chain.start(NONE)
            chain.onLaunchFailed(null, AttemptOutcome.ACTIVITY_NOT_FOUND, NONE)
            chain.onLaunchFailed(null, AttemptOutcome.ACTIVITY_NOT_FOUND, NONE)
        }
    }

    @Nested
    inner class LegacyModel {
        private val chain = AccessChain(modernPermissionModel = false)

        @Test
        fun `starts with the runtime dialog`() {
            assertEquals(RUNTIME_DIALOG, chain.start(NONE))
        }

        @Test
        fun `granted read and write finishes with legacy full access`() {
            chain.start(NONE)
            assertNull(chain.onRuntimeResult(granted = true, level = LEGACY_FULL))
            assertEquals(LEGACY_FULL, chain.finalLevel)
        }

        @Test
        fun `denied dialog goes to media only`() {
            chain.start(NONE)
            assertEquals(AccessStep.MEDIA_ONLY, chain.onRuntimeResult(granted = false, level = NONE))
            assertEquals(AttemptOutcome.DENIED, chain.attempts.single().outcome)
        }

        @Test
        fun `resume events do not interfere with the dialog`() {
            chain.start(NONE)
            assertEquals(RUNTIME_DIALOG, chain.onResumed(NONE))
            assertTrue(chain.attempts.isEmpty())
        }
    }
}
