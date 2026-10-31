package ai.venice.patcher.core

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("patcher", Context.MODE_PRIVATE)

    var dynamicColor: Boolean
        get() = sp.getBoolean("dyn", true)
        set(v) { sp.edit().putBoolean("dyn", v).apply() }

    var darkMode: Int
        get() = sp.getInt("dark", 0)
        set(v) { sp.edit().putInt("dark", v).apply() }

    var verbose: Boolean
        get() = sp.getBoolean("verbose", true)
        set(v) { sp.edit().putBoolean("verbose", v).apply() }

    var patched: Set<String>
        get() = sp.getStringSet("patched", emptySet()) ?: emptySet()
        set(v) { sp.edit().putStringSet("patched", v).apply() }
}
