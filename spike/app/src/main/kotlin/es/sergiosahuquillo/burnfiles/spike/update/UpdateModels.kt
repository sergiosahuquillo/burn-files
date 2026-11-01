package es.sergiosahuquillo.burnfiles.spike.update

enum class UpdateStep { QUERY, DOWNLOAD, VERIFY_SHA256, VERIFY_CERT, INSTALL_PERMISSION, INSTALL }

enum class UpdateOutcome { OK, MISMATCH, NETWORK_ERROR, RATE_LIMITED, ACTIVITY_NOT_FOUND, USER_CANCELLED, FAILED }

data class UpdateRecord(
    val fromVersionCode: Int,
    val toVersionCode: Int,
    val step: UpdateStep,
    val outcome: UpdateOutcome,
    val downloadBytes: Long = 0,
    val durationMs: Long = 0,
    val installerStatus: Int? = null
)
