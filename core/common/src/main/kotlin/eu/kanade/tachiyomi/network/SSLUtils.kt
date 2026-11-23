package eu.kanade.tachiyomi.network

import android.annotation.SuppressLint
import logcat.LogPriority
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/** Trusts every certificate. Weakens TLS, so it sits behind an off-by-default setting. */
@SuppressLint("CustomX509TrustManager")
fun OkHttpClient.Builder.disableCertificateVerification() = also { builder ->
    runCatching {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustAll), SecureRandom())
        builder.sslSocketFactory(sslContext.socketFactory, trustAll)
        builder.hostnameVerifier { _, _ -> true }
    }.onFailure { logcat(LogPriority.ERROR, it) { "Failed to disable certificate verification" } }
}
