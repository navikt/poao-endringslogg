package no.pto

import com.github.benmanes.caffeine.cache.Cache
import com.launchdarkly.eventsource.ConnectionErrorHandler
import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import no.pto.database.SubscribedApp
import okhttp3.internal.http2.StreamResetException
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("no.nav.pto.endringslogg.SanityListeningClient")
private var subscribedApps: HashMap<String, SubscribedApp> = hashMapOf()

class SanityListeningClient<V : Any?>(
    private val cache: Cache<String, V>,
    private val updateQuery: (query: String) -> V
) : EventHandler {
    fun subscribeToSanityApp(listenUrl: String, queryString: String) {
        logger.info("Starter å lytte på : {}", queryString)
        val eventHandler = SanityListeningClient(cache, updateQuery)
        val eventSource: EventSource = EventSource.Builder(eventHandler, URI.create(listenUrl))
            .reconnectTime(Duration.ofMillis(3000))
            .connectionErrorHandler(SanityConnectionErrorHandler())
            .build()

        if (isListeningTo(listenUrl)) {
            logger.warn("lytter allerde til: {}", queryString)
            return
        } else {
            subscribedApps[listenUrl] = SubscribedApp(listenUrl, queryString, eventSource)
        }
        eventSource.start()

        // Schedule task to ensure that connection has been established. If not, remove data from cache
        Executors.newSingleThreadScheduledExecutor().schedule({
            if (!subscribedApps[listenUrl]?.connectionEstablished!!) {
                subscribedApps[listenUrl]?.eventSource?.close()
                cache.asMap().remove(subscribedApps[listenUrl]?.queryString)
                logger.error("Klarte ikke å starte lytting mot: {}", queryString)
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
        val connection = subscribedApps[origin] ?: return

        when (event) {
            "welcome" -> { // connection is established
                connection.connectionEstablished = true
                logger.info("Subscribing to listening API: $origin")
            }
            "mutation" -> { // a change is discovered in Sanity -> update cache
                logger.info("Mutation in $origin discovered, updating cache.")
                cache.put(connection.queryString, updateQuery(connection.queryString))
            }
            "disconnect" -> {
                logger.warn("Listening API for $origin requested disconnection with error message: ${messageEvent.data}")
                subscribedApps.remove(origin)
                connection.eventSource.close()

                logger.info("Prøver å reconnecte til: {}", connection.queryString)
                subscribeToSanityApp(connection.listenURL, connection.queryString)
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

    fun isListeningTo(listeningUrl: String): Boolean {
        return subscribedApps.containsKey(listeningUrl)
    }
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
