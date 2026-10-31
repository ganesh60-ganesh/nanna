package ai.venice.patcher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

class AppScanner(private val ctx: Context) {

    private val pm = ctx.packageManager

    companion object {
        val TRACKERS = listOf(
            "com/google/firebase/analytics" to "Firebase Analytics",
            "com/google/android/gms/analytics" to "GMS Analytics",
            "com/google/firebase/crashlytics" to "Crashlytics",
            "com/facebook/appevents" to "Facebook",
            "com/appsflyer" to "AppsFlyer",
            "com/adjust/sdk" to "Adjust",
            "com/amplitude" to "Amplitude",
            "com/mixpanel" to "Mixpanel",
            "com/onesignal" to "OneSignal",
            "io/branch" to "Branch",
            "com/segment" to "Segment",
            "com/braze" to "Braze",
            "com/clevertap" to "CleverTap",
            "com/mparticle" to "mParticle",
            "com/flurry" to "Flurry",
            "com/yandex/metrica" to "AppMetrica",
            "com/google/firebase/perf" to "Firebase Perf",
        )
    }

    @Suppress("DEPRECATION")
    suspend fun scan(): List<AppEntry> = withContext(Dispatchers.IO) {
        pm.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_PERMISSIONS)
            .mapNotNull { p -> runCatching { build(p) }.getOrNull() }
            .sortedWith(compareBy({ it.system }, { it.label.lowercase() }))
    }

    @Suppress("DEPRECATION")
    private fun build(pkg: PackageInfo): AppEntry {
        val info = pkg.applicationInfo ?: error("no app info")
        val sig = pkg.signatures?.firstOrNull()?.toByteArray()
        val hash = sig?.let {
            MessageDigest.getInstance("SHA-256").digest(it)
                .joinToString("") { b -> "%02x".format(b) }
                .take(16)
        } ?: "unknown"

        val perms = (pkg.requestedPermissions ?: emptyArray()).toList()
            .map { it.substringAfterLast('.') to it }
            .filter { (short, _) -> short !in setOf("INTERNET", "ACCESS_NETWORK_STATE", "VIBRATE", "WAKE_LOCK") }
            .map { it.second }

        return AppEntry(
            label = pm.getApplicationLabel(info).toString(),
            packageName = pkg.packageName,
            versionName = pkg.versionName ?: "?",
            signatureHash = hash,
            size = File(info.sourceDir).length(),
            system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            splits = info.splitSourceDirs?.size ?: 0,
            installed = pkg.firstInstallTime,
            permissions = perms,
            trackers = scanTrackers(info.sourceDir),
            icon = info.loadIcon(pm).toBitmap()?.asImageBitmap(),
            info = info,
            pkgInfo = pkg,
        )
    }

    private fun scanTrackers(apkPath: String): List<String> = runCatching {
        val found = LinkedHashSet<String>()
        ZipFile(apkPath).use { zip ->
            for (e in zip.entries()) {
                if (!e.name.endsWith(".dex")) continue
                val bytes = zip.getInputStream(e).use { it.readBytes() }
                val text = String(bytes, Charsets.ISO_8859_1)
                for ((prefix, name) in TRACKERS) if (text.contains(prefix)) found += name
            }
        }
        found.toList()
    }.getOrDefault(emptyList())

    private fun Drawable.toBitmap(): Bitmap? {
        if (this is BitmapDrawable) return bitmap
        val w = intrinsicWidth.coerceAtLeast(1)
        val h = intrinsicHeight.coerceAtLeast(1)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { draw(Canvas(it)) }
    }
}
