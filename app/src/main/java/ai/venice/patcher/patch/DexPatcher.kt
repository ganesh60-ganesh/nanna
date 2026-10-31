package ai.venice.patcher.patch

import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.builder.MethodImplementationBuilder
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.MethodImplementation
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.immutable.reference.ImmutableStringReference
import java.io.File

/** In-place bytecode rewriter. Returns the number of methods patched. */
object DexPatcher {

    private val LICENSE = Regex("(?i)(licen[cs]e|purchas|premium|paid|unlock|entitle)")
    private val AD_METHODS = setOf("loadAd", "initialize", "load", "requestAd", "start")
    private const val ADS_PKG = "Lcom/google/android/gms/ads/"
    private const val BILLING = "Lcom/android/billingclient/api/"
    private val TRACKER_INIT = setOf("initialize", "init", "start", "logEvent", "record")

    fun patch(file: File, specs: Set<PatchSpec>): Int {
        val dex = DexFileFactory.loadDexFile(file, Opcodes.getDefault())
        var hits = 0

        for (cls in dex.classes.toList()) {
            for (m in cls.methods.toList()) {
                val impl = m.implementation ?: continue
                val kind = when {
                    PatchSpec.Ads in specs && isAd(m) -> Ret.VOID
                    PatchSpec.Iap in specs && isIap(m) != null -> isIap(m)!!
                    PatchSpec.Trackers in specs && isTracker(m) -> Ret.VOID
                    PatchSpec.License in specs && isLicense(m) -> Ret.TRUE
                    PatchSpec.Signature in specs && isSignature(m, impl) -> Ret.TRUE
                    else -> null
                } ?: continue

                m.implementation = build(impl, kind)
                hits++
            }
        }

        if (hits > 0) DexFileFactory.writeDexFile(file.absolutePath, dex)
        return hits
    }

    private fun isAd(m: Method) =
        m.definingClass.startsWith(ADS_PKG) && m.name in AD_METHODS && m.returnType == "V"

    private fun isIap(m: Method): Ret? {
        if (!m.definingClass.startsWith(BILLING)) return null
        return when {
            m.name == "getPurchaseState" && m.returnType == "I" -> Ret.ONE
            m.name == "getResponseCode" && m.returnType == "I" -> Ret.ZERO
            m.name in setOf("getPurchaseToken", "getOrderId", "getSignature")
                && m.returnType == "Ljava/lang/String;" -> Ret.TEXT
            m.name in setOf("acknowledgePurchase", "consumeAsync") && m.returnType == "V" -> Ret.VOID
            else -> null
        }
    }

    private fun isTracker(m: Method): Boolean {
        if (m.name !in TRACKER_INIT || m.returnType != "V") return false
        val c = m.definingClass
        return AppScannerPrefixes.any { c.startsWith(it) }
    }

    private fun isLicense(m: Method) =
        m.returnType == "Z" && m.parameterTypes.size <= 2 &&
            LICENSE.containsMatchIn(m.name) && !m.definingClass.startsWith(ADS_PKG)

    private fun isSignature(m: Method, impl: MethodImplementation): Boolean {
        if (m.returnType != "Z") return false
        if (m.name.contains("signature", true)) return true
        return impl.instructions.any { ins ->
            (ins as? ReferenceInstruction)?.reference?.toString()?.contains("Signature") == true
        }
    }

    private enum class Ret { TRUE, VOID, ONE, ZERO, TEXT }

    private fun build(old: MethodImplementation, kind: Ret): MethodImplementation {
        val r = old.registerCount
        val b = MethodImplementationBuilder(r + 1)
        when (kind) {
            Ret.TRUE -> {
                b.addInstruction(BuilderInstruction21s(Opcode.CONST_16, r, 1L))
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN, r))
            }
            Ret.ONE -> {
                b.addInstruction(BuilderInstruction21s(Opcode.CONST_16, r, 1L))
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN, r))
            }
            Ret.ZERO -> {
                b.addInstruction(BuilderInstruction21s(Opcode.CONST_16, r, 0L))
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN, r))
            }
            Ret.TEXT -> {
                b.addInstruction(
                    BuilderInstruction21c(Opcode.CONST_STRING, r, ImmutableStringReference("patched"))
                )
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN_OBJECT, r))
            }
            Ret.VOID -> b.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
        }
        return b.methodImplementation
    }

    private val AppScannerPrefixes = listOf(
        "Lcom/google/firebase/analytics", "Lcom/google/android/gms/analytics",
        "Lcom/google/firebase/crashlytics", "Lcom/facebook/appevents",
        "Lcom/appsflyer", "Lcom/adjust/sdk", "Lcom/amplitude", "Lcom/mixpanel",
        "Lcom/onesignal", "Lio/branch", "Lcom/segment", "Lcom/braze",
        "Lcom/clevertap", "Lcom/mparticle", "Lcom/flurry", "Lcom/yandex/metrica",
    )
}
