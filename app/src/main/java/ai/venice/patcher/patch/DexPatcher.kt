package ai.venice.patcher.patch

import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.builder.MethodImplementationBuilder
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.MethodImplementation
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.immutable.ImmutableClassDef
import org.jf.dexlib2.immutable.ImmutableDexFile
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.reference.ImmutableStringReference
import java.io.File

object DexPatcher {

    private val LICENSE = Regex("(?i)(licen[cs]e|purchas|premium|paid|unlock|entitle)")
    private val AD = setOf("loadAd", "initialize", "load", "requestAd", "start")
    private const val ADS = "Lcom/google/android/gms/ads/"
    private const val BILL = "Lcom/android/billingclient/api/"
    private val TRACK = setOf("initialize", "init", "start", "logEvent", "record")

    fun patch(file: File, specs: Set<PatchSpec>): Int {
        val dex = DexFileFactory.loadDexFile(file, Opcodes.getDefault())
        val rebuilt = mutableListOf<ClassDef>()
        var hits = 0

        for (cls in dex.classes) {
            val methods = mutableListOf<Method>()
            var dirty = false
            for (m in cls.methods) {
                val impl = m.implementation
                val kind = if (impl == null) null else kindFor(m, impl, specs)
                if (kind == null || impl == null) {
                    methods += m
                    continue
                }
                methods += ImmutableMethod.of(m, build(impl, kind))
                dirty = true
                hits++
            }
            rebuilt += if (dirty) ImmutableClassDef(
                cls.type, cls.accessFlags, cls.superclass, cls.interfaces,
                cls.sourceFile, cls.annotations, cls.fields, methods,
            ) else cls
        }

        if (hits > 0) {
            DexFileFactory.writeDexFile(file.absolutePath, ImmutableDexFile(dex.opcodes, rebuilt))
        }
        return hits
    }

    private fun kindFor(m: Method, impl: MethodImplementation, specs: Set<PatchSpec>): Ret? = when {
        PatchSpec.Ads in specs && isAd(m) -> Ret.VOID
        PatchSpec.Iap in specs -> iap(m)
        PatchSpec.Trackers in specs && isTrack(m) -> Ret.VOID
        PatchSpec.License in specs && isLicense(m) -> Ret.TRUE
        PatchSpec.Signature in specs && isSig(m, impl) -> Ret.TRUE
        else -> null
    }

    private fun isAd(m: Method) = m.definingClass.startsWith(ADS) && m.name in AD && m.returnType == "V"

    private fun iap(m: Method): Ret? {
        if (!m.definingClass.startsWith(BILL)) return null
        return when {
            m.name == "getPurchaseState" && m.returnType == "I" -> Ret.ONE
            m.name == "getResponseCode" && m.returnType == "I" -> Ret.ZERO
            m.name in setOf("getPurchaseToken", "getOrderId", "getSignature")
                && m.returnType == "Ljava/lang/String;" -> Ret.TEXT
            m.name in setOf("acknowledgePurchase", "consumeAsync") && m.returnType == "V" -> Ret.VOID
            else -> null
        }
    }

    private fun isTrack(m: Method): Boolean {
        if (m.name !in TRACK || m.returnType != "V") return false
        val c = m.definingClass
        return PREFIX.any { c.startsWith(it) }
    }

    private fun isLicense(m: Method) =
        m.returnType == "Z" && m.parameterTypes.size <= 2 &&
            LICENSE.containsMatchIn(m.name) && !m.definingClass.startsWith(ADS)

    private fun isSig(m: Method, impl: MethodImplementation): Boolean {
        if (m.returnType != "Z") return false
        if (m.name.contains("signature", true)) return true
        return impl.instructions.any { i ->
            (i as? ReferenceInstruction)?.reference?.toString()?.contains("Signature") == true
        }
    }

    private enum class Ret { TRUE, VOID, ONE, ZERO, TEXT }

    private fun build(old: MethodImplementation, k: Ret): MethodImplementation {
        val r = old.registerCount
        val b = MethodImplementationBuilder(r + 1)
        when (k) {
            Ret.TRUE, Ret.ONE -> {
                b.addInstruction(BuilderInstruction21s(Opcode.CONST_16, r, 1))
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN, r))
            }
            Ret.ZERO -> {
                b.addInstruction(BuilderInstruction21s(Opcode.CONST_16, r, 0))
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN, r))
            }
            Ret.TEXT -> {
                b.addInstruction(BuilderInstruction21c(Opcode.CONST_STRING, r, ImmutableStringReference("patched")))
                b.addInstruction(BuilderInstruction11x(Opcode.RETURN_OBJECT, r))
            }
            Ret.VOID -> b.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
        }
        return b.methodImplementation
    }

    private val PREFIX = listOf(
        "Lcom/google/firebase/analytics", "Lcom/google/android/gms/analytics",
        "Lcom/google/firebase/crashlytics", "Lcom/facebook/appevents", "Lcom/appsflyer",
        "Lcom/adjust/sdk", "Lcom/amplitude", "Lcom/mixpanel", "Lcom/onesignal",
        "Lio/branch", "Lcom/segment", "Lcom/braze", "Lcom/clevertap", "Lcom/mparticle",
        "Lcom/flurry", "Lcom/yandex/metrica",
    )
}
