package raito.domain.recommendation

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import raito.data.track.RAITO_TRACKER_USER_AGENT

class ShikimoriRecommendationsTest {

    @Test
    fun `the recommendations request identifies as Raito`() = runTest {
        var recorded: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            recorded = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("[]".toResponseBody("application/json".toMediaType())).build()
        }.build()

        ShikimoriRecommendations(client, fakeTracker(), Json { ignoreUnknownKeys = true }).getRecsById(1L)

        recorded!!.header("User-Agent") shouldBe RAITO_TRACKER_USER_AGENT
    }
}
