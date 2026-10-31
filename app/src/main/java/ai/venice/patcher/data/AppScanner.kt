package ai.venice.patcher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
        val BORING = setOf(
            "INTERNET", "ACCESS_NETWORK_STATE", "VIBRATE", "WAKE_LOCK",
            "FOREGROUND_SERVICE", "RECEIVE_BOOT_COMPLETED",
        )
    }

    @Suppress("DEPRECATION")
    suspend fun scan(): List<AppEntry> = withContext(Dispatchers.IO) {
        val flags = PackageManager.GET_SIGNATURES or PackageManager.GET_PERMISSIONS
        val packages: List<PackageInfo> = pm.getInstalledPackages(flags)
        val out = ArrayList<AppEntry>(packages.size)
        for (p in packages) {
            runCatching { build(p) }.getOrNull()?.let { out += it }
        }
        out.sortedWith(compareBy({ it.system }, { it.label.lowercase() }))
    }

    @Suppress("DEPRECATION")
    private fun build(pkg: PackageInfo): AppEntry {
        val info: ApplicationInfo = pkg.applicationInfo ?: error("no app info")
        val signatureBytes = pkg.signatures?.firstOrNull()?.toByteArray()
        val hash = if (signatureBytes == null) {
            "unknown"
        } else {
            MessageDigest.getInstance("SHA-256")
                .digest(signatureBytes)
                .joinToString("") { b -> "%02x".format(b) }
                .take(16)
        }

        val declared = pkg.requestedPermissions ?: emptyArray()
        val perms = ArrayList<String>()
        for (p in declared) {
            if (p.substringAfterLast('.') !in BORING) perms += p
        }

        val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        return AppEntry(
            label = pm.getApplicationLabel(info).toString(),
            packageName = pkg.packageName,
            versionName = pkg.versionName ?: "?",
            signatureHash = hash,
            size = File(info.sourceDir).length(),
            system = isSystem,
            splits = info.splitSourceDirs?.size ?: 0,
            installed = pkg.firstInstallTime,
            permissions = perms,
            trackers = scanTrackers(info.sourceDir),
            icon = info.loadIcon(pm).toBitmap()?.asImageBitmap(),
            info = info,
        )
    }

    private fun scanTrackers(path: String): List<String> {
        val found = LinkedHashSet<String>()
        runCatching {
            ZipFile(path).use { z ->
                for (e in z.entries()) {
                    if (!e.name.endsWith(".dex")) continue
                    val text = String(z.getInputStream(e).use { it.readBytes() }, Charsets.ISO_8859_1)
                    for ((key, name) in TRACKERS) if (text.contains(key)) found += name
                }
            }
        }
        return found.toList()
    }

    private fun Drawable.toBitmap(): Bitmap? {
        if (this is BitmapDrawable) return bitmap
        val w = intrinsicWidth.coerceAtLeast(1)
        val h = intrinsicHeight.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp))
        return bmp
    }
}
