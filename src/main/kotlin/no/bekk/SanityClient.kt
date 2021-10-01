import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import no.bekk.EndringJson
import java.util.concurrent.TimeUnit

sealed class Result<out T, out E>
class Ok<out T>(val value: T) : Result<T, Nothing>()
class Err<out E>(val error: E) : Result<Nothing, E>()

class SanityClient(
    val projId: String,
    val dataset: String,
    val apiVersion: String = "v2021-06-07",
    val token: String = "",
    val useCdn: Boolean = false
) {

    val baseUrl: String = "https://$projId.api.sanity.io/$apiVersion"
    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun query(queryString: String): Result<EndringJson, ClientRequestException> {
        return tryCacheFirst(queryCache, queryString) { q -> querySanity(q) }
    }

    private val queryCache: Cache<String, EndringJson> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(50)
        .build()

    private suspend fun querySanity(queryString: String): Result<EndringJson, ClientRequestException> {
        val response: EndringJson
        return try {
            response = client.get("$baseUrl/data/query/$dataset?query=$queryString")
            Ok(response)
        } catch (e: ClientRequestException) {
            print("Received client request exception with error code ${e.response} and message ${e.message}")
            Err(e)
        }
    }

    private suspend fun <K, V> tryCacheFirst(
        cache: Cache<K, V>,
        key: K,
        valueSupplier: suspend (queryString: K) -> Result<V, ClientRequestException>
    ): Result<V, ClientRequestException> {
        val value = cache.getIfPresent(key)
        return if (value == null) {
            when (val newValue = valueSupplier(key)) {
                is Ok -> {
                    cache.put(key, newValue.value)
                    newValue
                }
                is Err -> {
                    newValue
                }
                /* else clause should not be needed, kotlin language bug
                 * https://youtrack.jetbrains.com/issue/KT-35784
                 */
                else -> newValue
            }
        } else {
            Ok(value)
        }
    }
}
