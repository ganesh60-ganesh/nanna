package ai.venice.patcher.patch

import java.io.File
import java.util.zip.ZipFile

/** Structural manifest edits. No-ops if the AXML round-trip fails. */
object ManifestPatcher {

    private val AD_NODES = setOf(
        "com.google.android.gms.ads.APPLICATION_ID",
        "com.google.android.gms.ads.AdActivity",
        "com.google.android.gms.ads.MobileAdsInitProvider",
        "com.google.android.gms.ads.DELAY_APP_MEASUREMENT_INIT",
    )

    private val PERM_TAGS = setOf("uses-permission", "uses-permission-sdk-23")

    fun removePermissions(apk: File, names: List<String>): Boolean {
        val raw = read(apk) ?: return false
        val root = runCatching { Axml.parse(raw) }.getOrNull() ?: return false
        if (!prune(root, PERM_TAGS, names.toSet())) return false
        return runCatching { write(apk, Axml.write(root)) }.isSuccess
    }

    fun stripAdNodes(apk: File): Boolean {
        val raw = read(apk) ?: return false
        val root = runCatching { Axml.parse(raw) }.getOrNull() ?: return false
        if (!prune(root, null, AD_NODES)) return false
        return runCatching { write(apk, Axml.write(root)) }.isSuccess
    }

    private fun prune(parent: Axml.Elem, tags: Set<String>?, names: Set<String>): Boolean {
        var changed = false
        val cursor = parent.children.iterator()
        while (cursor.hasNext()) {
            val child = cursor.next()
            val tagOk = tags == null || child.name in tags
            val nameOk = child.attrs.any { a -> a.name == "name" && a.raw in names }
            if (tagOk && nameOk) {
                cursor.remove()
                changed = true
            } else if (prune(child, tags, names)) {
                changed = true
            }
        }
        return changed
    }

    private fun read(apk: File): ByteArray? = ZipFile(apk).use { z ->
        val entry = z.getEntry("AndroidManifest.xml") ?: return@use null
        z.getInputStream(entry).use { it.readBytes() }
    }

    private fun write(apk: File, bytes: ByteArray) {
        val tmp = File(apk.parentFile, "manifest.bin").apply { writeBytes(bytes) }
        val stage = File(apk.parentFile, "manifest.stage")
        ApkAssembler.rebuild(apk, stage, mapOf("AndroidManifest.xml" to tmp))
        stage.copyTo(apk, overwrite = true)
    }
}
