package ai.venice.patcher.core

object Shell {

    @Volatile private var probed = false
    @Volatile private var rooted = false

    fun hasRoot(): Boolean {
        if (probed) return rooted
        probed = true
        rooted = runCatching {
            val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            out.contains("uid=0")
        }.getOrDefault(false)
        return rooted
    }

    fun run(cmd: String): String? = runCatching {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out
    }.getOrNull()

    fun quote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}
