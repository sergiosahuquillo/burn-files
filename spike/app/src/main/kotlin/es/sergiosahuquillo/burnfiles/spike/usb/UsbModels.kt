package es.sergiosahuquillo.burnfiles.spike.usb

enum class ProbeKind { DIRECT, DOCUMENT_TREE }

enum class UsbOp { OK, FAILED, READ_ONLY, VOLUME_REMOVED, ACTIVITY_NOT_FOUND, NOT_RUN }

enum class ErrorKind { EACCES, EROFS, ENOENT, IO, SECURITY }

data class UsbProbeResult(
    val volumeIndex: Int,
    val probe: ProbeKind,
    val capacityBytes: Long,
    val freeBytes: Long,
    val detect: UsbOp = UsbOp.NOT_RUN,
    val write: UsbOp = UsbOp.NOT_RUN,
    val readVerify: UsbOp = UsbOp.NOT_RUN,
    val delete: UsbOp = UsbOp.NOT_RUN,
    val errorKind: ErrorKind? = null
)
