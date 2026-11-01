package es.sergiosahuquillo.burnfiles.spike.device

/** Device identifiers of the verification matrix (spec, *Contexto*). */
enum class MatrixId { D1, D2, D3, D4, D5, OTHER }

enum class UiMode { MOBILE, TV }

data class DeviceInfo(
    val matrixId: MatrixId,
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val release: String,
    val fireOs: String?,
    val uiMode: UiMode
)
