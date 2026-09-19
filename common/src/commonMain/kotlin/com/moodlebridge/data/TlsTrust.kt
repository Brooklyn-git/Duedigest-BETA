package com.moodlebridge.data

import okhttp3.OkHttpClient
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

data class UntrustedCertificate(val host: String, val fingerprint: String)

class UntrustedCertificateException(val untrusted: UntrustedCertificate) :
    CertificateException("Untrusted certificate for host '${untrusted.host}' (${untrusted.fingerprint})")

fun fingerprintOf(encoded: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
    return digest.joinToString(":") { "%02X".format(it) }
}

fun sha256Fingerprint(cert: X509Certificate): String = fingerprintOf(cert.encoded)

fun resolveTrust(host: String, fingerprint: String, provider: (String) -> String?): UntrustedCertificate? =
    if (provider(host)?.equals(fingerprint, ignoreCase = true) == true) null
    else UntrustedCertificate(host, fingerprint)

fun findUntrustedCert(e: Throwable?): UntrustedCertificate? {
    var cursor: Throwable? = e
    while (cursor != null) {
        if (cursor is UntrustedCertificateException) return cursor.untrusted
        cursor = cursor.cause
    }
    return null
}

fun buildTrustClient(fingerprintProvider: (String) -> String?): OkHttpClient.Builder {
    val defaultTrustManager = systemTrustManager()
    val trustManager = DelegatingTrustManager(defaultTrustManager, fingerprintProvider)
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .addInterceptor { chain ->
            hostHolder.set(chain.request().url.host)
            try {
                chain.proceed(chain.request())
            } finally {
                hostHolder.remove()
            }
        }
}

private val hostHolder = ThreadLocal<String?>()

private fun systemTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as java.security.KeyStore?)
    return factory.trustManagers.firstNotNullOfOrNull { it as? X509TrustManager }
        ?: throw IllegalStateException("No X509TrustManager available in the system trust store")
}

private class DelegatingTrustManager(
    private val delegate: X509TrustManager,
    private val fingerprintProvider: (String) -> String?,
) : X509ExtendedTrustManager() {

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        delegate.checkClientTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) =
        checkClientTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) =
        checkClientTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        try {
            delegate.checkServerTrusted(chain, authType)
            return
        } catch (e: CertificateException) {
            val leaf = chain.firstOrNull() ?: throw e
            val host = hostHolder.get() ?: throw e
            val untrusted = resolveTrust(host, sha256Fingerprint(leaf), fingerprintProvider)
            if (untrusted != null) throw UntrustedCertificateException(untrusted)
        }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) =
        checkServerTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) =
        checkServerTrusted(chain, authType)

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
}