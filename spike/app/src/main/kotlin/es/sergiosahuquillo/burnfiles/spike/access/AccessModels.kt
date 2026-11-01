package es.sergiosahuquillo.burnfiles.spike.access

enum class AccessLevel {
    FULL,
    LEGACY_FULL,
    MEDIA_ONLY,
    NONE
    ;

    val isFull: Boolean get() = this == FULL || this == LEGACY_FULL
}

/** Steps of the fallback chain, in the order of constitution principle IV (research R4). */
enum class AccessStep { APP_SETTINGS, GENERIC_SETTINGS, ADB, RUNTIME_DIALOG, MEDIA_ONLY }

enum class AttemptOutcome { OPENED, ACTIVITY_NOT_FOUND, SECURITY_EXCEPTION, GRANTED, DENIED, NO_EFFECT, SKIPPED }

data class AccessAttempt(
    val step: AccessStep,
    val resolvable: Boolean?,
    val outcome: AttemptOutcome,
    val levelAfter: AccessLevel
)

data class MediaCoverage(val images: Int, val video: Int, val audio: Int, val downloads: Int)
