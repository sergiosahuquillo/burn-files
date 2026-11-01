package es.sergiosahuquillo.burnfiles.spike.access

/**
 * Pure state machine of the storage access fallback chain (data-model.md, *AccessAttempt*).
 *
 * API ≥ 30: APP_SETTINGS → GENERIC_SETTINGS → ADB → MEDIA_ONLY.
 * API < 30: RUNTIME_DIALOG → MEDIA_ONLY.
 *
 * Every method returns the step the developer is now on, or `null` once [finalLevel] is set.
 */
class AccessChain(private val modernPermissionModel: Boolean) {
    private val recorded = mutableListOf<AccessAttempt>()
    val attempts: List<AccessAttempt> get() = recorded.toList()

    var step: AccessStep? = null
        private set
    var finalLevel: AccessLevel? = null
        private set

    private var awaitingReturn = false
    private var awaitingResolvable: Boolean? = null

    val isFinished: Boolean get() = finalLevel != null

    fun start(level: AccessLevel): AccessStep? {
        recorded.clear()
        finalLevel = null
        awaitingReturn = false
        awaitingResolvable = null
        step = if (modernPermissionModel) AccessStep.APP_SETTINGS else AccessStep.RUNTIME_DIALOG
        if (level.isFull) finish(level)
        return if (isFinished) null else step
    }

    /** A settings screen was opened; the outcome is known when the app comes back ([onResumed]). */
    fun onLaunched(resolvable: Boolean?) {
        check(step == AccessStep.APP_SETTINGS || step == AccessStep.GENERIC_SETTINGS) { "Not a settings step" }
        awaitingReturn = true
        awaitingResolvable = resolvable
    }

    /**
     * The settings screen could not be opened
     * ([AttemptOutcome.ACTIVITY_NOT_FOUND] or [AttemptOutcome.SECURITY_EXCEPTION]).
     */
    fun onLaunchFailed(resolvable: Boolean?, outcome: AttemptOutcome, level: AccessLevel): AccessStep? {
        require(outcome == AttemptOutcome.ACTIVITY_NOT_FOUND || outcome == AttemptOutcome.SECURITY_EXCEPTION)
        val current = step ?: return null
        record(current, resolvable, outcome, level)
        return advance(level)
    }

    /** The app came back to the foreground and the level was re-evaluated. */
    fun onResumed(level: AccessLevel): AccessStep? {
        val current = step ?: return null
        if (isFinished) return null
        when {
            awaitingReturn && level.isFull -> {
                record(current, awaitingResolvable, AttemptOutcome.GRANTED, level)
                finish(level)
            }

            awaitingReturn -> {
                record(current, awaitingResolvable, AttemptOutcome.NO_EFFECT, level)
                advance(level)
            }

            current == AccessStep.ADB && level.isFull -> {
                record(current, null, AttemptOutcome.GRANTED, level)
                finish(level)
            }
        }
        return if (isFinished) null else step
    }

    /** Result of the runtime permission dialog for [AccessStep.RUNTIME_DIALOG] or [AccessStep.MEDIA_ONLY]. */
    fun onRuntimeResult(granted: Boolean, level: AccessLevel): AccessStep? {
        val current = step ?: return null
        if (isFinished) return null
        if (current != AccessStep.RUNTIME_DIALOG && current != AccessStep.MEDIA_ONLY) return current
        record(current, null, if (granted) AttemptOutcome.GRANTED else AttemptOutcome.DENIED, level)
        return when {
            current == AccessStep.MEDIA_ONLY || level.isFull -> finish(level).let { null }
            else -> advance(level)
        }
    }

    /** The developer gave up on ADB and continues in limited mode. */
    fun skipAdb(level: AccessLevel): AccessStep? {
        if (step != AccessStep.ADB || isFinished) return step
        record(AccessStep.ADB, null, AttemptOutcome.SKIPPED, level)
        return advance(level)
    }

    private fun advance(level: AccessLevel): AccessStep? {
        awaitingReturn = false
        awaitingResolvable = null
        val next =
            when (step) {
                AccessStep.APP_SETTINGS -> AccessStep.GENERIC_SETTINGS
                AccessStep.GENERIC_SETTINGS -> AccessStep.ADB
                AccessStep.ADB, AccessStep.RUNTIME_DIALOG -> AccessStep.MEDIA_ONLY
                AccessStep.MEDIA_ONLY, null -> null
            }
        if (next == null) {
            finish(level)
        } else {
            step = next
        }
        return next
    }

    private fun finish(level: AccessLevel) {
        awaitingReturn = false
        finalLevel = level
    }

    private fun record(step: AccessStep, resolvable: Boolean?, outcome: AttemptOutcome, level: AccessLevel) {
        recorded += AccessAttempt(step, resolvable, outcome, level)
    }
}
