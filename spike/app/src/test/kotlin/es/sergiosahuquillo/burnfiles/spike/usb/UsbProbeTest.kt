package es.sergiosahuquillo.burnfiles.spike.usb

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class UsbProbeTest {
    @TempDir
    lateinit var volume: File

    private fun probe(io: ProbeIo = FileProbeIo) = DirectProbe(io = io, name = { "BurnFilesSpike-probe-test.bin" })

    @Test
    fun `full sequence on a writable volume leaves nothing behind`() {
        val result = probe().run(0, volume, 100, 50)
        assertEquals(UsbOp.OK, result.detect)
        assertEquals(UsbOp.OK, result.write)
        assertEquals(UsbOp.OK, result.readVerify)
        assertEquals(UsbOp.OK, result.delete)
        assertEquals(null, result.errorKind)
        assertTrue(volume.listFiles().isNullOrEmpty())
    }

    @Test
    fun `missing volume fails detection`() {
        val result = probe().run(1, File(volume, "gone"), 0, 0)
        assertEquals(UsbOp.FAILED, result.detect)
        assertEquals(UsbOp.NOT_RUN, result.write)
        assertEquals(ErrorKind.ENOENT, result.errorKind)
    }

    @Test
    fun `read-only file system is reported as READ_ONLY`() {
        val io = FakeIo(writeError = FileNotFoundException("/mnt/usb/x: open failed: EROFS (Read-only file system)"))
        val result = probe(io).run(0, volume, 0, 0)
        assertEquals(UsbOp.READ_ONLY, result.write)
        assertEquals(ErrorKind.EROFS, result.errorKind)
        assertEquals(UsbOp.NOT_RUN, result.readVerify)
    }

    @Test
    fun `permission denied is a failed write with EACCES`() {
        val io = FakeIo(writeError = FileNotFoundException("open failed: EACCES (Permission denied)"))
        val result = probe(io).run(0, volume, 0, 0)
        assertEquals(UsbOp.FAILED, result.write)
        assertEquals(ErrorKind.EACCES, result.errorKind)
    }

    @Test
    fun `volume removed in the middle of the write`() {
        val io = FakeIo(writeError = IOException("I/O error"), removeOnWrite = true)
        val result = probe(io).run(0, volume, 0, 0)
        assertEquals(UsbOp.OK, result.detect)
        assertEquals(UsbOp.VOLUME_REMOVED, result.write)
        assertEquals(ErrorKind.IO, result.errorKind)
    }

    @Test
    fun `corrupted read back fails verification but still deletes`() {
        val io = FakeIo(corruptRead = true)
        val result = probe(io).run(0, volume, 0, 0)
        assertEquals(UsbOp.FAILED, result.readVerify)
        assertEquals(UsbOp.OK, result.delete)
    }

    @Test
    fun `security exceptions are normalized`() {
        assertEquals(ErrorKind.SECURITY, UsbErrors.kind(SecurityException("nope")))
        assertEquals(ErrorKind.ENOENT, UsbErrors.kind(IOException("No such file or directory")))
        assertEquals(ErrorKind.IO, UsbErrors.kind(IOException()))
    }

    @Test
    fun `the probe only deletes the file it created`() {
        val keep = File(volume, "holiday.jpg").apply { writeText("keep") }
        probe().run(0, volume, 0, 0)
        assertTrue(keep.exists())
        assertFalse(File(volume, "BurnFilesSpike-probe-test.bin").exists())
    }

    private class FakeIo(
        val writeError: Exception? = null,
        val removeOnWrite: Boolean = false,
        val corruptRead: Boolean = false
    ) : ProbeIo {
        private var present = true
        private var stored: ByteArray? = null

        override fun exists(dir: File) = present

        override fun write(file: File, bytes: ByteArray) {
            if (removeOnWrite) present = false
            writeError?.let { throw it }
            stored = bytes
        }

        override fun read(file: File): ByteArray {
            val data = stored ?: throw FileNotFoundException("No such file")
            return if (corruptRead) data.copyOf().also { it[0] = (it[0] + 1).toByte() } else data
        }

        override fun delete(file: File): Boolean = (stored != null).also { stored = null }
    }
}
