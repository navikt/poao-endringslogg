package no.pto.plugins

import Err
import Ok
import SanityClient
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.pto.env.erIProd
import no.pto.env.getSystemmeldingPoaoQuery
import no.pto.model.Systemmelding
import no.pto.model.SystemmeldingSanity
import java.time.ZonedDateTime

val systemmeldingerPoaoQuery: String = getSystemmeldingPoaoQuery()

fun Application.configureSystemmeldingRouting(client: SanityClient) {
    routing {
        get("/systemmeldinger") {
            logger.info("Henter ut alle alerts, prod: {}", erIProd())
            when (val systemmeldinger = client.querySystemmelding(systemmeldingerPoaoQuery)) {
                is Ok -> {
                    val result = systemmeldinger.value.result
                    if (result.isEmpty()) {
                        call.response.status(HttpStatusCode(200, "Ingen data"))
                        call.respond(listOf<Systemmelding>())
                    } else {
                        val aktiveSystemmeldinger = filtrerSystemmeldinger(result)
                        call.respond(aktiveSystemmeldinger)
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
            )
        }
}
