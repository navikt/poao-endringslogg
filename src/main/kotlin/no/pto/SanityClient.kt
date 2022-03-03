import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import no.pto.EndringJson
import no.pto.MessageEventHandler
import no.pto.SlideImageDl
import no.pto.SlideImageJson
import no.pto.model.SystemmeldingSanity
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

sealed class Result<out T, out E>
class Ok<out T>(val value: T) : Result<T, Nothing>()
class Err<out E>(val error: E) : Result<Nothing, E>()

private val logger = LoggerFactory.getLogger("no.nav.pto.endringslogg.Application")
val apiVersion: String = "v2021-06-07"

private val endringsloggCache: Cache<String, EndringJson> = Caffeine.newBuilder()
    .expireAfterWrite(1000, TimeUnit.HOURS) // Cache skal bli oppdatert av sanity client
    .maximumSize(100)
    .build()

private val systemmeldingCache: Cache<String, List<SystemmeldingSanity>> = Caffeine.newBuilder()
    .expireAfterWrite(1000, TimeUnit.HOURS) // Cache skal bli oppdatert av sanity client
    .maximumSize(100)
    .build()

class SanityClient(
    private val projId: String
) {
    private val baseUrl: String = "https://$projId.api.sanity.io/$apiVersion"
    private val sanityListenerEndringlogg = MessageEventHandler(endringsloggCache, this::querySanityEndringslogg)
    private val sanityListenerSystemMelding = MessageEventHandler(systemmeldingCache, this::querySanitySystemmelding)

    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    fun queryEndringslogg(queryString: String, dataset: String): Result<EndringJson, ClientRequestException> {
        return tryCacheFirst(endringsloggCache, queryString, dataset) { q, d ->
            querySanityEndringslogg(q, d)
        }
    }

    fun querySystemmelding(queryString: String): Result<List<SystemmeldingSanity>, ClientRequestException> {
        return tryCacheFirst(
            systemmeldingCache,
            queryString,
            "production"
        ) { query, _ -> querySanitySystemmelding(query, "production") }
    }

    @Throws(ClientRequestException::class)
    private fun querySanityEndringslogg(queryString: String, dataset: String): EndringJson {
        val response: EndringJson
        runBlocking {
            response = client.get("$baseUrl/data/query/$dataset?query=$queryString")
        }
        val responseWithImage = EndringJson(response.result.map {
            it.copy(
                modal = it.modal?.copy(
                    slides = it.modal.slides.map { slide ->
                        when (slide.image) {
                            is SlideImageJson ->
                                slide.copy(
                                    image = SlideImageDl(
                                        imageObjToByteArray(
                                            slide.image.slideImage.jsonObject,
                                            dataset
                                        )
                                    )
                                )
                            else -> slide
                        }
                    }
                )
            )
        })
        val listenUrl = "$baseUrl/data/listen/$dataset?query=$queryString&includeResult=false&visibility=query"
        // call was successful. must then check if the response is empty. If empty -> don't subscribe
        if (response.result.isNotEmpty()) {
            sanityListenerEndringlogg.subscribeToSanityApp(listenUrl, queryString, dataset)
        }
        return responseWithImage
    }

    @Throws(ClientRequestException::class)
    private fun querySanitySystemmelding(queryString: String, dataset: String): List<SystemmeldingSanity> {
        logger.info("Gjør kall mot sanity...")
        val response: List<SystemmeldingSanity>
        runBlocking {
            response = client.get("$baseUrl/data/query/production?query=$queryString")
        }
        val listenUrl = "$baseUrl/data/listen/production?query=$queryString&includeResult=false&visibility=query"

        sanityListenerSystemMelding.subscribeToSanityApp(listenUrl, queryString, dataset)
        return response
    }

    private fun imageObjToByteArray(obj: JsonObject, dataset: String): ByteArray {
        val refJson: String = obj["asset"]!!.jsonObject["_ref"].toString().replace("\"", "")
        val ref = refJson.replace("(-([A-Za-z]+))\$".toRegex(), ".\$2").drop(6)
        val url = "https://cdn.sanity.io/images/$projId/$dataset/$ref"
        val httpResp: HttpResponse
        val byteArr: ByteArray
        runBlocking {
            httpResp = client.get(url)
            byteArr = httpResp.receive()
        }

        return byteArr
    }

    private fun <V> tryCacheFirst(
        cache: Cache<String, V>,
        query: String,
        dataset: String,
        valueSupplier: (queryString: String, datasetString: String) -> V
    ): Result<V, ClientRequestException> {
        val key = "$query.$dataset"
        return try {
            val value = cache.get(key) { valueSupplier(query, dataset) }
            Ok(value)
        } catch (e: ClientRequestException) {
            Err(e)
        }
    }
}
