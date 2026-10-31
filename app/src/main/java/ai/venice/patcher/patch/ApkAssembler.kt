package ai.venice.patcher.patch

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkAssembler {

    private val DROP = Regex("^META-INF/(MANIFEST\\.MF|.*\\.(SF|RSA|DSA|EC))$", RegexOption.IGNORE_CASE)

    fun rebuild(src: File, dst: File, replacements: Map<String, File>) {
        ZipFile(src).use { zin ->
            val counter = Counter(BufferedOutputStream(FileOutputStream(dst)))
            val zout = ZipOutputStream(counter, Charsets.UTF_8)

            for (e in zin.entries().toList()) {
                if (e.isDirectory || DROP.matches(e.name)) continue
                val bytes = replacements[e.name]?.readBytes()
                    ?: zin.getInputStream(e).use { it.readBytes() }

                val store = e.method == ZipEntry.STORED ||
                    e.name == "resources.arsc" || e.name.endsWith(".so")
                val out = ZipEntry(e.name).apply { time = e.time }

                if (store) {
                    out.method = ZipEntry.STORED
                    out.size = bytes.size.toLong()
                    out.compressedSize = bytes.size.toLong()
                    out.crc = CRC32().apply { update(bytes) }.value
                    val nameLen = e.name.toByteArray(Charsets.UTF_8).size
                    val pad = (4 - ((counter.count + 30 + nameLen + 4) % 4).toInt() % 4) % 4
                    out.extra = ByteArray(4 + pad).also {
                        it[0] = 0x35; it[1] = 0xD9.toByte(); it[2] = pad.toByte(); it[3] = 0
                    }
                } else out.method = ZipEntry.DEFLATED

                zout.putNextEntry(out)
                zout.write(bytes)
                zout.closeEntry()
            }
            zout.finish()
            zout.close()
        }
    }

    private class Counter(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
    }
}
