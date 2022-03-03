package no.pto.plugins

import Err
import Ok
import SanityClient
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.pto.*
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
            val (userId, appId, _, maxEntries) = call.receive<BrukerData>()
            if (appId != "afolg") {
                call.respond(HttpStatusCode.NotImplemented)
            }
            val seenEntryIds = getSeenEntriesForUser(userId).map(UUID::toString).toSet()
            val seenForcedEntryIds = getSeenForcedEntriesForUser(userId).map(UUID::toString).toSet()

            when (val endringslogger = client.queryEndringslogg(endringsloggPoaoqQuery)) {
                is Ok -> {
                    if (endringslogger.value.result.isEmpty()) {
                        call.response.status(HttpStatusCode(204, "Data for app $appId doesn't exist."))
                    } else {
                        call.respond(endringslogger.value.result.map {
                            it.copy(
                                seen = it.id in seenEntryIds,
                                seenForced = it.id in seenForcedEntryIds,
                                forcedModal = it.modal?.forcedModal
                            )
                        }.subList(0, min(maxEntries, endringslogger.value.result.size)))
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
            val duration = call.receive<SessionDuration>()
            // TODO: report to prometheus
            call.respond(HttpStatusCode.OK)
        }
        patch("/analytics/modal-open") {
            val id = call.receive<DocumentId>()
            // TODO: report to prometheus
            call.respond(HttpStatusCode.OK)
        }
        patch("/analytics/link-click") {
            val id = call.receive<DocumentId>()
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
