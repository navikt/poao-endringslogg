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
import no.pto.SanityListeningClient
import no.pto.database.EndringJson
import no.pto.database.SlideImageDl
import no.pto.database.SlideImageJson
import no.pto.env.getEndringsloggPoaoQuery
import no.pto.env.getSystemmeldingPoaoQuery
import no.pto.model.SystemmeldingSanityRespons
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

sealed class Result<out T, out E>
class Ok<out T>(val value: T) : Result<T, Nothing>()
class Err<out E>(val error: E) : Result<Nothing, E>()

private val logger = LoggerFactory.getLogger("no.nav.pto.endringslogg.SanityClient")

private val endringsloggCache: Cache<String, EndringJson> = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS) // Cache blir også bli oppdatert av SanityListeningClient
    .maximumSize(1000)
    .build()

private val systemmeldingCache: Cache<String, SystemmeldingSanityRespons> = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS) // Cache blir også oppdatert av SanityListeningClient
    .maximumSize(1000)
    .build()

class SanityClient(
    private val projId: String,
    apiVersion: String
) {
    private val baseUrl: String = "https://$projId.api.sanity.io/$apiVersion"
    private val sanityListenerEndringlogg = SanityListeningClient(endringsloggCache, this::querySanityEndringslogg)
    private val sanityListenerSystemMelding = SanityListeningClient(systemmeldingCache, this::querySanitySystemmelding)

    fun initSanityEndringloggListener() {
        val listenUrl = getEndringsloggListeningURL()
        sanityListenerEndringlogg.subscribeToSanityApp(listenUrl, getEndringsloggPoaoQuery())
    }

    fun initSanitySystemMeldingListener() {
        val listenUrl = getSystemMeldingListeningURL()
        sanityListenerSystemMelding.subscribeToSanityApp(listenUrl, getSystemmeldingPoaoQuery())
    }

    fun reconnectListening() {
        val listenUrlSystemmeldinger = getSystemMeldingListeningURL()
        val listenUrlEndringslogg = getEndringsloggListeningURL()

        if(!sanityListenerSystemMelding.isListeningTo(listenUrlSystemmeldinger)){
            logger.info("Prøver å reconnecte lytting på Systemmeldinger")
            initSanitySystemMeldingListener()
        }
        if(!sanityListenerEndringlogg.isListeningTo(listenUrlEndringslogg)){
            logger.info("Prøver å reconnecte lytting på Endringslogg")
            initSanityEndringloggListener()
        }
        logger.info("Kontrollerte at lyttning er aktivt.")
    }

    private fun getEndringsloggListeningURL(): String {
        val queryString = getEndringsloggPoaoQuery()
        return "$baseUrl/data/listen/production?query=$queryString&includeResult=false&visibility=query"
    }
    private fun getSystemMeldingListeningURL(): String {
        val queryString = getSystemmeldingPoaoQuery()
        return "$baseUrl/data/listen/production?query=$queryString&includeResult=false&visibility=query"
    }

    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    fun queryEndringslogg(queryString: String): Result<EndringJson, ClientRequestException> {
        return tryCacheFirst(endringsloggCache, queryString) { query ->
            querySanityEndringslogg(query)
        }
    }

    fun querySystemmelding(queryString: String): Result<SystemmeldingSanityRespons, ClientRequestException> {
        return tryCacheFirst(systemmeldingCache, queryString) { query ->
            querySanitySystemmelding(query)
        }
    }

    @Throws(ClientRequestException::class)
    private fun querySanityEndringslogg(queryString: String): EndringJson {
        val response: EndringJson
        runBlocking {
            response = client.get("$baseUrl/data/query/production?query=$queryString")
        }
        val responseWithImage = EndringJson(response.result.map {
            it.copy(
                modal = it.modal?.copy(
                    slides = it.modal.slides.map { slide ->
                        when (slide.image) {
                            is SlideImageJson ->
                                slide.copy(
                                    image = SlideImageDl(
                                        imageObjToByteArray(slide.image.slideImage.jsonObject)
                                    )
                                )
                            else -> slide
                        }
                    }
                )
            )
        })
        return responseWithImage
    }

    @Throws(ClientRequestException::class)
    private fun querySanitySystemmelding(queryString: String): SystemmeldingSanityRespons {
        logger.info("Gjør kall mot sanity...")
        val response: SystemmeldingSanityRespons
        runBlocking {
            response = client.get("$baseUrl/data/query/production?query=$queryString")
        }
        return response
    }

    private fun imageObjToByteArray(obj: JsonObject): ByteArray {
        val refJson: String = obj["asset"]!!.jsonObject["_ref"].toString().replace("\"", "")
        val ref = refJson.replace("(-([A-Za-z]+))\$".toRegex(), ".\$2").drop(6)
        val url = "https://cdn.sanity.io/images/$projId/production/$ref"
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
        valueSupplier: (queryString: String) -> V
    ): Result<V, ClientRequestException> {
        return try {
            val value = cache.get(query) { valueSupplier(query) }
            Ok(value)
        } catch (e: ClientRequestException) {
            Err(e)
        }
    }
}
