package no.pto

import com.github.benmanes.caffeine.cache.Cache
import com.launchdarkly.eventsource.ConnectionErrorHandler
import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import no.pto.env.erIProd
import okhttp3.internal.http2.StreamResetException
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val logger = LoggerFactory.getLogger("no.nav.pto.endringslogg.Application")
private var subscribedApps: HashMap<String, SubscribedApp> = hashMapOf()


/* Class to handle events from EventHandler */
class MessageEventHandler<V : Any?>(
    val cache: Cache<String, V>,
    val updateQuery: (query: String) -> V
) : EventHandler {

    fun subscribeToSanityApp(listenUrl: String, queryString: String) {
        logger.info("Starter å lytte på: {}", queryString)
        val eventHandler = MessageEventHandler(cache, updateQuery)
        val eventSource: EventSource = EventSource.Builder(eventHandler, URI.create(listenUrl))
            .reconnectTime(Duration.ofMillis(3000))
            .connectionErrorHandler(SanityConnectionErrorHandler())
            .build()

        eventSource.start()
        if (subscribedApps.containsKey(listenUrl)) {
            logger.warn("lytter allerde til: {}", queryString)
            eventSource.close()
        } else {
            subscribedApps[listenUrl] = SubscribedApp(listenUrl, queryString, eventSource)
        }

        // Schedule task to ensure that connection has been established. If not, remove data from cache
        Executors.newSingleThreadScheduledExecutor().schedule({
            if (!subscribedApps[listenUrl]?.connectionEstablished!!) {
                logger.warn("Connection to $listenUrl not established.")
                subscribedApps[listenUrl]?.eventSource?.close()
                cache.asMap().remove(subscribedApps[listenUrl]?.queryString)
                logger.error("Klarte ikke å starte lytting mot: {}", queryString)
                subscribeToSanityApp(listenUrl, queryString)
            }
        }, 20, TimeUnit.SECONDS)
    }

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
                        cache.asMap().remove(subscribedApps[origin]?.queryString)
                        subscribedApps.remove(origin)
                        logger.info("Unsubscribed from listening API: $origin")
                    }, msToNextDay(DayOfWeek.SATURDAY, 1), TimeUnit.MILLISECONDS)
                }
                subscribedApps[origin]?.connectionEstablished = true
                logger.info("Subscribing to listening API: $origin")
            }
            "mutation" -> { // a change is discovered in Sanity -> update cache
                logger.info("Mutation in $origin discovered, updating cache.")
                updateCache(cache, subscribedApps[origin]!!.queryString) { q -> updateQuery(q) }
            }
            "disconnect" -> { // client should disconnect and stay disconnected. Likely due to a query error
                logger.info("Listening API for $origin requested disconnection with error message: ${messageEvent.data}")
                val connection = subscribedApps[origin];
                connection?.connectionEstablished = false
                connection?.eventSource?.close()
                cache.asMap().remove(connection?.queryString)
                subscribedApps.remove(origin)

                if (connection != null) {
                    logger.info("Prøver å reconnecte til: {}", connection.queryString)
                    subscribeToSanityApp(connection.queryString, connection.queryString)
                }
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

private fun <V> updateCache(
    cache: Cache<String, V>,
    query: String,
    valueSupplier: (queryString: String) -> V
) {
    cache.put(query, valueSupplier(query))
}

/* Shuts down connection when connection attempt fails*/
private class SanityConnectionErrorHandler : ConnectionErrorHandler {
    override fun onConnectionError(t: Throwable?): ConnectionErrorHandler.Action {
        return if (t is StreamResetException) { // to handle stream resets every 30 minutes
            ConnectionErrorHandler.Action.PROCEED
        } else {
            ConnectionErrorHandler.Action.SHUTDOWN
        }
    }
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