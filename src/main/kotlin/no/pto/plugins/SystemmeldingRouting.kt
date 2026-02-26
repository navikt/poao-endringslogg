package no.pto.plugins

import Err
import Ok
import SanityClient
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.pto.env.getSystemmeldingPoaoQuery
import no.pto.model.Systemmelding
import no.pto.model.SystemmeldingSanity
import java.time.ZonedDateTime

val systemmeldingerPoaoQuery: String = getSystemmeldingPoaoQuery()

fun Application.configureSystemmeldingRouting(client: SanityClient) {
    routing {
        options("/systemmeldinger") { call.respond(HttpStatusCode.OK) }
        get("/systemmeldinger") {
            when (val systemmeldinger = client.querySystemmelding(systemmeldingerPoaoQuery)) {
                is Ok -> {
                    val result = systemmeldinger.value.result
                    if (result.isEmpty()) {
                        logger.info("status=200, method=GET, /systemmeldinger, 0 aktive meldinger")
                        call.respond(HttpStatusCode.OK, listOf<Systemmelding>())
                    } else {
                        val aktiveSystemmeldinger = filtrerSystemmeldinger(result)
                        logger.info("status=200, method=GET, /systemmeldinger, {} aktive meldinger", aktiveSystemmeldinger.size)
                        call.respond(HttpStatusCode.OK, aktiveSystemmeldinger)
                    }
                }
                is Err -> {
                    logger.info("Got a client request exception with error code ${systemmeldinger.error.response.status.value} and message ${systemmeldinger.error.message}")
                    call.response.status(
                        HttpStatusCode(
                            systemmeldinger.error.response.status.value,
                            "Received error: ${systemmeldinger.error.message}"
                        )
                    )
                }

                else -> {}
            }
        }
    }
}

fun filtrerSystemmeldinger(systemmeldinger: List<SystemmeldingSanity>): List<Systemmelding> {
    return systemmeldinger
        .filter { it.fraDato == null || ZonedDateTime.parse(it.fraDato).isBefore(ZonedDateTime.now()) }
        .filter { it.tilDato == null || ZonedDateTime.parse(it.tilDato).isAfter(ZonedDateTime.now()) }
        .map {
            Systemmelding(
                tittel = it.title,
                type = it.alert,
                beskrivelse = it.description
            )
        }
}
