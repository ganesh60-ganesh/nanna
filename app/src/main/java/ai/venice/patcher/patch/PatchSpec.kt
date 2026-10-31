package ai.venice.patcher.patch

sealed class PatchSpec(val label: String, val note: String) {
    object License : PatchSpec("Remove license verification", "Forces license and entitlement checks to pass")
    object Ads : PatchSpec("Block ad SDK", "Neuters MobileAds init, ad loading and ad providers")
    object Signature : PatchSpec("Bypass signature checks", "Forces PackageInfo signature comparisons to pass")
    object Iap : PatchSpec("Unlock in-app purchases", "Reports purchases as PURCHASED. Local checks only")
    object Trackers : PatchSpec("Disable trackers", "No-ops analytics and attribution init calls")
    data class Permissions(val names: List<String>) :
        PatchSpec("Remove ${names.size} permission(s)", "Strips permissions from the manifest")
}
