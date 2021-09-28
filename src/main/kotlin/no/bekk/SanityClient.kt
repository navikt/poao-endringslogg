import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import no.bekk.EndringJson

sealed class Result<out T, out E>
class Ok<out T>(val value: T): Result<T, Nothing>()
class Err<out E>(val error: E): Result<Nothing, E>()

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

    suspend fun query(query: String): Result<EndringJson, ClientRequestException> {
        val response: EndringJson
        return try {
            response = client.get("$baseUrl/data/query/$dataset?query=$query")
            Ok(response)
        } catch (e: ClientRequestException){
            print("Received client request exception with error code ${e.response} and message ${e.message}")
            Err(e)
        }
    }
}