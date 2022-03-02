import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.launchdarkly.eventsource.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import no.pto.EndringJson
import no.pto.SlideImageDl
import no.pto.SlideImageJson
import java.net.URI
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import no.pto.SubscribedApp
import okhttp3.internal.http2.StreamResetException
import org.slf4j.LoggerFactory

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

    private val logger = LoggerFactory.getLogger(javaClass)
    private val baseUrl: String = "https://$projId.api.sanity.io/$apiVersion"
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    private var subscribedApps: HashMap<String, SubscribedApp> = hashMapOf()

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

    fun query(queryString: String, dataset: String): Result<EndringJson, ClientRequestException> {
        return tryCacheFirst(queryCache, queryString, dataset) { q, d -> querySanity(q, d) }
    }

    private val queryCache: Cache<String, EndringJson> = Caffeine.newBuilder()
        .expireAfterWrite(1000, TimeUnit.HOURS) // Cache skal bli oppdatert av sanity client
        .maximumSize(1000)
        .build()

    @Throws(ClientRequestException::class)
    private fun querySanity(queryString: String, dataset: String): EndringJson {
        val response: EndringJson
        runBlocking {
            response = client.get("$baseUrl/data/query/$dataset?query=$queryString")
        }
        val responseWithImage = EndringJson(response.result.map {
            it.copy(
                modal = it.modal?.copy(
                    slides = it.modal.slides.map { s ->
                        when (s.image) {
                            is SlideImageJson ->
                                s.copy(
                                    image = SlideImageDl(
                                        imageObjToByteArray(
                                            s.image.slideImage.jsonObject,
                                            dataset
                                        )
                                    )
                                )
                            else -> s
                        }
                    }
                )
            )
        })
        val listenUrl = "$baseUrl/data/listen/$dataset?query=$queryString&includeResult=false&visibility=query"
        // call was successful. must then check if the response is empty. If empty -> don't subscribe
        if (response.result.isNotEmpty() and !subscribedApps.contains(listenUrl)) {
            subscribeToSanityApp(listenUrl, queryString, dataset)
        }
        return responseWithImage
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

    private fun <V> updateCache(
        cache: Cache<String, V>,
        query: String,
        dataset: String,
        valueSupplier: (queryString: String, datasetString: String) -> V
    ) {
        val key = "$query.$dataset"
        val newValue = valueSupplier(query, dataset)
        cache.put(key, newValue)
    }

    private fun subscribeToSanityApp(listenUrl: String, queryString: String, dataset: String) {
        val eventHandler = MessageEventHandler()
        val eventSource: EventSource = EventSource.Builder(eventHandler, URI.create(listenUrl))
            .reconnectTime(Duration.ofMillis(3000))
            .connectionErrorHandler(SanityConnectionErrorHandler())
            .build()

        eventSource.start()
        if (!subscribedApps.containsKey(listenUrl)) {
            subscribedApps[listenUrl] =
                SubscribedApp(listenUrl, queryString, dataset, "$queryString.$dataset", eventSource)
        }

        // Schedule task to ensure that connection has been established. If not, remove data from cache
        Executors.newSingleThreadScheduledExecutor().schedule({
            if (!subscribedApps[listenUrl]?.connectionEstablished!!) {
                logger.warn("Connection to $listenUrl not established.")
                subscribedApps[listenUrl]?.eventSource?.close()
                subscribedApps.remove(listenUrl)
                queryCache.asMap().remove(subscribedApps[listenUrl]?.cacheKey)
            }
        }, 20, TimeUnit.SECONDS)

    }

    /* calculates milliseconds from now until next given weekday with hourly offset in UTC time */
    private fun msToNextDay(dayOfWeek: DayOfWeek, hourOffset: Long): Long {
        val nextDay = LocalDate.now(Clock.systemUTC())
            .with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .atStartOfDay()
            .plusHours(hourOffset)
        val duration = Duration.between(LocalDateTime.now(Clock.systemUTC()), nextDay).toMillis()
        return if (duration < 0) {
            duration + TimeUnit.DAYS.toMillis(7)  // add one week if calculated duration is negative
        } else {
            duration
        }
    }

    /* Class to handle events from EventHandler */
    private inner class MessageEventHandler : EventHandler {

        @Throws(Exception::class)
        override fun onOpen() {
            logger.info("Åpner stream mot Sanity")
        }

        @Throws(Exception::class)
        override fun onClosed() {
            logger.info("Lukker stream mot Sanity")
        }

        /* Handles events from Sanity listen API*/
        override fun onMessage(event: String, messageEvent: MessageEvent) {
            val origin = messageEvent.origin.toString()
            when (event) {
                "welcome" -> { // connection is established
                    // cancels subscription, and clears cache every Saturday morning 01.00 UTC time
                    if (!subscribedApps[origin]!!.connectionEstablished) {
                        Executors.newSingleThreadScheduledExecutor().schedule({
                            subscribedApps[origin]?.connectionEstablished = false
                            subscribedApps[origin]?.eventSource?.close()
                            queryCache.asMap().remove(subscribedApps[origin]?.cacheKey)
                            subscribedApps.remove(origin)
                            logger.info("Unsubscribed from listening API: $origin")
                        }, msToNextDay(DayOfWeek.SATURDAY, 1), TimeUnit.MILLISECONDS)
                    }
                    subscribedApps[origin]?.connectionEstablished = true
                    logger.info("Subscribing to listening API: $origin")
                }
                "mutation" -> { // a change is discovered in Sanity -> update cache
                    logger.info("Mutation in $origin discovered, updating cache.")
                    updateCache(
                        queryCache,
                        subscribedApps[origin]!!.queryString,
                        subscribedApps[origin]!!.dataset
                    ) { q, p -> querySanity(q, p) }
                }
                "disconnect" -> { // client should disconnect and stay disconnected. Likely due to a query error
                    logger.info("Listening API for $origin requested disconnection with error message: ${messageEvent.data}")
                    subscribedApps[origin]?.connectionEstablished = false
                    subscribedApps[origin]?.eventSource?.close()
                    queryCache.asMap().remove(subscribedApps[origin]?.cacheKey)
                    subscribedApps.remove(origin)
                }
            }
        }

        override fun onError(t: Throwable) {
            if (t is StreamResetException) {
                logger.info("Stream mot Sanity ble resatt", t)
            } else {
                logger.error("En feil oppstod", t)
            }
        }

        override fun onComment(comment: String) {
            logger.debug("Holder stream mot Sanity i gang")
        }
    }

    /* Shuts down connection when connection attempt fails*/
    private inner class SanityConnectionErrorHandler : ConnectionErrorHandler {
        override fun onConnectionError(t: Throwable?): ConnectionErrorHandler.Action {
            return if (t is StreamResetException) { // to handle stream resets every 30 minutes
                ConnectionErrorHandler.Action.PROCEED
            } else {
                ConnectionErrorHandler.Action.SHUTDOWN
            }
        }
    }
}
