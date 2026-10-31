package ai.venice.patcher.patch

import java.util.zip.ZipFile

/** Structural edits to AndroidManifest.xml. Degrades to a no-op if the AXML round-trip fails. */
object ManifestPatcher {

    private val AD_NODES = listOf(
        "com.google.android.gms.ads.APPLICATION_ID",
        "com.google.android.gms.ads.AdActivity",
        "com.google.android.gms.ads.MobileAdsInitProvider",
        "com.google.android.gms.ads.flag.OPTIMIZATION_HACK",
        "com.google.android.gms.ads.DELAY_APP_MEASUREMENT_INIT",
        "com.google.android.gms.ads.APPLICATION_ID_ADJUSTED",
    )

    fun removePermissions(apk: java.io.File, names: List<String>): Boolean = runCatching {
        val raw = readManifest(apk) ?: return false
        val root = Axml.parse(raw)
        var changed = false
        val wanted = names.toSet()
        removeIf(root) { e ->
            e.name == "uses-permission" || e.name == "uses-permission-sdk-23"
        } andAlso { e ->
            e.attrs.any { it.name == "name" && it.raw in wanted }
        }.let { changed = it }
        if (!changed) return false
        writeManifest(apk, Axml.write(root))
        true
    }.getOrDefault(false)

    fun stripAdNodes(apk: java.io.File): Boolean = runCatching {
        val raw = readManifest(apk) ?: return false
        val root = Axml.parse(raw)
        val hit = removeIf(root) { e -> true } andAlso { e ->
            e.attrs.any { it.name == "name" && it.raw in AD_NODES }
        }
        if (!hit) return false
        writeManifest(apk, Axml.write(root))
        true
    }.getOrDefault(false)

    private infix fun ((Axml.Elem) -> Boolean).andAlso(pred: (Axml.Elem) -> Boolean): (Axml.Elem) -> Boolean =
        { e -> this(e) && pred(e) }

    private fun removeIf(parent: Axml.Elem, tag: (Axml.Elem) -> Boolean, match: (Axml.Elem) -> Boolean): Boolean {
        var changed = false
        val it = parent.children.iterator()
        while (it.hasNext()) {
            val child = it.next()
            if (tag(child) && match(child)) { it.remove(); changed = true }
            else if (removeIf(child, tag, match)) changed = true
        }
        return changed
    }

    private fun readManifest(apk: java.io.File): ByteArray? = ZipFile(apk).use { zip ->
        val e = zip.getEntry("AndroidManifest.xml") ?: return null
        zip.getInputStream(e).use { it.readBytes() }
    }

    private fun writeManifest(apk: java.io.File, bytes: ByteArray) {
        val tmp = java.io.File(apk.parentFile, "manifest.bin")
        tmp.writeBytes(bytes)
        val stage = java.io.File(apk.parentFile, "manifest.stage")
        ApkAssembler.rebuild(apk, stage, mapOf("AndroidManifest.xml" to tmp))
        stage.copyTo(apk, overwrite = true)
    }
}
