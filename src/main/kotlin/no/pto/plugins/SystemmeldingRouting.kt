package no.pto.plugins

import Err
import Ok
import SanityClient
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.pto.env.erIProd
import no.pto.model.Systemmelding
import java.net.URLEncoder
import java.nio.charset.Charset

val q1MeldingerQuery = URLEncoder.encode("*[_type=='alert_overiskten'][0...100][publisert_q1]", Charset.forName("utf-8"))
val prodMedlingerQuery = URLEncoder.encode("*[_type=='alert_overiskten'][0...100][publisert]", Charset.forName("utf-8"))

fun Application.configureSystemmeldingRouting(client: SanityClient) {
    routing {
        get("/systemmeldinger") {
            val query = if (erIProd()) prodMedlingerQuery else q1MeldingerQuery
            logger.info("Henter ut alle alerts, prod: {}", erIProd())

            when (val systemmeldinger = client.querySystemmelding(query)) {
                is Ok -> {
                    if (systemmeldinger.value.isEmpty()) {
                        call.response.status(HttpStatusCode(200, "Ingen data"))
                        call.respond(listOf<Systemmelding>())
                    } else {
                        call.respond(systemmeldinger.value.map {
                            Systemmelding(
                                tittel = it.tittel,
                                type = it.alert,
                                beskrivelse = it.description
                            )
                        })
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