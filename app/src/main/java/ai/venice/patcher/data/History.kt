package ai.venice.patcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryEntry(
    val time: Long,
    val pkg: String,
    val label: String,
    val actions: List<String>,
    val ok: Boolean,
    val note: String,
)

class History(ctx: Context) {

    private val file = File(ctx.filesDir, "history.json")

    fun load(): List<HistoryEntry> = runCatching {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryEntry(
                o.getLong("t"), o.getString("p"), o.getString("l"),
                o.getJSONArray("a").let { a -> (0 until a.length()).map { a.getString(it) } },
                o.getBoolean("ok"), o.optString("n"),
            )
        }
    }.getOrDefault(emptyList()).reversed()

    fun append(e: HistoryEntry) {
        val all = load().reversed().toMutableList()
        all += e
        val arr = JSONArray()
        all.takeLast(300).forEach { h ->
            arr.put(
                JSONObject()
                    .put("t", h.time).put("p", h.pkg).put("l", h.label)
                    .put("a", JSONArray(h.actions)).put("ok", h.ok).put("n", h.note)
            )
        }
        runCatching { file.writeText(arr.toString()) }
    }

    fun clear() = runCatching { file.delete() }.let { }
}
