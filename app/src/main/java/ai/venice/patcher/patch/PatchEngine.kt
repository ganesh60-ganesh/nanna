package ai.venice.patcher.patch

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File
import java.util.zip.ZipFile

class PatchException(message: String) : Exception(message)

class PatchEngine(private val ctx: Context) {

    private val signer = Signer(ctx)

    fun run(
        info: ApplicationInfo,
        specs: Set<PatchSpec>,
        log: (String) -> Unit,
        progress: (Float) -> Unit,
        isCancelled: () -> Boolean,
    ): File {
        val work = File(ctx.cacheDir, "work/${System.currentTimeMillis()}").apply { mkdirs() }
        val base = File(work, "base.apk")

        log("Staging ${File(info.sourceDir).name}")
        File(info.sourceDir).copyTo(base, overwrite = true)
        progress(0.1f)

        val dexSpecs = specs.filterNot { it is PatchSpec.Permissions }
        val permSpecs = specs.filterIsInstance<PatchSpec.Permissions>().flatMap { it.names }

        var hits = 0
        if (dexSpecs.isNotEmpty()) {
            val dexDir = File(work, "dex").apply { mkdirs() }
            val replacements = HashMap<String, File>()

            ZipFile(base).use { zip ->
                val entries = zip.entries().toList().filter { it.name.endsWith(".dex") }
                log("Scanning ${entries.size} dex file(s)")

                entries.forEachIndexed { i, entry ->
                    if (isCancelled()) throw PatchException("Cancelled")
                    val target = File(dexDir, "classes$i.dex")
                    zip.getInputStream(entry).use { inp -> target.outputStream().use { inp.copyTo(it) } }
                    val found = DexPatcher.patch(target, dexSpecs.toSet())
                    if (found > 0) {
                        log("  ${entry.name}: $found method(s)")
                        replacements[entry.name] = target
                        hits += found
                    }
                    progress(0.1f + 0.4f * (i + 1) / entries.size)
                }
            }

            if (hits > 0) {
                log("Repackaging")
                val stage = File(work, "stage.apk")
                ApkAssembler.rebuild(base, stage, replacements)
                stage.copyTo(base, overwrite = true)
            }
            progress(0.55f)
        }

        if (PatchSpec.Ads in specs) {
            log("Stripping ad components from manifest")
            if (ManifestPatcher.stripAdNodes(base)) hits++
        }

        if (permSpecs.isNotEmpty()) {
            log("Removing ${permSpecs.size} permission(s)")
            if (ManifestPatcher.removePermissions(base, permSpecs)) hits++
        }

        if (hits == 0) throw PatchException("Nothing matched — try a different patch set")

        if (isCancelled()) throw PatchException("Cancelled")
        log("Signing with local key")
        val signed = File(work, "signed.apk")
        signer.sign(base, signed)
        progress(0.9f)

        log("Verifying signature")
        if (!signer.verify(signed)) throw PatchException("Signature verification failed")

        val outDir = File(ctx.cacheDir, "install").apply { mkdirs() }
        val out = File(outDir, "${info.packageName}-patched.apk")
        signed.copyTo(out, overwrite = true)
        progress(1f)
        log("Done — ${out.length() / 1024} KB")
        return out
    }
}
