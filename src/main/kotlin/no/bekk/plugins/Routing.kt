package no.bekk.plugins

import Err
import Ok
import SanityClient
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.bekk.*
import java.util.*

fun Application.configureRouting(client: SanityClient) {
    routing {
        post("/endringslogg") {
            val (userId, appId, maxEntries) = call.receive<BrukerData>()
            val entries = getSeenEntriesForUser(userId).map(UUID::toString).toSet()

            when(val endringslogger = client.query("*[_type == '$appId'][0...$maxEntries]")) {
                is Ok -> {
                    if (endringslogger.value.result.isEmpty()){
                        call.response.status(HttpStatusCode(204, "Data for app $appId doesn't exist."))
                    } else {
                        call.respond(endringslogger.value.result.map {
                            it.copy(seen = it.id in entries)
                        })
                    }
                }
                is Err -> {
                    print("Got a client request exception with error code ${endringslogger.error.response.status} and message ${endringslogger.error.message}")
                    call.response.status(HttpStatusCode(endringslogger.error.response.status.value, "Received error: ${endringslogger.error.message}"))
                }
            }

        }
        post("/analytics/sett-endringer") {
            val seen = call.receive<SeenStatus>()
            insertSeenEntries(seen.userId, seen.appId, seen.documentIds.map(UUID::fromString))
            call.respond(HttpStatusCode.OK) // TODO: Return status for insert
        }
        post("/analytics/session-duration") {
            val duration = call.receive<SessionDuration>()
            insertSessionDuration(duration)
            call.respond(HttpStatusCode.OK) // TODO: Return status for insert
        }
        patch("/analytics/modal-open") {
            val id = call.receive<DocumentId>()
            setModalOpen(id.documentId)
            call.respond(HttpStatusCode.OK) // TODO: Return status for insert
        }
        patch("/analytics/link-click") {
            val id = call.receive<DocumentId>()
            setLinkClicked(id.documentId)
            call.respond(HttpStatusCode.OK) // TODO: Return status for insert
        }


        get("/data/seen-all") {
            call.respond(getAllEntriesInSeen())
        }
        get("/data/seen-app") {

            call.request.queryParameters["appId"]?.let {
                call.respond(getSeenEntriesForAppId(it))
            } ?: call.respond(HttpStatusCode.BadRequest)
        }

        get("/data/seen") {
            call.request.queryParameters["docId"]?.let {
                call.respond(getSeenEntriesForDocId(it))
            } ?: call.respond(HttpStatusCode.BadRequest)
        }

        get("data/user-session-all"){
            call.request.queryParameters["appId"]?.let {
                call.respond(getAllEntriesInUserSessions(it))
            } ?: call.respond(HttpStatusCode.BadRequest)
        }

        get("data/unique-user-sessions-per-day"){
            call.request.queryParameters["appId"]?.let { appId ->
                call.request.queryParameters["moreThanMs"]?.let { moreThan ->
                    call.respond(getUniqueVisitorsPerDayForAppId(appId, moreThan.toInt()))
                } ?: call.respond(HttpStatusCode.BadRequest)
             } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
}
