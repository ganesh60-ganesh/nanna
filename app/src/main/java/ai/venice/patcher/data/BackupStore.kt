package ai.venice.patcher.data

import android.content.Context
import org.json.JSONObject
import java.io.File

data class Backup(val dir: File, val label: String, val pkg: String, val time: Long, val apk: File)

class BackupStore(private val ctx: Context) {

    private val root = File(ctx.filesDir, "backups").apply { mkdirs() }

    fun save(entry: AppEntry): Backup {
        val dir = File(root, "${entry.packageName}-${System.currentTimeMillis()}").apply { mkdirs() }
        val src = File(entry.info.sourceDir)
        val apk = File(dir, "base.apk")
        src.copyTo(apk, overwrite = true)

        entry.info.splitSourceDirs?.forEachIndexed { i, path ->
            runCatching { File(path).copyTo(File(dir, "split$i.apk"), overwrite = true) }
        }

        File(dir, "meta.json").writeText(
            JSONObject()
                .put("label", entry.label).put("pkg", entry.packageName)
                .put("time", System.currentTimeMillis()).toString()
        )
        return Backup(dir, entry.label, entry.packageName, System.currentTimeMillis(), apk)
    }

    fun list(): List<Backup> = (root.listFiles() ?: emptyArray())
        .filter { it.isDirectory }
        .mapNotNull { dir ->
            runCatching {
                val meta = JSONObject(File(dir, "meta.json").readText())
                Backup(dir, meta.getString("label"), meta.getString("pkg"), meta.getLong("time"),
                    File(dir, "base.apk"))
            }.getOrNull()
        }
        .sortedByDescending { it.time }

    fun delete(b: Backup) = b.dir.deleteRecursively()
}
