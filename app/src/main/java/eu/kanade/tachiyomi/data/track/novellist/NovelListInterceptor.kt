package eu.kanade.tachiyomi.data.track.novellist

import okhttp3.Interceptor
import okhttp3.Response
import raito.data.track.RAITO_TRACKER_USER_AGENT
import raito.data.track.TrackerSignedOutException

/**
 * One credential shape, the JWT the sign-in cookie carries, sent as the `jwtAuth` bearer the
 * service's OpenAPI document declares. None of tsundoku's spoofed CORS headers are reproduced: every
 * write route answers 401 rather than 403 without them, so they reach the route unaided.
 */
class NovelListInterceptor(
    private var token: String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credential = token ?: throw TrackerSignedOutException("NovelList")

        val authRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $credential")
            .header("User-Agent", RAITO_TRACKER_USER_AGENT)
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(token: String?) {
        this.token = token
    }
}
