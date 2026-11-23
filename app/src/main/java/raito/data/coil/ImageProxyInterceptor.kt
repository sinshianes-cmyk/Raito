package raito.data.coil

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.isOriginal
import eu.kanade.tachiyomi.network.NetworkPreferences
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Routes plain http(s) image URLs through wsrv.nl or 0ms.dev. Falls back to the original URL if the
 * proxy fails. Reads the pref per request so a change applies at once.
 */
class ImageProxyInterceptor(private val preferences: NetworkPreferences) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val mode = preferences.imageProxy.get()
        if (mode < 0) return chain.proceed()
        val request = chain.request
        val url = when (val data = request.data) {
            is HttpUrl -> data
            is String -> data.toHttpUrlOrNull()
            else -> null
        }
        if (url == null || url.host == "wsrv.nl" || url.host == "v.recipes") return chain.proceed()

        val proxied: HttpUrl = when (mode) {
            0 -> {
                val b = HttpUrl.Builder().scheme("https").host("wsrv.nl")
                    .addQueryParameter("url", url.toString())
                    .addQueryParameter("we", null)
                val size = request.sizeResolver.size()
                if (!size.isOriginal) {
                    b.addQueryParameter("crop", "cover")
                    (size.height as? Dimension.Pixels)?.let { b.addQueryParameter("h", it.px.toString()) }
                    (size.width as? Dimension.Pixels)?.let { b.addQueryParameter("w", it.px.toString()) }
                }
                b.build()
            }
            1 -> "https://v.recipes/i/$url".toHttpUrl()
            else -> return chain.proceed()
        }
        val result = chain.withRequest(request.newBuilder().data(proxied).build()).proceed()
        return if (result is SuccessResult) result else chain.proceed()
    }
}
