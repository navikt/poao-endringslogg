package no.pto.plugins

import Err
import Ok
import SanityClient
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.pto.database.*
import no.pto.env.getEndringsloggPoaoQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.min

val logger: Logger = LoggerFactory.getLogger("no.nav.pto.endringlogg.routing")
val endringsloggPoaoqQuery: String = getEndringsloggPoaoQuery()

fun Application.configureEndringsloggRouting(client: SanityClient) {
    routing {
        post("/endringslogg") {
            val (userId: String, appId: String, _, maxEntries: Int) = call.receive<BrukerData>()
            if (appId != "afolg") {
                call.respond(HttpStatusCode.NotImplemented)
            } else {
                val seenEntryIds = getSeenEntriesForUser(userId).map(UUID::toString).toSet()
                val seenForcedEntryIds = getSeenForcedEntriesForUser(userId).map(UUID::toString).toSet()

                when (val endringslogger = client.queryEndringslogg(endringsloggPoaoqQuery)) {
                    is Ok -> {
                        if (endringslogger.value.result.isEmpty()) {
                            logger.info("status=204, method=GET, /endringslogg, fant ingen endringslogger")
                            call.response.status(HttpStatusCode(204, "Data for app $appId doesn't exist."))
                        } else {
                            logger.info("status=200, method=GET, /endringslogg, {} endringslogger.", endringslogger.value.result.size)
                            call.respond(endringslogger.value.result.map {
                                it.copy(
                                    seen = it.id in seenEntryIds,
                                    seenForced = it.id in seenForcedEntryIds,
                                    forcedModal = it.modal?.forcedModal
                                )
                            }.sortedByDescending { it.date }.subList(0, min(maxEntries, endringslogger.value.result.size)))
                        }
                    }
                    is Err -> {
                        logger.info("Got a client request exception with error code ${endringslogger.error.response.status.value} and message ${endringslogger.error.message}")
                        call.response.status(
                            HttpStatusCode(
                                endringslogger.error.response.status.value,
                                "Received error: ${endringslogger.error.message}"
                            )
                        )
                    }

                    else -> {}
                }
            }
        }

        post("/analytics/sett-endringer") {
            val seen = call.receive<SeenStatus>()
            insertSeenEntries(seen.userId, seen.appId, seen.documentIds.map(UUID::fromString))
            call.respond(HttpStatusCode.OK)
        }

        post("/analytics/seen-forced-modal") {
            val seen = call.receive<SeenForcedStatus>()
            insertSeenForcedEntries(seen.userId, seen.documentIds.map(UUID::fromString))
            call.respond(HttpStatusCode.OK)
        }

        post("/analytics/session-duration") {
            call.receive<SessionDuration>()
            // TODO: report to prometheus
            call.respond(HttpStatusCode.OK)
        }

        patch("/analytics/modal-open") {
            call.receive<DocumentId>()
            // TODO: report to prometheus
            call.respond(HttpStatusCode.OK)
        }

        patch("/analytics/link-click") {
            call.receive<DocumentId>()
            // TODO: report to prometheus
            call.respond(HttpStatusCode.OK)
        }

        get("/data/seen-all") {
            call.respond(HttpStatusCode.Gone)
        }
        get("/data/seen-app") {
            call.respond(HttpStatusCode.Gone)
        }
        get("/data/seen") {
            call.respond(HttpStatusCode.Gone)
        }
        get("data/user-session-all") {
            call.respond(HttpStatusCode.Gone)
        }
        get("data/unique-user-sessions-per-day") {
            call.respond(HttpStatusCode.Gone)
        }
    }
}
