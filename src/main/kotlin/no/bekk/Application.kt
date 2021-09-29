package no.bekk

import SanityClient
import io.ktor.application.*
import io.ktor.serialization.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import no.bekk.env.*
import no.bekk.plugins.*
import org.flywaydb.core.Flyway


fun Application.main() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            encodeDefaults = true
        })
    }
    install(CORS) {
        host("https://app.adeo.no/veilarbportefoljeflatefs")
        host("https://veilarbportefoljeflatefs.nais.adeo.no")
        host("https://veilarbportefoljeflatefs-q1.nais.preprod.local")
        host("https://app-q1.adeo.no/veilarbportefoljeflatefs")
        host("https://app-q1.dev.adeo.no/veilarbportefoljeflatefs")
        host("https://endringslogg.sanity.studio")

        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Patch)
        header(HttpHeaders.ContentType)
    }
}

fun main() {
    val flyway: Flyway = Flyway.configure().dataSource("jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_DATABASE?reWriteBatchedInserts=true?sslmode=require", DB_USERNAME, DB_PASSWORD).load()
    flyway.migrate()

    val client = SanityClient(SANITY_PROJECT_ID, "production")

    connectToDatabase()

    embeddedServer(Netty, environment = applicationEngineEnvironment {
        module {
            main()
            configureRouting(client)
        }
        connector {
            port = 8080
            host = "0.0.0.0"
        }
    }) {
    }.start(wait = true)
}
