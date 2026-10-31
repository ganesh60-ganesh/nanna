package ai.venice.patcher.patch

import android.content.Context
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

class Signer(private val ctx: Context) {

    private val store = File(ctx.filesDir, "signing.identity")
    @Volatile private var cached: Pair<PrivateKey, X509Certificate>? = null

    private fun identity(): Pair<PrivateKey, X509Certificate> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val pair = if (store.exists()) load() else generate().also { save(it) }
            cached = pair
            return pair
        }
    }

    fun sign(input: File, output: File) {
        val (key, cert) = identity()
        val cfg = ApkSigner.SignerConfig.Builder("PATCHER", key, listOf(cert)).build()
        ApkSigner.Builder(listOf(cfg))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    fun verify(file: File): Boolean =
        runCatching { ApkVerifier.Builder(file).build().verify().isVerified }.getOrDefault(false)

    private fun generate(): Pair<PrivateKey, X509Certificate> {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val subject = X500Name("CN=Patcher, O=Local")
        val now = System.currentTimeMillis()
        val b = JcaX509v3CertificateBuilder(
            subject, BigInteger.valueOf(now),
            Date(now - 86_400_000L), Date(now + 3_650L * 86_400_000L), subject, kp.public
        )
        val s = JcaContentSignerBuilder("SHA256withRSA").build(kp.private)
        return kp.private to JcaX509CertificateConverter().getCertificate(b.build(s))
    }

    private fun save(p: Pair<PrivateKey, X509Certificate>) {
        val e = Base64.getEncoder()
        store.writeText(e.encodeToString(p.first.encoded) + "\n" + e.encodeToString(p.second.encoded))
    }

    private fun load(): Pair<PrivateKey, X509Certificate> {
        val (k, c) = store.readText().lines()
        val d = Base64.getDecoder()
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(d.decode(k)))
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(d.decode(c).inputStream()) as X509Certificate
        return key to cert
    }
}
