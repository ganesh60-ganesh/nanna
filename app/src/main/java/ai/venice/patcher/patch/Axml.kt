package ai.venice.patcher.patch

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal Android binary XML reader/writer. Parses to a tree and re-serialises with a fresh
 * string pool, which makes structural edits (element removal) possible without apktool.
 */
object Axml {

    const val NS_ANDROID = "http://schemas.android.com/apk/res/android"

    class Attr(val ns: String?, val name: String, val raw: String?, val type: Int, val data: Int)
    class Elem(val ns: String?, val name: String, val attrs: MutableList<Attr>) {
        val children = mutableListOf<Elem>()
    }
    class Text(val value: String)

    sealed class Node
    class ElemNode(val elem: Elem) : Node()
    class TextNode(val text: Text) : Node()

    fun parse(bytes: ByteArray): Elem {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buf.short.toInt() == 0x0003) { "not AXML" }
        buf.short
        buf.int

        var strings = emptyList<String>()
        val resIds = mutableListOf<Int>()
        val stack = ArrayDeque<Elem>()
        var root: Elem? = null
        val pendingText = ArrayList<Text>()

        while (buf.remaining() >= 8) {
            val type = buf.short.toInt()
            val headerSize = buf.short.toInt()
            val size = buf.int
            if (size < 8 || size > buf.remaining() + 8) break
            val end = buf.position() - 8 + size

            when (type) {
                0x0001 -> strings = readPool(buf, end)
                0x0180 -> while (buf.position() < end) resIds += buf.int
                0x0102 -> {
                    buf.int; buf.int
                    val nsIdx = buf.int
                    val nameIdx = buf.int
                    buf.short; buf.short
                    val count = buf.short.toInt()
                    buf.short; buf.short; buf.short
                    val attrs = mutableListOf<Attr>()
                    repeat(count) {
                        val ans = buf.int
                        val an = buf.int
                        val araw = buf.int
                        val vsize = buf.short.toInt()
                        buf.get()
                        val dtype = buf.get().toInt()
                        val data = buf.int
                        if (vsize > 8) buf.position(buf.position() + vsize - 8)
                        attrs += Attr(
                            if (ans >= 0) strings[ans] else null,
                            strings[an],
                            if (araw >= 0) strings[araw] else null,
                            dtype,
                            data,
                        )
                    }
                    val el = Elem(if (nsIdx >= 0) strings[nsIdx] else null, strings[nameIdx], attrs)
                    if (stack.isEmpty()) root = el else stack.last().children += el
                    stack.addLast(el)
                }
                0x0103 -> { buf.int; buf.int; buf.int; buf.int; if (stack.isNotEmpty()) stack.removeLast() }
                0x0104 -> {
                    buf.int; buf.int
                    val idx = buf.int
                    val v = Text(strings[idx])
                    if (stack.isNotEmpty()) pendingText += v
                    buf.int; buf.get(); buf.int
                }
            }
            buf.position(end)
        }
        return root ?: error("empty manifest")
    }

    private fun readPool(buf: ByteBuffer, end: Int): List<String> {
        val count = buf.int
        buf.int
        val flags = buf.int
        val start = buf.int
        buf.int
        val offsets = IntArray(count) { buf.int }
        val base = end - (buf.limit() - buf.position()) - 0
        val dataStart = buf.position() - 4 - 4 * count + start - 4
        val utf8 = (flags and 0x100) != 0
        val out = ArrayList<String>(count)
        for (off in offsets) {
            val p = dataStart + off
            out += if (utf8) readUtf8(buf, p) else readUtf16(buf, p)
        }
        return out
    }

    private fun readUtf8(buf: ByteBuffer, at: Int): String {
        var p = at
        var len = buf.get(p++).toInt() and 0xFF
        if (len and 0x80 != 0) { len = ((len and 0x7F) shl 8) or (buf.get(p++).toInt() and 0xFF) }
        val bytes = ByteArray(len) { buf.get(p + it) }
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16(buf: ByteBuffer, at: Int): String {
        var p = at
        var len = buf.getShort(p).toInt() and 0xFFFF
        p += 2
        if (len and 0x8000 != 0) { len = ((len and 0x7FFF) shl 16) or (buf.getShort(p).toInt() and 0xFFFF); p += 2 }
        val chars = CharArray(len) { (buf.getShort(p + it * 2).toInt() and 0xFFFF).toChar() }
        return String(chars)
    }

    fun write(root: Elem): ByteArray {
        val pool = LinkedHashSet<String>()
        pool += "android"
        pool += NS_ANDROID
        fun collect(e: Elem) {
            e.ns?.let { pool += it }
            pool += e.name
            e.attrs.forEach { a -> a.ns?.let { pool += it }; pool += a.name; a.raw?.let { pool += it } }
            e.children.forEach(::collect)
        }
        collect(root)
        val index = pool.withIndex().associate { (i, s) -> s to i }

        val body = ByteArrayOutputStream()

        fun chunk(type: Int, payload: ByteArray, headerExtra: Int = 8) {
            val size = 8 + headerExtra + payload.size
            val b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            b.putShort(type.toShort()); b.putShort((8 + headerExtra).toShort()); b.putInt(size)
            body.write(b.array())
            body.write(payload)
        }

        fun emit(e: Elem) {
            val attrs = ByteArrayOutputStream()
            for (a in e.attrs) {
                val ab = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
                ab.putInt(a.ns?.let { index[it] } ?: -1)
                ab.putInt(index[a.name] ?: -1)
                ab.putInt(a.raw?.let { index[it] } ?: -1)
                ab.putShort(8); ab.put(0)
                ab.put(a.type.toByte())
                ab.putInt(a.data)
                attrs.write(ab.array())
            }
            val payload = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(0); putInt(0)
                putInt(e.ns?.let { index[it] } ?: -1)
                putInt(index[e.name] ?: -1)
                putShort(20); putShort(20); putShort(e.attrs.size.toShort())
                putShort(0); putShort(0); putShort(0)
            }.array()
            chunk(0x0102, payload + attrs.toByteArray(), headerExtra = 8)

            e.children.forEach(::emit)

            val endPayload = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(0); putInt(0)
                putInt(e.ns?.let { index[it] } ?: -1)
                putInt(index[e.name] ?: -1)
            }.array()
            chunk(0x0103, endPayload, headerExtra = 8)
        }

        emit(root)

        val poolBytes = buildPool(pool.toList())
        val total = 8 + poolBytes.size + body.size()
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(0x0003); out.putShort(8); out.putInt(total)
        out.put(poolBytes)
        out.put(body.toByteArray())
        return out.array()
    }

    private fun buildPool(strings: List<String>): ByteArray {
        val data = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)
        for ((i, s) in strings.withIndex()) {
            offsets[i] = data.size()
            val bytes = s.toByteArray(Charsets.UTF_8)
            data.write(bytes.size)
            data.write(bytes)
            data.write(0)
        }
        while (data.size() % 4 != 0) data.write(0)

        val headerSize = 28
        val stringsStart = headerSize + 4 * strings.size
        val size = stringsStart + data.size()
        val buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0x0001); buf.putShort(headerSize.toShort()); buf.putInt(size)
        buf.putInt(strings.size); buf.putInt(0); buf.putInt(0x100)
        buf.putInt(stringsStart); buf.putInt(0)
        offsets.forEach { buf.putInt(it) }
        buf.put(data.toByteArray())
        return buf.array()
    }
}
