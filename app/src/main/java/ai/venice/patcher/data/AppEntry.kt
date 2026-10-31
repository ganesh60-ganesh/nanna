package ai.venice.patcher.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.graphics.ImageBitmap

data class AppEntry(
    val label: String,
    val packageName: String,
    val versionName: String,
    val signatureHash: String,
    val size: Long,
    val system: Boolean,
    val splits: Int,
    val installed: Long,
    val permissions: List<String>,
    val trackers: List<String>,
    val icon: ImageBitmap?,
    val info: ApplicationInfo,
    val pkgInfo: PackageInfo,
)
