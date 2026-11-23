package eu.kanade.tachiyomi.network

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

const val PROXY_DISABLED = 0
const val PROXY_HTTP = 1
const val PROXY_SOCKS = 2

/** Reads [NetworkPreferences] on every call, so proxy edits apply to the next request. */
class ProxyProvider(private val preferences: NetworkPreferences) {

    private var cached: Proxy? = null

    private fun enabled() = preferences.proxyType.get() != PROXY_DISABLED

    /** Null when the configuration is unusable. */
    fun currentProxy(): Proxy? {
        val type = when (preferences.proxyType.get()) {
            PROXY_HTTP -> Proxy.Type.HTTP
            PROXY_SOCKS -> Proxy.Type.SOCKS
            else -> return Proxy.NO_PROXY
        }
        val address = preferences.proxyAddress.get().trim()
        val port = preferences.proxyPort.get().trim().toIntOrNull() ?: return null
        if (address.isEmpty() || port !in 1..0xFFFF) return null
        cached?.let {
            val a = it.address() as? InetSocketAddress
            if (a != null && it.type() == type && a.port == port && a.hostString == address) return it
        }
        return Proxy(type, InetSocketAddress.createUnresolved(address, port)).also { cached = it }
    }

    val selector = object : ProxySelector() {
        // A broken config falls back to a direct connection rather than failing every request.
        override fun select(uri: URI?): List<Proxy> = listOf(currentProxy() ?: Proxy.NO_PROXY)
        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) = Unit
    }

    val authenticator = Authenticator { _: Route?, response: Response ->
        if (!enabled() || response.request.header("Proxy-Authorization") != null) return@Authenticator null
        val login = preferences.proxyLogin.get()
        val password = preferences.proxyPassword.get()
        if (login.isEmpty() || password.isEmpty()) return@Authenticator null
        response.request.newBuilder()
            .header("Proxy-Authorization", Credentials.basic(login, password))
            .build()
    }
}

